package pl.kurs.threads;

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

    private static Map<String, String> version(int n) {
        Map<String, String> m = new HashMap<>();
        for (char c = 'a'; c <= 'z'; c++) {
            m.put("klucz." + c, "v" + n);
        }
        return m;
    }

    private static Thread daemon(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
        return t;
    }

    @Test
    @DisplayName("1) Jeden wątek: pobierz, czytaj, przeladuj")
    void basics() {
        Configuration k = new Configuration(Map.of("url", "http://a", "timeout", "5"));
        assertEquals("http://a", k.get("url"));
        assertNull(k.get("brak"));
        assertEquals(Integer.valueOf(2), k.read(Map::size));
        k.reload(Map.of("url", "http://b"));
        assertEquals("http://b", k.get("url"));
        assertNull(k.get("timeout"), "Po przeładowaniu stare klucze mają zniknąć");
    }

    @Test
    @DisplayName("2) 4 czytelników może być wewnątrz read() w tym samym czasie")
    void concurrentReaders() throws Exception {
        Configuration k = new Configuration(version(1));
        CyclicBarrier allInside = new CyclicBarrier(4);
        AtomicInteger met = new AtomicInteger();
        List<Thread> t = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            t.add(daemon(() -> k.read(m -> {
                try {
                    allInside.await(2, TimeUnit.SECONDS);
                    met.incrementAndGet();
                } catch (Exception e) {
                    // did not meet the other readers
                }
                return null;
            })));
        }
        for (Thread w : t) {
            w.join(5000);
        }
        assertEquals(4, met.get(), "Czytelnicy blokują się nawzajem: tylko " + met.get() + " z 4 było w read() naraz");
    }

    @Test
    @DisplayName("3) reload() czeka, aż czytelnik skończy, a czytelnik nigdy nie widzi mieszanki wersji")
    void consistency() throws Exception {
        Configuration k = new Configuration(version(0));
        CountDownLatch readerInside = new CountDownLatch(1);
        CountDownLatch readerMayLeave = new CountDownLatch(1);
        AtomicBoolean reloaded = new AtomicBoolean();
        Thread reader = daemon(() -> k.read(m -> {
            readerInside.countDown();
            try {
                readerMayLeave.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        assertTrue(readerInside.await(2, TimeUnit.SECONDS), "Czytelnik nie wszedł do read()");
        Thread writer = daemon(() -> {
            k.reload(version(1));
            reloaded.set(true);
        });
        Thread.sleep(300);
        assertFalse(reloaded.get(), "reload() nie poczekało na czytelnika, który był w read()");
        readerMayLeave.countDown();
        writer.join(2000);
        reader.join(2000);
        assertTrue(reloaded.get(), "reload() nie skończyło się po wyjściu czytelnika");

        // Now many reloads and reads at once: every read must see a single version.
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<String> mixed = new AtomicReference<>();
        List<Thread> readers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            readers.add(daemon(() -> {
                while (!stop.get()) {
                    String result = k.read(m -> {
                        long distinctCount = m.values().stream().distinct().count();
                        return distinctCount == 1 && m.size() == 26 ? null : "rozmiar " + m.size() + ", wartości " + m.values().stream().distinct().toList();
                    });
                    if (result != null) {
                        mixed.compareAndSet(null, result);
                    }
                }
            }));
        }
        for (int n = 2; n < 3000; n++) {
            k.reload(version(n));
        }
        stop.set(true);
        for (Thread c : readers) {
            c.join(2000);
        }
        assertNull(mixed.get(), () -> "Czytelnik zobaczył stan pośredni: " + mixed.get());
        assertEquals("v2999", k.get("klucz.q"));
    }

    @Test
    @DisplayName("4) Wyjątek w funkcji przekazanej do read() nie blokuje przeładowania")
    void exceptionWhileReading() throws Exception {
        Configuration k = new Configuration(version(1));
        assertThrows(IllegalStateException.class, () -> k.read(m -> {
            throw new IllegalStateException("błąd w kodzie czytelnika");
        }));
        Thread writer = daemon(() -> k.reload(version(2)));
        writer.join(2000);
        assertFalse(writer.isAlive(), "reload() wisi po wyjątku w read() (blokada nie została zwolniona?)");
        assertEquals("v2", k.get("klucz.a"));
    }
}
