package pl.kurs.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final ParallelSum sum = new ParallelSum();

    private static long[] array(int n) {
        return LongStream.rangeClosed(1, n).toArray();
    }

    private static long expected(int n) {
        return (long) n * (n + 1) / 2;
    }

    @Test
    @DisplayName("1) Duża tablica podzielna przez liczbę wątków: suma się zgadza (20 powtórzeń)")
    void largeArray() {
        long[] t = array(8_000_000);
        for (int i = 0; i < 20; i++) {
            assertEquals(expected(8_000_000), sum.sum(t, 4), "Zła suma w powtórzeniu " + (i + 1));
        }
    }

    @Test
    @DisplayName("2) Długość niepodzielna przez liczbę wątków (1 000 003 elementy, 4 wątki)")
    void remainder() {
        assertEquals(expected(1_000_003), sum.sum(array(1_000_003), 4));
    }

    @Test
    @DisplayName("3) Więcej wątków niż elementów (3 elementy, 8 wątków) i jeden wątek")
    void smallArray() {
        assertEquals(6, sum.sum(array(3), 8));
        assertEquals(expected(1000), sum.sum(array(1000), 1));
    }
}
