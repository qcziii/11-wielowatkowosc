package pl.kurs.threads;

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

    private static final String[] CATEGORIES = {"książki", "elektronika", "dom"};

    /** Source of n orders with ids 1..n and value id (category id % 3). Tracks who read how many. */
    static class CountingSource implements Iterator<Order> {
        final int n;
        final AtomicInteger readCount = new AtomicInteger();
        final Set<Thread> readerThreads = ConcurrentHashMap.newKeySet();

        CountingSource(int n) {
            this.n = n;
        }

        @Override
        public boolean hasNext() {
            readerThreads.add(Thread.currentThread());
            return readCount.get() < n;
        }

        @Override
        public Order next() {
            readerThreads.add(Thread.currentThread());
            int id = readCount.incrementAndGet();
            if (id > n) {
                throw new NoSuchElementException();
            }
            return new Order(id, CATEGORIES[id % 3], BigDecimal.valueOf(id));
        }
    }

    /** Test handler: sleeps, measures concurrency, records handled ids and threads. */
    static class RecordingHandler implements OrderHandler {
        final long ms;
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger max = new AtomicInteger();
        final AtomicInteger started = new AtomicInteger();
        final Map<Long, Integer> handled = new ConcurrentHashMap<>();
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();
        volatile CountingSource source;
        final AtomicInteger maxReadAhead = new AtomicInteger();

        RecordingHandler(long ms) {
            this.ms = ms;
        }

        @Override
        public void handle(Order z) throws Exception {
            threads.add(Thread.currentThread());
            int start = started.incrementAndGet();
            if (source != null) {
                maxReadAhead.accumulateAndGet(source.readCount.get() - start, Math::max);
            }
            max.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                handled.merge(z.id(), 1, Integer::sum);
                if (ms > 0) {
                    Thread.sleep(ms);
                }
                if (z.id() % 10 == 0) {
                    throw new IllegalStateException("błąd zamówienia " + z.id());
                }
            } finally {
                active.decrementAndGet();
            }
        }
    }

    @Test
    @DisplayName("1) 10 000 zamówień: każde obsłużone dokładnie raz, raport się zgadza")
    void report() throws Exception {
        RecordingHandler o = new RecordingHandler(0);
        Report r = new OrderProcessor(4, 50, o).process(new CountingSource(10_000));
        assertEquals(10_000, o.handled.size(), "Liczba różnych obsłużonych zamówień");
        assertTrue(o.handled.values().stream().allMatch(v -> v == 1), "Któreś zamówienie obsłużone więcej niż raz");
        assertEquals(9_000, r.processed());
        assertEquals(1_000, r.errors(), "Co dziesiąte zamówienie kończy się błędem");
        Map<String, BigDecimal> expected = new java.util.HashMap<>();
        for (long id = 1; id <= 10_000; id++) {
            if (id % 10 != 0) {
                expected.merge(CATEGORIES[(int) (id % 3)], BigDecimal.valueOf(id), BigDecimal::add);
            }
        }
        assertEquals(expected, r.sumsByCategory());
    }

    @Test
    @DisplayName("2) Konsumenci pracują równolegle: dokładnie 4 obsługi naraz (200 zamówień po 20 ms)")
    void parallel() throws Exception {
        RecordingHandler o = new RecordingHandler(20);
        long start = System.nanoTime();
        new OrderProcessor(4, 10, o).process(new CountingSource(200));
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(4, o.max.get(), "Naraz działało " + o.max.get() + " obsług, konsumentów jest 4");
        assertTrue(ms < 2500, "200 x 20 ms na 4 konsumentach to ok. 1 s, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("3) Źródło czyta tylko jeden wątek i nie czyta na zapas więcej, niż mieści kolejka")
    void boundedQueue() throws Exception {
        RecordingHandler o = new RecordingHandler(5);
        CountingSource z = new CountingSource(300);
        o.source = z;
        new OrderProcessor(3, 10, o).process(z);
        assertEquals(1, z.readerThreads.size(), "Źródło czytało " + z.readerThreads.size() + " wątków: " + z.readerThreads);
        // Read-ahead: 10 in the queue + 3 held by consumers + 1 in the producer's hand (with a small margin).
        assertTrue(o.maxReadAhead.get() <= 10 + 3 + 2,
                "Producent przeczytał na zapas " + o.maxReadAhead.get() + " zamówień, a kolejka ma pojemność 10");
    }

    @Test
    @DisplayName("4) Po powrocie wszystkie wątki konsumentów i producenta są zakończone")
    void cleanup() throws Exception {
        RecordingHandler o = new RecordingHandler(1);
        CountingSource z = new CountingSource(100);
        new OrderProcessor(4, 10, o).process(z);
        for (Thread t : o.threads) {
            t.join(1000);
            assertFalse(t.isAlive(), "Konsument " + t.getName() + " dalej działa, stan " + t.getState());
        }
        for (Thread t : z.readerThreads) {
            if (t != Thread.currentThread()) {
                t.join(1000);
                assertFalse(t.isAlive(), "Producent " + t.getName() + " dalej działa, stan " + t.getState());
            }
        }
    }

    @Test
    @DisplayName("5) Puste źródło: pusty raport, nic nie zawisa")
    void emptySource() throws Exception {
        Report r = new OrderProcessor(4, 10, new RecordingHandler(0)).process(new CountingSource(0));
        assertEquals(new Report(0, 0, Map.of()), r);
    }
}
