package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    /**
     * Wykonuje operację w osobnym wątku i czeka na koniec maksymalnie 2 s.
     * Zwraca wynik albo wyjątek, który poleciał z wykonaj().
     */
    private static Object wTerminie(PulaPolaczen pula, java.util.function.Function<Polaczenie, ?> op) throws Exception {
        AtomicReference<Object> wynik = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                wynik.set(pula.wykonaj(op));
            } catch (Throwable e) {
                wynik.set(e);
            }
        });
        t.setDaemon(true);
        t.start();
        t.join(2000);
        if (t.isAlive()) {
            t.interrupt();
            fail("Operacja czeka na połączenie ponad 2 s, choć nikt nie używa puli (wolne połączenia: "
                    + pula.wolnePolaczenia() + " z " + pula.maksimum() + ")");
        }
        return wynik.get();
    }

    @Test
    @DisplayName("1) Naraz co najwyżej 3 operacje (pula 3, 12 wątków)")
    void limit() throws Exception {
        PulaPolaczen pula = new PulaPolaczen(3);
        AtomicInteger teraz = new AtomicInteger();
        AtomicInteger maks = new AtomicInteger();
        List<Thread> watki = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Thread t = new Thread(() -> {
                try {
                    pula.wykonaj(p -> {
                        maks.accumulateAndGet(teraz.incrementAndGet(), Math::max);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        teraz.decrementAndGet();
                        return null;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            watki.add(t);
            t.start();
        }
        for (Thread t : watki) {
            t.join();
        }
        assertEquals(3, maks.get(), "Naraz działało " + maks.get() + " operacji");
        assertEquals(3, pula.wolnePolaczenia());
    }

    @Test
    @DisplayName("2) Po 5 operacjach z wyjątkiem pula dalej działa i ma 3 wolne połączenia")
    void poBledach() throws Exception {
        PulaPolaczen pula = new PulaPolaczen(3);
        for (int i = 0; i < 5; i++) {
            final int nr = i;
            Object wynik = wTerminie(pula, p -> {
                throw new IllegalStateException("błąd SQL " + nr);
            });
            assertInstanceOf(IllegalStateException.class, wynik, "Wyjątek z operacji ma lecieć do wywołującego");
        }
        assertEquals("ok", wTerminie(pula, p -> "ok"));
        assertEquals(3, pula.wolnePolaczenia(), "Pula po błędach ma złą liczbę wolnych połączeń");
    }

    @Test
    @DisplayName("3) Wątek przerwany w czasie czekania na połączenie nie psuje licznika wolnych połączeń")
    void przerwanieWCzasieCzekania() throws Exception {
        PulaPolaczen pula = new PulaPolaczen(1);
        CountDownLatch zajete = new CountDownLatch(1);
        CountDownLatch zwolnij = new CountDownLatch(1);
        Thread trzyma = new Thread(() -> {
            try {
                pula.wykonaj(p -> {
                    zajete.countDown();
                    try {
                        zwolnij.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        trzyma.start();
        assertTrue(zajete.await(2, TimeUnit.SECONDS));

        AtomicReference<Throwable> wynikCzekajacego = new AtomicReference<>();
        Thread czeka = new Thread(() -> {
            try {
                pula.wykonaj(p -> "nie powinno się wykonać");
            } catch (Throwable e) {
                wynikCzekajacego.set(e);
            }
        });
        czeka.start();
        while (czeka.getState() != Thread.State.WAITING) {
            Thread.sleep(5);
        }
        czeka.interrupt();
        czeka.join(2000);
        assertInstanceOf(InterruptedException.class, wynikCzekajacego.get(), "Przerwany wątek ma dostać InterruptedException");

        zwolnij.countDown();
        trzyma.join(2000);
        assertEquals(1, pula.wolnePolaczenia(), "Pula na 1 połączenie ma teraz " + pula.wolnePolaczenia() + " wolnych");
    }
}
