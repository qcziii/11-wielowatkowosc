package pl.kurs.watki;

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

    /** Uruchamia wątki (demony), czeka maksymalnie 5 s; jeśli wiszą, opisuje zakleszczenie. */
    private static void uruchomICzekaj(String nazwa, List<Runnable> prace) throws InterruptedException {
        List<Thread> watki = new ArrayList<>();
        for (int i = 0; i < prace.size(); i++) {
            Thread t = new Thread(prace.get(i), nazwa + "-" + i);
            t.setDaemon(true);
            watki.add(t);
            t.start();
        }
        long koniec = System.currentTimeMillis() + 5000;
        for (Thread t : watki) {
            t.join(Math.max(1, koniec - System.currentTimeMillis()));
        }
        if (watki.stream().anyMatch(Thread::isAlive)) {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            long[] ids = mx.findDeadlockedThreads();
            String opis = ids == null ? "" : Arrays.stream(mx.getThreadInfo(ids))
                    .filter(i -> i.getThreadName().startsWith(nazwa + "-"))
                    .map(CheckTest::opis).collect(Collectors.joining("; "));
            if (opis.isEmpty()) {
                opis = "(JVM nie wykrył zakleszczenia monitorów)";
            }
            fail("Przelewy nie skończyły się w 5 s. Zakleszczenie: " + opis);
        }
    }

    private static String opis(ThreadInfo i) {
        return i.getThreadName() + " czeka na " + i.getLockName() + ", który trzyma " + i.getLockOwnerName();
    }

    @Test
    @DisplayName("1) Jeden wątek: przelew i brak środków")
    void podstawy() {
        Rachunek a = new Rachunek(1, 100);
        Rachunek b = new Rachunek(2, 0);
        assertTrue(bank.przelej(a, b, 70));
        assertFalse(bank.przelej(a, b, 70));
        assertEquals(30, a.saldo());
        assertEquals(70, b.saldo());
        assertTrue(bank.przelej(a, a, 10), "Przelew na ten sam rachunek: saldo się nie zmienia");
        assertEquals(30, a.saldo());
    }

    @Test
    @DisplayName("2) Dwa wątki przelewają naraz A->B i B->A (po 100 000 razy): nic nie zawisa")
    void wObieStrony() throws Exception {
        Rachunek a = new Rachunek(1, 1_000);
        Rachunek b = new Rachunek(2, 1_000);
        uruchomICzekaj("a-b", List.of(
                () -> { for (int i = 0; i < 100_000; i++) bank.przelej(a, b, 1); },
                () -> { for (int i = 0; i < 100_000; i++) bank.przelej(b, a, 1); }));
        assertEquals(2_000, a.saldo() + b.saldo(), "Suma pieniędzy w banku się zmieniła");
    }

    @Test
    @DisplayName("3) 8 wątków, losowe przelewy między 5 rachunkami: nic nie zawisa, suma się zgadza")
    void losowe() throws Exception {
        Rachunek[] r = new Rachunek[5];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Rachunek(100 - i * 7, 10_000);
        }
        List<Runnable> prace = new ArrayList<>();
        for (int w = 0; w < 8; w++) {
            prace.add(() -> {
                ThreadLocalRandom los = ThreadLocalRandom.current();
                for (int i = 0; i < 50_000; i++) {
                    bank.przelej(r[los.nextInt(r.length)], r[los.nextInt(r.length)], los.nextInt(1, 100));
                }
            });
        }
        uruchomICzekaj("losowe", prace);
        long suma = Arrays.stream(r).mapToLong(Rachunek::saldo).sum();
        assertEquals(50_000, suma, "Suma pieniędzy w banku się zmieniła");
        assertTrue(Arrays.stream(r).allMatch(x -> x.saldo() >= 0), "Ujemne saldo: " + Arrays.toString(Arrays.stream(r).mapToLong(Rachunek::saldo).toArray()));
    }
}
