package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    /** Czeka, aż wątek zacznie czekać (WAITING), maksymalnie 2 s. */
    private static void czekajAzCzeka(Thread t, AtomicReference<Throwable> blad) throws InterruptedException {
        long koniec = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (t.getState() != Thread.State.WAITING && System.nanoTime() < koniec) {
            Thread.sleep(10);
        }
        assertNull(blad.get(), () -> "Wątek " + t.getName() + " rzucił wyjątek: " + blad.get());
        assertEquals(Thread.State.WAITING, t.getState(), "Wątek " + t.getName() + " nie czeka (wait), tylko jest w stanie " + t.getState());
    }

    private static Thread watek(String nazwa, Akcja akcja, AtomicReference<Throwable> blad) {
        Thread t = new Thread(() -> {
            try {
                akcja.wykonaj();
            } catch (Throwable e) {
                blad.compareAndSet(null, e);
            }
        }, nazwa);
        t.setDaemon(true);
        return t;
    }

    interface Akcja {
        void wykonaj() throws Exception;
    }

    @Test
    @DisplayName("1) Jeden wątek: kolejność FIFO i rozmiar")
    void fifo() throws Exception {
        BuforOgraniczony<String> b = new BuforOgraniczony<>(3);
        assertEquals(0, b.rozmiar());
        b.wloz("a");
        b.wloz("b");
        b.wloz("c");
        assertEquals(3, b.rozmiar());
        assertEquals("a", b.wez());
        assertEquals("b", b.wez());
        b.wloz("d");
        assertEquals("c", b.wez());
        assertEquals("d", b.wez());
        assertEquals(0, b.rozmiar());
    }

    @Test
    @DisplayName("2) wez() na pustym buforze czeka (wait, bez zjadania procesora), aż ktoś włoży element")
    void wezCzeka() throws Exception {
        BuforOgraniczony<String> b = new BuforOgraniczony<>(2);
        AtomicReference<String> odebrany = new AtomicReference<>();
        AtomicReference<Throwable> blad = new AtomicReference<>();
        Thread konsument = watek("konsument", () -> odebrany.set(b.wez()), blad);
        konsument.start();
        czekajAzCzeka(konsument, blad);
        assertNull(odebrany.get(), "wez() wrócił z pustego bufora");
        b.wloz("x");
        konsument.join(2000);
        assertNull(blad.get(), () -> "Konsument rzucił wyjątek: " + blad.get());
        assertEquals("x", odebrany.get(), "Konsument nie dostał elementu po wloz()");
    }

    @Test
    @DisplayName("3) wloz() na pełnym buforze czeka, aż ktoś wyjmie element")
    void wlozCzeka() throws Exception {
        BuforOgraniczony<Integer> b = new BuforOgraniczony<>(2);
        b.wloz(1);
        b.wloz(2);
        AtomicBoolean wlozyl = new AtomicBoolean();
        AtomicReference<Throwable> blad = new AtomicReference<>();
        Thread producent = watek("producent", () -> {
            b.wloz(3);
            wlozyl.set(true);
        }, blad);
        producent.start();
        czekajAzCzeka(producent, blad);
        assertFalse(wlozyl.get(), "wloz() wrócił, choć bufor był pełny");
        assertEquals(2, b.rozmiar(), "Pełny bufor (pojemność 2) ma więcej elementów niż pojemność");
        assertEquals(1, b.wez());
        producent.join(2000);
        assertNull(blad.get(), () -> "Producent rzucił wyjątek: " + blad.get());
        assertTrue(wlozyl.get(), "Producent nie włożył elementu po zwolnieniu miejsca");
        assertEquals(2, b.wez());
        assertEquals(3, b.wez());
    }

    @Test
    @DisplayName("4) Przerwany wątek czekający w wez() dostaje InterruptedException")
    void przerwanie() throws Exception {
        BuforOgraniczony<String> b = new BuforOgraniczony<>(1);
        AtomicReference<Throwable> blad = new AtomicReference<>();
        Thread konsument = watek("konsument", b::wez, blad);
        konsument.start();
        czekajAzCzeka(konsument, blad);
        konsument.interrupt();
        konsument.join(2000);
        assertFalse(konsument.isAlive(), "Przerwany konsument dalej czeka");
        assertInstanceOf(InterruptedException.class, blad.get(), "Oczekiwano InterruptedException");
    }

    @Test
    @DisplayName("5) 4 producentów i 4 konsumentów, mały bufor: każdy element odebrany dokładnie raz, nic nie zawisa")
    void wieluProducentowIKonsumentow() throws Exception {
        int producenci = 4, konsumenci = 4, naProducenta = 50_000;
        for (int pojemnosc : new int[]{1, 3}) {
            BuforOgraniczony<Integer> b = new BuforOgraniczony<>(pojemnosc);
            AtomicLong suma = new AtomicLong();
            AtomicInteger odebrane = new AtomicInteger();
            AtomicInteger przekroczenia = new AtomicInteger();
            AtomicReference<Throwable> blad = new AtomicReference<>();
            ConcurrentLinkedQueue<Thread> watki = new ConcurrentLinkedQueue<>();
            for (int p = 0; p < producenci; p++) {
                int baza = p * naProducenta;
                watki.add(watek("producent-" + p, () -> {
                    for (int i = 1; i <= naProducenta; i++) {
                        b.wloz(baza + i);
                    }
                }, blad));
            }
            for (int k = 0; k < konsumenci; k++) {
                watki.add(watek("konsument-" + k, () -> {
                    for (int i = 0; i < naProducenta; i++) {
                        int r = b.rozmiar();
                        if (r > pojemnosc || r < 0) {
                            przekroczenia.incrementAndGet();
                        }
                        suma.addAndGet(b.wez());
                        odebrane.incrementAndGet();
                    }
                }, blad));
            }
            watki.forEach(Thread::start);
            for (Thread t : watki) {
                t.join(10_000);
                assertFalse(t.isAlive(), "Wątek " + t.getName() + " zawisł w stanie " + t.getState()
                        + " (pojemność " + pojemnosc + ", odebrano " + odebrane.get() + " z " + producenci * naProducenta + ")");
            }
            assertNull(blad.get(), () -> "Wątek rzucił wyjątek: " + blad.get());
            long n = (long) producenci * naProducenta;
            assertEquals(n, odebrane.get());
            assertEquals(n * (n + 1) / 2, suma.get(), "Suma odebranych elementów się nie zgadza: coś zgubione albo zdublowane");
            assertEquals(0, przekroczenia.get(), "Tyle razy rozmiar() pokazał więcej niż pojemność albo mniej niż 0");
            assertEquals(0, b.rozmiar());
        }
    }
}
