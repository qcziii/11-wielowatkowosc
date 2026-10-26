package pl.kurs.threads;

/**
 * The generator runs in a separate thread and computes simulation steps in a loop
 * until another thread calls stop().
 */
public class Generator implements Runnable {

    private boolean running = true;
    private long steps;

    @Override
    public void run() {
        while (running) {
            steps++;
        }
    }

    /** Called from another thread (e.g. the thread handling the "Stop" button). */
    public void stop() {
        running = false;
    }

    /** Number of steps done. Read after the generator thread has finished. */
    public long steps() {
        return steps;
    }
}
