package pl.kurs.watki;

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

    private static final int WATKI = 8;
    private static final int NA_WATEK = 200_000;

    private static void naRaz(int watki, Runnable praca) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        Thread[] t = new Thread[watki];
        for (int i = 0; i < watki; i++) {
            t[i] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                praca.run();
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
    void jedenWatek() {
        LicznikOdwiedzin l = new LicznikOdwiedzin();
        for (int i = 0; i < 1000; i++) {
            l.odwiedz(i % 10 == 0);
        }
        assertEquals(1000, l.odwiedziny());
        assertEquals(100, l.unikalni());
    }

    @Test
    @DisplayName("2) 8 wątków po 200 000 odwiedzin: żadna się nie gubi")
    void wieleWatkow() throws Exception {
        LicznikOdwiedzin l = new LicznikOdwiedzin();
        naRaz(WATKI, () -> {
            for (int i = 0; i < NA_WATEK; i++) {
                l.odwiedz(i % 4 == 0);
            }
        });
        assertEquals(WATKI * NA_WATEK, l.odwiedziny(), "Zgubione odwiedziny");
        assertEquals(WATKI * NA_WATEK / 4, l.unikalni(), "Zgubieni unikalni użytkownicy");
    }
}
