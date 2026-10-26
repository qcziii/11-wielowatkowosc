package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private static Map<String, String> wersja(int n) {
        Map<String, String> m = new HashMap<>();
        for (char c = 'a'; c <= 'z'; c++) {
            m.put("klucz." + c, "v" + n);
        }
        return m;
    }

    private static Thread demon(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
        return t;
    }

    @Test
    @DisplayName("1) Jeden wątek: pobierz, czytaj, przeladuj")
    void podstawy() {
        Konfiguracja k = new Konfiguracja(Map.of("url", "http://a", "timeout", "5"));
        assertEquals("http://a", k.pobierz("url"));
        assertNull(k.pobierz("brak"));
        assertEquals(Integer.valueOf(2), k.czytaj(Map::size));
        k.przeladuj(Map.of("url", "http://b"));
        assertEquals("http://b", k.pobierz("url"));
        assertNull(k.pobierz("timeout"), "Po przeładowaniu stare klucze mają zniknąć");
    }

    @Test
    @DisplayName("2) 4 czytelników może być wewnątrz czytaj() w tym samym czasie")
    void czytelnicyRownoczesnie() throws Exception {
        Konfiguracja k = new Konfiguracja(wersja(1));
        CyclicBarrier wszyscyWSrodku = new CyclicBarrier(4);
        AtomicInteger spotkali = new AtomicInteger();
        List<Thread> t = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            t.add(demon(() -> k.czytaj(m -> {
                try {
                    wszyscyWSrodku.await(2, TimeUnit.SECONDS);
                    spotkali.incrementAndGet();
                } catch (Exception e) {
                    // nie doczekał się pozostałych czytelników
                }
                return null;
            })));
        }
        for (Thread w : t) {
            w.join(5000);
        }
        assertEquals(4, spotkali.get(), "Czytelnicy blokują się nawzajem: tylko " + spotkali.get() + " z 4 było w czytaj() naraz");
    }

    @Test
    @DisplayName("3) przeladuj() czeka, aż czytelnik skończy, a czytelnik nigdy nie widzi mieszanki wersji")
    void spojnosc() throws Exception {
        Konfiguracja k = new Konfiguracja(wersja(0));
        CountDownLatch czytelnikWSrodku = new CountDownLatch(1);
        CountDownLatch czytelnikMozeWyjsc = new CountDownLatch(1);
        AtomicBoolean przeladowane = new AtomicBoolean();
        Thread czytelnik = demon(() -> k.czytaj(m -> {
            czytelnikWSrodku.countDown();
            try {
                czytelnikMozeWyjsc.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        assertTrue(czytelnikWSrodku.await(2, TimeUnit.SECONDS), "Czytelnik nie wszedł do czytaj()");
        Thread pisarz = demon(() -> {
            k.przeladuj(wersja(1));
            przeladowane.set(true);
        });
        Thread.sleep(300);
        assertFalse(przeladowane.get(), "przeladuj() nie poczekało na czytelnika, który był w czytaj()");
        czytelnikMozeWyjsc.countDown();
        pisarz.join(2000);
        czytelnik.join(2000);
        assertTrue(przeladowane.get(), "przeladuj() nie skończyło się po wyjściu czytelnika");

        // Teraz wiele przeładowań i odczytów naraz: każdy odczyt ma widzieć jedną wersję.
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<String> mieszanka = new AtomicReference<>();
        List<Thread> czytelnicy = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            czytelnicy.add(demon(() -> {
                while (!stop.get()) {
                    String wynik = k.czytaj(m -> {
                        long rozne = m.values().stream().distinct().count();
                        return rozne == 1 && m.size() == 26 ? null : "rozmiar " + m.size() + ", wartości " + m.values().stream().distinct().toList();
                    });
                    if (wynik != null) {
                        mieszanka.compareAndSet(null, wynik);
                    }
                }
            }));
        }
        for (int n = 2; n < 3000; n++) {
            k.przeladuj(wersja(n));
        }
        stop.set(true);
        for (Thread c : czytelnicy) {
            c.join(2000);
        }
        assertNull(mieszanka.get(), () -> "Czytelnik zobaczył stan pośredni: " + mieszanka.get());
        assertEquals("v2999", k.pobierz("klucz.q"));
    }

    @Test
    @DisplayName("4) Wyjątek w funkcji przekazanej do czytaj() nie blokuje przeładowania")
    void wyjatekWCzytaniu() throws Exception {
        Konfiguracja k = new Konfiguracja(wersja(1));
        assertThrows(IllegalStateException.class, () -> k.czytaj(m -> {
            throw new IllegalStateException("błąd w kodzie czytelnika");
        }));
        Thread pisarz = demon(() -> k.przeladuj(wersja(2)));
        pisarz.join(2000);
        assertFalse(pisarz.isAlive(), "przeladuj() wisi po wyjątku w czytaj() (blokada nie została zwolniona?)");
        assertEquals("v2", k.pobierz("klucz.a"));
    }
}
