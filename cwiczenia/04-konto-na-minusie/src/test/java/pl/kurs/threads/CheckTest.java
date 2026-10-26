package pl.kurs.threads;

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
    void singleThread() {
        Account k = new Account(100);
        assertTrue(k.withdraw(60));
        assertFalse(k.withdraw(60), "Wypłata 60 przy saldzie 40 powinna się nie udać");
        assertEquals(40, k.balance());
        k.deposit(20);
        assertTrue(k.withdraw(60));
        assertEquals(0, k.balance());
    }

    @Test
    @DisplayName("2) 8 wątków naraz wpłaca i wypłaca: saldo nigdy poniżej zera")
    void manyThreads() throws Exception {
        int threads = 8;
        int iterations = 500_000;
        Account account = new Account(0);
        AtomicInteger deposits = new AtomicInteger();
        AtomicInteger successfulWithdrawals = new AtomicInteger();
        AtomicInteger lowestBalance = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        Thread[] t = new Thread[threads];
        for (int w = 0; w < threads; w++) {
            t[w] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int i = 0; i < iterations; i++) {
                    account.deposit(10);
                    deposits.incrementAndGet();
                    // Two withdrawal attempts per deposit: the second one should usually fail.
                    for (int p = 0; p < 2; p++) {
                        if (account.withdraw(10)) {
                            successfulWithdrawals.incrementAndGet();
                            lowestBalance.accumulateAndGet(account.balance(), Math::min);
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
        assertTrue(lowestBalance.get() >= 0, "Saldo spadło do " + lowestBalance.get()
                + ": wypłacono więcej, niż było na koncie");
        assertEquals(10 * (deposits.get() - successfulWithdrawals.get()), account.balance(),
                "Saldo nie zgadza się z liczbą wpłat i udanych wypłat");
        assertTrue(account.balance() >= 0, "Saldo końcowe ujemne: " + account.balance());
    }
}
