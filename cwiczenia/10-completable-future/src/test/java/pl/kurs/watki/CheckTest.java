package pl.kurs.watki;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final AtomicInteger nr = new AtomicInteger();
    private final ExecutorService executor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "sklep-" + nr.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    /** Nazwy wątków, w których wołano usługi. */
    private final Set<String> watkiUslug = ConcurrentHashMap.newKeySet();

    @AfterEach
    void zamknij() {
        executor.shutdownNow();
    }

    private void praca(long ms) {
        watkiUslug.add(Thread.currentThread().getName());
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("przerwano", e);
        }
    }

    private final Uslugi.Klienci klienci = id -> {
        praca(200);
        if (id == 404) {
            throw new Uslugi.KlientNieZnalezionyException(id);
        }
        return new Uslugi.Klient(id, "Klient " + id);
    };

    private final Uslugi.Zamowienia zamowienia = id -> {
        praca(200);
        return List.of(new Uslugi.Zamowienie("Z1", new BigDecimal("100.00")), new Uslugi.Zamowienie("Z2", new BigDecimal("50.00")));
    };

    private Uslugi.Rabaty rabat(long ms, BigDecimal wartosc, boolean blad) {
        return id -> {
            praca(ms);
            if (blad) {
                throw new IllegalStateException("usługa rabatów niedostępna");
            }
            return wartosc;
        };
    }

    private Podsumowanie pobierz(SerwisPodsumowan s, long id) throws Exception {
        return s.podsumowanie(id).get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("1) Wynik poprawny, trzy usługi wołane równolegle (ok. 200 ms, nie 600 ms)")
    void rownolegle() throws Exception {
        SerwisPodsumowan s = new SerwisPodsumowan(klienci, zamowienia, rabat(200, new BigDecimal("0.10"), false), executor);
        long start = System.nanoTime();
        Podsumowanie p = pobierz(s, 7);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(new Podsumowanie("Klient 7", 2, new BigDecimal("150.00"), new BigDecimal("0.10"), new BigDecimal("135.00")), p);
        assertTrue(ms < 450, "Usługi po 200 ms wołane równolegle to ok. 200 ms, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("2) podsumowanie() wraca od razu, nie czeka na usługi")
    void nieBlokuje() {
        SerwisPodsumowan s = new SerwisPodsumowan(klienci, zamowienia, rabat(200, BigDecimal.ZERO, false), executor);
        long start = System.nanoTime();
        CompletableFuture<Podsumowanie> f = s.podsumowanie(7);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < 100, "podsumowanie() blokowało przez " + ms + " ms");
        assertFalse(f.isDone(), "Wynik gotowy od razu? Usługi trwają 200 ms");
    }

    @Test
    @DisplayName("3) Usługi wołane na podanym executorze (wątki sklep-N), nie na ForkJoinPool.commonPool")
    void executor() throws Exception {
        SerwisPodsumowan s = new SerwisPodsumowan(klienci, zamowienia, rabat(200, BigDecimal.ZERO, false), executor);
        pobierz(s, 7);
        assertFalse(watkiUslug.isEmpty());
        assertTrue(watkiUslug.stream().allMatch(n -> n.startsWith("sklep-")), "Usługi wołane w wątkach: " + watkiUslug);
    }

    @Test
    @DisplayName("4) Usługa rabatów rzuca wyjątek: rabat 0, wynik dalej jest")
    void rabatBlad() throws Exception {
        SerwisPodsumowan s = new SerwisPodsumowan(klienci, zamowienia, rabat(50, null, true), executor);
        Podsumowanie p = pobierz(s, 7);
        assertEquals(0, p.rabat().signum(), "Rabat ma być 0, jest " + p.rabat());
        assertEquals(new BigDecimal("150.00"), p.doZaplaty());
    }

    @Test
    @DisplayName("5) Usługa rabatów odpowiada po 3 s: po 500 ms liczymy bez rabatu")
    void rabatWolny() throws Exception {
        SerwisPodsumowan s = new SerwisPodsumowan(klienci, zamowienia, rabat(3000, new BigDecimal("0.50"), false), executor);
        long start = System.nanoTime();
        Podsumowanie p = pobierz(s, 7);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(0, p.rabat().signum(), "Rabat ma być 0, jest " + p.rabat());
        assertTrue(ms < 900, "Po 500 ms bez odpowiedzi rabatu wynik ma być od razu, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("6) Brak klienta: future kończy się wyjątkiem, przyczyna KlientNieZnalezionyException")
    void brakKlienta() {
        SerwisPodsumowan s = new SerwisPodsumowan(klienci, zamowienia, rabat(200, BigDecimal.ZERO, false), executor);
        ExecutionException e = assertThrows(ExecutionException.class, () -> s.podsumowanie(404).get(5, TimeUnit.SECONDS));
        assertInstanceOf(Uslugi.KlientNieZnalezionyException.class, e.getCause(), "Przyczyna: " + e.getCause());
    }
}
