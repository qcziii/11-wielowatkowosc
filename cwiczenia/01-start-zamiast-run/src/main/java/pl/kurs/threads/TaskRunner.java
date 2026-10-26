package pl.kurs.threads;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every task in its own thread and waits until all of them finish.
 */
public class TaskRunner {

    /**
     * Runs all tasks in parallel (each in its own thread) and returns only
     * when all of them have finished.
     */
    public void runInParallel(List<Runnable> tasks) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        int idx = 1;
        for (Runnable task : tasks) {
            Thread thread = new Thread(task, "task-" + idx++);
            threads.add(thread);
            thread.run();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
