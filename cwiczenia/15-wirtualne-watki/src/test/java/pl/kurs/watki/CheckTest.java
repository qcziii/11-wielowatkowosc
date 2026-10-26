package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class CheckTest {

    /** Udaje API: każde wywołanie śpi 100 ms. Zapisuje, w jakich wątkach było wołane. */
    static class WolneApi implements ZrodloCen {
        final AtomicInteger wywolania = new AtomicInteger();
        final AtomicInteger platformowe = new AtomicInteger();
        final Set<Thread> watki = ConcurrentHashMap.newKeySet();

        @Override
        public BigDecimal cena(String produkt) {
            wywolania.incrementAndGet();
            Thread t = Thread.currentThread();
            watki.add(t);
            if (!t.isVirtual()) {
                platformowe.incrementAndGet();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("przerwano", e);
            }
            return new BigDecimal(produkt.substring(2)).movePointLeft(2);
        }
    }

    private static List<String> produkty(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> "p-" + i).toList();
    }

    @Test
    @DisplayName("1) 20 produktów: ceny poprawne, w kolejności")
    void poprawnosc() throws Exception {
        WolneApi api = new WolneApi();
        Map<String, BigDecimal> ceny = new PobieraczCen(api).pobierzWszystkie(produkty(20));
        assertEquals(produkty(20), new ArrayList<>(ceny.keySet()));
        assertEquals(new BigDecimal("0.07"), ceny.get("p-7"));
    }

    @Test
    @Timeout(value = 8, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @DisplayName("2) 2000 produktów po 100 ms: całość w mniej niż 3 s, w wątkach wirtualnych")
    void szybko() throws Exception {
        WolneApi api = new WolneApi();
        long start = System.nanoTime();
        Map<String, BigDecimal> ceny = new PobieraczCen(api).pobierzWszystkie(produkty(2000));
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(2000, ceny.size());
        assertEquals(0, api.platformowe.get(), api.platformowe.get() + " wywołań API poszło w zwykłych wątkach platformowych, a miały w wirtualnych");
        assertTrue(ms < 3000, "Trwało " + ms + " ms");
    }

    @Test
    @Timeout(value = 8, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @DisplayName("3) Po powrocie żaden wątek z pobierania już nie działa")
    void sprzatanie() throws Exception {
        WolneApi api = new WolneApi();
        new PobieraczCen(api).pobierzWszystkie(produkty(50));
        for (Thread t : api.watki) {
            t.join(1000);
            assertFalse(t.isAlive(), "Wątek " + t + " dalej działa");
        }
    }
}
