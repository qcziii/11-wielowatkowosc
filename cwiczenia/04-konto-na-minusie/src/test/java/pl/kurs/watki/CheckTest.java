package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    @Test
    @DisplayName("1) Jeden wątek: wypłata ponad saldo się nie udaje")
    void jedenWatek() {
        Konto k = new Konto(100);
        assertTrue(k.wyplac(60));
        assertFalse(k.wyplac(60), "Wypłata 60 przy saldzie 40 powinna się nie udać");
        assertEquals(40, k.saldo());
        k.wplac(20);
        assertTrue(k.wyplac(60));
        assertEquals(0, k.saldo());
    }

    @Test
    @DisplayName("2) 8 wątków naraz wpłaca i wypłaca: saldo nigdy poniżej zera")
    void wieleWatkow() throws Exception {
        int watki = 8;
        int powtorzenia = 500_000;
        Konto konto = new Konto(0);
        AtomicInteger wplaty = new AtomicInteger();
        AtomicInteger udaneWyplaty = new AtomicInteger();
        AtomicInteger najnizszeSaldo = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        Thread[] t = new Thread[watki];
        for (int w = 0; w < watki; w++) {
            t[w] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int i = 0; i < powtorzenia; i++) {
                    konto.wplac(10);
                    wplaty.incrementAndGet();
                    // Dwie próby wypłaty na jedną wpłatę: druga zwykle powinna się nie udać.
                    for (int p = 0; p < 2; p++) {
                        if (konto.wyplac(10)) {
                            udaneWyplaty.incrementAndGet();
                            najnizszeSaldo.accumulateAndGet(konto.saldo(), Math::min);
                        }
                    }
                }
            });
            t[w].start();
        }
        start.countDown();
        for (Thread w : t) {
            w.join();
        }
        assertTrue(najnizszeSaldo.get() >= 0, "Saldo spadło do " + najnizszeSaldo.get()
                + ": wypłacono więcej, niż było na koncie");
        assertEquals(10 * (wplaty.get() - udaneWyplaty.get()), konto.saldo(),
                "Saldo nie zgadza się z liczbą wpłat i udanych wypłat");
        assertTrue(konto.saldo() >= 0, "Saldo końcowe ujemne: " + konto.saldo());
    }
}
