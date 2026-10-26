package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final UruchamiaczZadan uruchamiacz = new UruchamiaczZadan();

    @Test
    @DisplayName("1) Zadania nie wykonują się w wątku, który wywołał uruchomRownolegle")
    void zadaniaWOsobnychWatkach() throws Exception {
        Thread wolajacy = Thread.currentThread();
        Set<Thread> uzyte = ConcurrentHashMap.newKeySet();
        List<Runnable> zadania = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            zadania.add(() -> uzyte.add(Thread.currentThread()));
        }
        uruchamiacz.uruchomRownolegle(zadania);
        assertFalse(uzyte.contains(wolajacy),
                "Zadanie wykonało się w wątku testu (" + wolajacy.getName() + "), a nie w nowym wątku");
        assertEquals(3, uzyte.size(), "Każde zadanie ma mieć swój wątek, użyte wątki: " + uzyte);
    }

    @Test
    @DisplayName("2) Wszystkie 4 zadania działają w tym samym momencie (spotykają się na barierze)")
    void zadaniaRownolegle() throws Exception {
        CyclicBarrier bariera = new CyclicBarrier(4);
        AtomicInteger doczekaly = new AtomicInteger();
        List<Runnable> zadania = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            zadania.add(() -> {
                try {
                    // Bariera puści dalej dopiero, gdy dojdą do niej wszystkie 4 zadania naraz.
                    bariera.await(1, TimeUnit.SECONDS);
                    doczekaly.incrementAndGet();
                } catch (Exception e) {
                    // zadanie nie doczekało się pozostałych
                }
            });
        }
        uruchamiacz.uruchomRownolegle(zadania);
        assertEquals(4, doczekaly.get(),
                "Tylko " + doczekaly.get() + " z 4 zadań doczekało się pozostałych na barierze: zadania nie działały równocześnie");
    }

    @Test
    @DisplayName("3) uruchomRownolegle wraca dopiero, gdy wszystkie zadania się skończą")
    void czekaNaKoniec() throws Exception {
        AtomicInteger skonczone = new AtomicInteger();
        List<Runnable> zadania = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            zadania.add(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                skonczone.incrementAndGet();
            });
        }
        long start = System.nanoTime();
        uruchamiacz.uruchomRownolegle(zadania);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(3, skonczone.get(), "Metoda wróciła, zanim zadania się skończyły (skończone: " + skonczone.get() + " z 3)");
        assertTrue(ms < 800, "3 zadania po 300 ms równolegle powinny zająć ok. 300 ms, zajęły " + ms + " ms");
    }
}
