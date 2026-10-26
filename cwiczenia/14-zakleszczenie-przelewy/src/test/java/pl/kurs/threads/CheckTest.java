package pl.kurs.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final Bank bank = new Bank();

    /** Starts the (daemon) threads, waits up to 5 s; if they hang, describes the deadlock. */
    private static void runAndWait(String name, List<Runnable> jobs) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            Thread t = new Thread(jobs.get(i), name + "-" + i);
            t.setDaemon(true);
            threads.add(t);
            t.start();
        }
        long deadline = System.currentTimeMillis() + 5000;
        for (Thread t : threads) {
            t.join(Math.max(1, deadline - System.currentTimeMillis()));
        }
        if (threads.stream().anyMatch(Thread::isAlive)) {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            long[] ids = mx.findDeadlockedThreads();
            String describe = ids == null ? "" : Arrays.stream(mx.getThreadInfo(ids))
                    .filter(i -> i.getThreadName().startsWith(name + "-"))
                    .map(CheckTest::describe).collect(Collectors.joining("; "));
            if (describe.isEmpty()) {
                describe = "(JVM nie wykrył zakleszczenia monitorów)";
            }
            fail("Przelewy nie skończyły się w 5 s. Zakleszczenie: " + describe);
        }
    }

    private static String describe(ThreadInfo i) {
        return i.getThreadName() + " czeka na " + i.getLockName() + ", który trzyma " + i.getLockOwnerName();
    }

    @Test
    @DisplayName("1) Jeden wątek: przelew i brak środków")
    void basics() {
        Account a = new Account(1, 100);
        Account b = new Account(2, 0);
        assertTrue(bank.transfer(a, b, 70));
        assertFalse(bank.transfer(a, b, 70));
        assertEquals(30, a.balance());
        assertEquals(70, b.balance());
        assertTrue(bank.transfer(a, a, 10), "Przelew na ten sam rachunek: saldo się nie zmienia");
        assertEquals(30, a.balance());
    }

    @Test
    @DisplayName("2) Dwa wątki przelewają naraz A->B i B->A (po 100 000 razy): nic nie zawisa")
    void bothDirections() throws Exception {
        Account a = new Account(1, 1_000);
        Account b = new Account(2, 1_000);
        runAndWait("a-b", List.of(
                () -> { for (int i = 0; i < 100_000; i++) bank.transfer(a, b, 1); },
                () -> { for (int i = 0; i < 100_000; i++) bank.transfer(b, a, 1); }));
        assertEquals(2_000, a.balance() + b.balance(), "Suma pieniędzy w banku się zmieniła");
    }

    @Test
    @DisplayName("3) 8 wątków, losowe przelewy między 5 rachunkami: nic nie zawisa, suma się zgadza")
    void randomTransfers() throws Exception {
        Account[] r = new Account[5];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Account(100 - i * 7, 10_000);
        }
        List<Runnable> jobs = new ArrayList<>();
        for (int w = 0; w < 8; w++) {
            jobs.add(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < 50_000; i++) {
                    bank.transfer(r[random.nextInt(r.length)], r[random.nextInt(r.length)], random.nextInt(1, 100));
                }
            });
        }
        runAndWait("losowe", jobs);
        long sum = Arrays.stream(r).mapToLong(Account::balance).sum();
        assertEquals(50_000, sum, "Suma pieniędzy w banku się zmieniła");
        assertTrue(Arrays.stream(r).allMatch(x -> x.balance() >= 0), "Ujemne saldo: " + Arrays.toString(Arrays.stream(r).mapToLong(Account::balance).toArray()));
    }
}
