package pl.kurs.threads;

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

    /** Fake API: every call sleeps 100 ms. Records the threads it was called on. */
    static class SlowApi implements PriceSource {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger platformCalls = new AtomicInteger();
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();

        @Override
        public BigDecimal price(String product) {
            calls.incrementAndGet();
            Thread t = Thread.currentThread();
            threads.add(t);
            if (!t.isVirtual()) {
                platformCalls.incrementAndGet();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            }
            return new BigDecimal(product.substring(2)).movePointLeft(2);
        }
    }

    private static List<String> products(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> "p-" + i).toList();
    }

    @Test
    @DisplayName("1) 20 produktów: ceny poprawne, w kolejności")
    void correctness() throws Exception {
        SlowApi api = new SlowApi();
        Map<String, BigDecimal> prices = new PriceFetcher(api).fetchAll(products(20));
        assertEquals(products(20), new ArrayList<>(prices.keySet()));
        assertEquals(new BigDecimal("0.07"), prices.get("p-7"));
    }

    @Test
    @Timeout(value = 8, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @DisplayName("2) 2000 produktów po 100 ms: całość w mniej niż 3 s, w wątkach wirtualnych")
    void fast() throws Exception {
        SlowApi api = new SlowApi();
        long start = System.nanoTime();
        Map<String, BigDecimal> prices = new PriceFetcher(api).fetchAll(products(2000));
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(2000, prices.size());
        assertEquals(0, api.platformCalls.get(), api.platformCalls.get() + " wywołań API poszło w zwykłych wątkach platformowych, a miały w wirtualnych");
        assertTrue(ms < 3000, "Trwało " + ms + " ms");
    }

    @Test
    @Timeout(value = 8, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @DisplayName("3) Po powrocie żaden wątek z pobierania już nie działa")
    void cleanup() throws Exception {
        SlowApi api = new SlowApi();
        new PriceFetcher(api).fetchAll(products(50));
        for (Thread t : api.threads) {
            t.join(1000);
            assertFalse(t.isAlive(), "Wątek " + t + " dalej działa");
        }
    }
}
