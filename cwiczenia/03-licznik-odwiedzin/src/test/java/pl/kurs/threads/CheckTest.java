package pl.kurs.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 200_000;

    private static void runConcurrently(int threads, Runnable work) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        Thread[] t = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            t[i] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                work.run();
            });
            t[i].start();
        }
        start.countDown();
        for (Thread w : t) {
            w.join();
        }
    }

    @Test
    @DisplayName("1) Jeden wątek: liczy poprawnie")
    void singleThread() {
        VisitCounter l = new VisitCounter();
        for (int i = 0; i < 1000; i++) {
            l.visit(i % 10 == 0);
        }
        assertEquals(1000, l.visits());
        assertEquals(100, l.uniqueUsers());
    }

    @Test
    @DisplayName("2) 8 wątków po 200 000 odwiedzin: żadna się nie gubi")
    void manyThreads() throws Exception {
        VisitCounter l = new VisitCounter();
        runConcurrently(THREADS, () -> {
            for (int i = 0; i < PER_THREAD; i++) {
                l.visit(i % 4 == 0);
            }
        });
        assertEquals(THREADS * PER_THREAD, l.visits(), "Zgubione odwiedziny");
        assertEquals(THREADS * PER_THREAD / 4, l.uniqueUsers(), "Zgubieni unikalni użytkownicy");
    }
}
