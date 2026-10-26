package pl.kurs.threads;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

/**
 * Checks disk usage every 100 ms and remembers the last reading.
 * Runs in a separate thread, started by start() and shut down by stop().
 */
public class DiskMonitor {

    private final IntSupplier sensor;
    private final AtomicInteger measurements = new AtomicInteger();
    private volatile int lastReading = -1;
    private Thread thread;

    public DiskMonitor(IntSupplier sensor) {
        this.sensor = sensor;
    }

    public void start() {
        thread = new Thread(this::loop, "disk-monitor");
        thread.start();
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            lastReading = sensor.getAsInt();
            measurements.incrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("Monitor: sleep interrupted");
            }
        }
        System.out.println("Monitor: finished");
    }

    /**
     * Shuts the monitor down and waits for its thread to finish (at most 1 s).
     *
     * @return true if the monitor thread has finished
     */
    public boolean stop() throws InterruptedException {
        thread.interrupt();
        thread.join(1000);
        return !thread.isAlive();
    }

    public int measurements() {
        return measurements.get();
    }

    public int lastReading() {
        return lastReading;
    }
}
