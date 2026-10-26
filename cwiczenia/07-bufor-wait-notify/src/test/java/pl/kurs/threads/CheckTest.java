package pl.kurs.threads;

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

    /** Waits (up to 2 s) until the thread is WAITING. */
    private static void awaitWaitingState(Thread t, AtomicReference<Throwable> error) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (t.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertNull(error.get(), () -> "Wątek " + t.getName() + " rzucił wyjątek: " + error.get());
        assertEquals(Thread.State.WAITING, t.getState(), "Wątek " + t.getName() + " nie czeka (wait), tylko jest w stanie " + t.getState());
    }

    private static Thread thread(String name, Action action, AtomicReference<Throwable> error) {
        Thread t = new Thread(() -> {
            try {
                action.execute();
            } catch (Throwable e) {
                error.compareAndSet(null, e);
            }
        }, name);
        t.setDaemon(true);
        return t;
    }

    interface Action {
        void execute() throws Exception;
    }

    @Test
    @DisplayName("1) Jeden wątek: kolejność FIFO i rozmiar")
    void fifo() throws Exception {
        BoundedBuffer<String> b = new BoundedBuffer<>(3);
        assertEquals(0, b.size());
        b.put("a");
        b.put("b");
        b.put("c");
        assertEquals(3, b.size());
        assertEquals("a", b.take());
        assertEquals("b", b.take());
        b.put("d");
        assertEquals("c", b.take());
        assertEquals("d", b.take());
        assertEquals(0, b.size());
    }

    @Test
    @DisplayName("2) take() na pustym buforze czeka (wait, bez zjadania procesora), aż ktoś włoży element")
    void takeWaits() throws Exception {
        BoundedBuffer<String> b = new BoundedBuffer<>(2);
        AtomicReference<String> received = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread consumer = thread("consumer", () -> received.set(b.take()), error);
        consumer.start();
        awaitWaitingState(consumer, error);
        assertNull(received.get(), "take() wrócił z pustego bufora");
        b.put("x");
        consumer.join(2000);
        assertNull(error.get(), () -> "Konsument rzucił wyjątek: " + error.get());
        assertEquals("x", received.get(), "Konsument nie dostał elementu po put()");
    }

    @Test
    @DisplayName("3) put() na pełnym buforze czeka, aż ktoś wyjmie element")
    void putWaits() throws Exception {
        BoundedBuffer<Integer> b = new BoundedBuffer<>(2);
        b.put(1);
        b.put(2);
        AtomicBoolean putDone = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread producer = thread("producer", () -> {
            b.put(3);
            putDone.set(true);
        }, error);
        producer.start();
        awaitWaitingState(producer, error);
        assertFalse(putDone.get(), "put() wrócił, choć bufor był pełny");
        assertEquals(2, b.size(), "Pełny bufor (pojemność 2) ma więcej elementów niż pojemność");
        assertEquals(1, b.take());
        producer.join(2000);
        assertNull(error.get(), () -> "Producent rzucił wyjątek: " + error.get());
        assertTrue(putDone.get(), "Producent nie włożył elementu po zwolnieniu miejsca");
        assertEquals(2, b.take());
        assertEquals(3, b.take());
    }

    @Test
    @DisplayName("4) Przerwany wątek czekający w take() dostaje InterruptedException")
    void interruption() throws Exception {
        BoundedBuffer<String> b = new BoundedBuffer<>(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread consumer = thread("consumer", b::take, error);
        consumer.start();
        awaitWaitingState(consumer, error);
        consumer.interrupt();
        consumer.join(2000);
        assertFalse(consumer.isAlive(), "Przerwany konsument dalej czeka");
        assertInstanceOf(InterruptedException.class, error.get(), "Oczekiwano InterruptedException");
    }

    @Test
    @DisplayName("5) 4 producentów i 4 konsumentów, mały bufor: każdy element odebrany dokładnie raz, nic nie zawisa")
    void manyProducersAndConsumers() throws Exception {
        int producers = 4, consumers = 4, perProducer = 50_000;
        for (int capacity : new int[]{1, 3}) {
            BoundedBuffer<Integer> b = new BoundedBuffer<>(capacity);
            AtomicLong sum = new AtomicLong();
            AtomicInteger received = new AtomicInteger();
            AtomicInteger violations = new AtomicInteger();
            AtomicReference<Throwable> error = new AtomicReference<>();
            ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<>();
            for (int p = 0; p < producers; p++) {
                int base = p * perProducer;
                threads.add(thread("producer-" + p, () -> {
                    for (int i = 1; i <= perProducer; i++) {
                        b.put(base + i);
                    }
                }, error));
            }
            for (int k = 0; k < consumers; k++) {
                threads.add(thread("consumer-" + k, () -> {
                    for (int i = 0; i < perProducer; i++) {
                        int r = b.size();
                        if (r > capacity || r < 0) {
                            violations.incrementAndGet();
                        }
                        sum.addAndGet(b.take());
                        received.incrementAndGet();
                    }
                }, error));
            }
            threads.forEach(Thread::start);
            for (Thread t : threads) {
                t.join(10_000);
                assertFalse(t.isAlive(), "Wątek " + t.getName() + " zawisł w stanie " + t.getState()
                        + " (pojemność " + capacity + ", odebrano " + received.get() + " z " + producers * perProducer + ")");
            }
            assertNull(error.get(), () -> "Wątek rzucił wyjątek: " + error.get());
            long n = (long) producers * perProducer;
            assertEquals(n, received.get());
            assertEquals(n * (n + 1) / 2, sum.get(), "Suma odebranych elementów się nie zgadza: coś zgubione albo zdublowane");
            assertEquals(0, violations.get(), "Tyle razy size() pokazał więcej niż pojemność albo mniej niż 0");
            assertEquals(0, b.size());
        }
    }
}
