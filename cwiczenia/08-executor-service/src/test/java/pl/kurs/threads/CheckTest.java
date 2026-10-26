package pl.kurs.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final ParallelProcessor p = new ParallelProcessor();

    /** Test function: "works" for a moment and records how many calls run at once and on which threads. */
    static class Observer<T, R> implements Function<T, R> {
        final Function<T, R> f;
        final long workMs;
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger max = new AtomicInteger();
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();

        Observer(long workMs, Function<T, R> f) {
            this.workMs = workMs;
            this.f = f;
        }

        @Override
        public R apply(T t) {
            threads.add(Thread.currentThread());
            max.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                Thread.sleep(workMs);
                return f.apply(t);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            } finally {
                active.decrementAndGet();
            }
        }

        void assertThreadsFinished() throws InterruptedException {
            for (Thread t : threads) {
                t.join(1000);
                assertFalse(t.isAlive(), "Wątek puli " + t.getName() + " dalej działa po powrocie z process() (pula nie zamknięta?)");
            }
        }
    }

    @Test
    @DisplayName("1) Wyniki w kolejności danych wejściowych")
    void preservesOrder() {
        List<Integer> data = IntStream.rangeClosed(1, 50).boxed().toList();
        // Earlier elements work longer, so they finish later.
        Function<Integer, String> f = i -> {
            try {
                Thread.sleep(60 - i);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "#" + i;
        };
        List<String> result = p.process(data, f, 4);
        assertEquals(data.stream().map(i -> "#" + i).toList(), result);
    }

    @Test
    @DisplayName("2) Równolegle, ale nie więcej niż liczbaWatkow naraz (24 elementy po 100 ms, 4 wątki)")
    void parallel() {
        Observer<Integer, Integer> f = new Observer<>(100, i -> i * i);
        long start = System.nanoTime();
        List<Integer> result = p.process(IntStream.range(0, 24).boxed().toList(), f, 4);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(IntStream.range(0, 24).map(i -> i * i).boxed().toList(), result);
        assertEquals(4, f.max.get(), "Naraz działało maksymalnie " + f.max.get() + " wywołań, a miały 4");
        assertTrue(ms < 1500, "24 x 100 ms na 4 wątkach to ok. 600 ms, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("3) Po powrocie nie zostają działające wątki puli")
    void threadsFinished() throws Exception {
        Observer<Integer, Integer> f = new Observer<>(10, i -> i);
        p.process(IntStream.range(0, 20).boxed().toList(), f, 3);
        f.assertThreadsFinished();
    }

    @Test
    @DisplayName("4) Wyjątek z funkcji: ProcessingException z oryginalną przyczyną, pula też zamknięta")
    void exception() throws Exception {
        Observer<Integer, Integer> f = new Observer<>(10, i -> {
            if (i == 7) {
                throw new IllegalArgumentException("zły element 7");
            }
            return i;
        });
        ProcessingException e = assertThrows(ProcessingException.class,
                () -> p.process(IntStream.range(0, 20).boxed().toList(), f, 3));
        assertInstanceOf(IllegalArgumentException.class, e.getCause(),
                "Przyczyną ma być oryginalny wyjątek z funkcji, a jest: " + e.getCause());
        assertEquals("zły element 7", e.getCause().getMessage());
        f.assertThreadsFinished();
    }

    @Test
    @DisplayName("5) Pusta lista: pusty wynik")
    void emptyList() {
        assertEquals(List.of(), p.process(List.<Integer>of(), i -> i, 4));
    }
}
