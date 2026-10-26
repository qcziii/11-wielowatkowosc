package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private static final String[] KATEGORIE = {"książki", "elektronika", "dom"};

    /** Źródło n zamówień o id 1..n i wartości id (kategoria id % 3). Pilnuje, kto i ile przeczytał. */
    static class Zrodlo implements Iterator<Zamowienie> {
        final int n;
        final AtomicInteger przeczytane = new AtomicInteger();
        final Set<Thread> czytajacy = ConcurrentHashMap.newKeySet();

        Zrodlo(int n) {
            this.n = n;
        }

        @Override
        public boolean hasNext() {
            czytajacy.add(Thread.currentThread());
            return przeczytane.get() < n;
        }

        @Override
        public Zamowienie next() {
            czytajacy.add(Thread.currentThread());
            int id = przeczytane.incrementAndGet();
            if (id > n) {
                throw new NoSuchElementException();
            }
            return new Zamowienie(id, KATEGORIE[id % 3], BigDecimal.valueOf(id));
        }
    }

    /** Obsługa testowa: śpi, liczy równoległość, zapisuje obsłużone id i wątki. */
    static class Obsluga implements ObslugaZamowienia {
        final long ms;
        final AtomicInteger teraz = new AtomicInteger();
        final AtomicInteger maks = new AtomicInteger();
        final AtomicInteger rozpoczete = new AtomicInteger();
        final Map<Long, Integer> obsluzone = new ConcurrentHashMap<>();
        final Set<Thread> watki = ConcurrentHashMap.newKeySet();
        volatile Zrodlo zrodlo;
        final AtomicInteger najwiecejNaZapas = new AtomicInteger();

        Obsluga(long ms) {
            this.ms = ms;
        }

        @Override
        public void obsluz(Zamowienie z) throws Exception {
            watki.add(Thread.currentThread());
            int start = rozpoczete.incrementAndGet();
            if (zrodlo != null) {
                najwiecejNaZapas.accumulateAndGet(zrodlo.przeczytane.get() - start, Math::max);
            }
            maks.accumulateAndGet(teraz.incrementAndGet(), Math::max);
            try {
                obsluzone.merge(z.id(), 1, Integer::sum);
                if (ms > 0) {
                    Thread.sleep(ms);
                }
                if (z.id() % 10 == 0) {
                    throw new IllegalStateException("błąd zamówienia " + z.id());
                }
            } finally {
                teraz.decrementAndGet();
            }
        }
    }

    @Test
    @DisplayName("1) 10 000 zamówień: każde obsłużone dokładnie raz, raport się zgadza")
    void raport() throws Exception {
        Obsluga o = new Obsluga(0);
        Raport r = new PrzetwarzaczZamowien(4, 50, o).przetworz(new Zrodlo(10_000));
        assertEquals(10_000, o.obsluzone.size(), "Liczba różnych obsłużonych zamówień");
        assertTrue(o.obsluzone.values().stream().allMatch(v -> v == 1), "Któreś zamówienie obsłużone więcej niż raz");
        assertEquals(9_000, r.przetworzone());
        assertEquals(1_000, r.bledy(), "Co dziesiąte zamówienie kończy się błędem");
        Map<String, BigDecimal> oczekiwane = new java.util.HashMap<>();
        for (long id = 1; id <= 10_000; id++) {
            if (id % 10 != 0) {
                oczekiwane.merge(KATEGORIE[(int) (id % 3)], BigDecimal.valueOf(id), BigDecimal::add);
            }
        }
        assertEquals(oczekiwane, r.sumyWgKategorii());
    }

    @Test
    @DisplayName("2) Konsumenci pracują równolegle: dokładnie 4 obsługi naraz (200 zamówień po 20 ms)")
    void rownolegle() throws Exception {
        Obsluga o = new Obsluga(20);
        long start = System.nanoTime();
        new PrzetwarzaczZamowien(4, 10, o).przetworz(new Zrodlo(200));
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(4, o.maks.get(), "Naraz działało " + o.maks.get() + " obsług, konsumentów jest 4");
        assertTrue(ms < 2500, "200 x 20 ms na 4 konsumentach to ok. 1 s, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("3) Źródło czyta tylko jeden wątek i nie czyta na zapas więcej, niż mieści kolejka")
    void ograniczonaKolejka() throws Exception {
        Obsluga o = new Obsluga(5);
        Zrodlo z = new Zrodlo(300);
        o.zrodlo = z;
        new PrzetwarzaczZamowien(3, 10, o).przetworz(z);
        assertEquals(1, z.czytajacy.size(), "Źródło czytało " + z.czytajacy.size() + " wątków: " + z.czytajacy);
        // Na zapas: 10 w kolejce + 3 u konsumentów + 1 w ręku producenta (z małym zapasem na pomiar).
        assertTrue(o.najwiecejNaZapas.get() <= 10 + 3 + 2,
                "Producent przeczytał na zapas " + o.najwiecejNaZapas.get() + " zamówień, a kolejka ma pojemność 10");
    }

    @Test
    @DisplayName("4) Po powrocie wszystkie wątki konsumentów i producenta są zakończone")
    void sprzatanie() throws Exception {
        Obsluga o = new Obsluga(1);
        Zrodlo z = new Zrodlo(100);
        new PrzetwarzaczZamowien(4, 10, o).przetworz(z);
        for (Thread t : o.watki) {
            t.join(1000);
            assertFalse(t.isAlive(), "Konsument " + t.getName() + " dalej działa, stan " + t.getState());
        }
        for (Thread t : z.czytajacy) {
            if (t != Thread.currentThread()) {
                t.join(1000);
                assertFalse(t.isAlive(), "Producent " + t.getName() + " dalej działa, stan " + t.getState());
            }
        }
    }

    @Test
    @DisplayName("5) Puste źródło: pusty raport, nic nie zawisa")
    void puste() throws Exception {
        Raport r = new PrzetwarzaczZamowien(4, 10, new Obsluga(0)).przetworz(new Zrodlo(0));
        assertEquals(new Raport(0, 0, Map.of()), r);
    }
}
