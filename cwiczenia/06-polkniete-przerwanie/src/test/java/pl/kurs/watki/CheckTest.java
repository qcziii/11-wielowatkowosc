package pl.kurs.watki;

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
    void mierzy() throws Exception {
        AtomicInteger wartosc = new AtomicInteger(40);
        MonitorDysku m = new MonitorDysku(wartosc::incrementAndGet);
        m.start();
        Thread.sleep(550);
        int pomiary = m.pomiary();
        m.stop();
        assertTrue(pomiary >= 4 && pomiary <= 8, "W 550 ms oczekiwano ok. 6 pomiarów, było " + pomiary);
        assertTrue(m.ostatniOdczyt() > 40);
    }

    @Test
    @DisplayName("2) stop() wyłącza monitor, który właśnie czeka w sleep")
    void stopPodczasSnu() throws Exception {
        MonitorDysku m = new MonitorDysku(() -> 50);
        m.start();
        Thread.sleep(250);
        assertTrue(m.stop(), "Wątek monitora dalej działa 1 s po stop()");
        int poStopie = m.pomiary();
        Thread.sleep(300);
        assertEquals(poStopie, m.pomiary(), "Monitor dalej mierzy po stop()");
    }

    @Test
    @DisplayName("3) stop() wyłącza monitor, który właśnie odczytuje czujnik")
    void stopPodczasOdczytu() throws Exception {
        // Odczyt czujnika trwa (aktywne czekanie, bez sleep), stop() trafia w trakcie odczytu.
        MonitorDysku m = new MonitorDysku(() -> {
            long koniec = System.nanoTime() + 80_000_000;
            while (System.nanoTime() < koniec) {
                Thread.onSpinWait();
            }
            return 70;
        });
        m.start();
        Thread.sleep(30);
        assertTrue(m.stop(), "Wątek monitora dalej działa 1 s po stop()");
    }
}
