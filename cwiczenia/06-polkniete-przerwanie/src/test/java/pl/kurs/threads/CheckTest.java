package pl.kurs.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    @Test
    @DisplayName("1) Monitor mierzy co ok. 100 ms")
    void measures() throws Exception {
        AtomicInteger value = new AtomicInteger(40);
        DiskMonitor m = new DiskMonitor(value::incrementAndGet);
        m.start();
        Thread.sleep(550);
        int measurements = m.measurements();
        m.stop();
        assertTrue(measurements >= 4 && measurements <= 8, "W 550 ms oczekiwano ok. 6 pomiarów, było " + measurements);
        assertTrue(m.lastReading() > 40);
    }

    @Test
    @DisplayName("2) stop() wyłącza monitor, który właśnie czeka w sleep")
    void stopDuringSleep() throws Exception {
        DiskMonitor m = new DiskMonitor(() -> 50);
        m.start();
        Thread.sleep(250);
        assertTrue(m.stop(), "Wątek monitora dalej działa 1 s po stop()");
        int afterStop = m.measurements();
        Thread.sleep(300);
        assertEquals(afterStop, m.measurements(), "Monitor dalej mierzy po stop()");
    }

    @Test
    @DisplayName("3) stop() wyłącza monitor, który właśnie odczytuje czujnik")
    void stopDuringReading() throws Exception {
        // Reading the sensor takes a while (busy wait, no sleep), stop() arrives mid-read.
        DiskMonitor m = new DiskMonitor(() -> {
            long deadline = System.nanoTime() + 80_000_000;
            while (System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            return 70;
        });
        m.start();
        Thread.sleep(30);
        assertTrue(m.stop(), "Wątek monitora dalej działa 1 s po stop()");
    }
}
