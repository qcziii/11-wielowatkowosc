package pl.kurs.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final TaskRunner runner = new TaskRunner();

    @Test
    @DisplayName("1) Zadania nie wykonują się w wątku, który wywołał uruchomRownolegle")
    void tasksInSeparateThreads() throws Exception {
        Thread caller = Thread.currentThread();
        Set<Thread> usedThreads = ConcurrentHashMap.newKeySet();
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> usedThreads.add(Thread.currentThread()));
        }
        runner.runInParallel(tasks);
        assertFalse(usedThreads.contains(caller),
                "Zadanie wykonało się w wątku testu (" + caller.getName() + "), a nie w nowym wątku");
        assertEquals(3, usedThreads.size(), "Każde zadanie ma mieć swój wątek, użyte wątki: " + usedThreads);
    }

    @Test
    @DisplayName("2) Wszystkie 4 zadania działają w tym samym momencie (spotykają się na barierze)")
    void tasksInParallel() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(4);
        AtomicInteger arrived = new AtomicInteger();
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> {
                try {
                    // The barrier only opens once all 4 tasks reach it at the same time.
                    barrier.await(1, TimeUnit.SECONDS);
                    arrived.incrementAndGet();
                } catch (Exception e) {
                    // the task did not meet the others
                }
            });
        }
        runner.runInParallel(tasks);
        assertEquals(4, arrived.get(),
                "Tylko " + arrived.get() + " z 4 zadań doczekało się pozostałych na barierze: zadania nie działały równocześnie");
    }

    @Test
    @DisplayName("3) uruchomRownolegle wraca dopiero, gdy wszystkie zadania się skończą")
    void waitsForAll() throws Exception {
        AtomicInteger finished = new AtomicInteger();
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                finished.incrementAndGet();
            });
        }
        long start = System.nanoTime();
        runner.runInParallel(tasks);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(3, finished.get(), "Metoda wróciła, zanim zadania się skończyły (skończone: " + finished.get() + " z 3)");
        assertTrue(ms < 800, "3 zadania po 300 ms równolegle powinny zająć ok. 300 ms, zajęły " + ms + " ms");
    }
}
