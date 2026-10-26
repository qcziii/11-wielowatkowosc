package pl.kurs.watki;

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

    private final SumaRownolegla suma = new SumaRownolegla();

    private static long[] tablica(int n) {
        return LongStream.rangeClosed(1, n).toArray();
    }

    private static long oczekiwana(int n) {
        return (long) n * (n + 1) / 2;
    }

    @Test
    @DisplayName("1) Duża tablica podzielna przez liczbę wątków: suma się zgadza (20 powtórzeń)")
    void duzaTablica() {
        long[] t = tablica(8_000_000);
        for (int i = 0; i < 20; i++) {
            assertEquals(oczekiwana(8_000_000), suma.suma(t, 4), "Zła suma w powtórzeniu " + (i + 1));
        }
    }

    @Test
    @DisplayName("2) Długość niepodzielna przez liczbę wątków (1 000 003 elementy, 4 wątki)")
    void resztaZDzielenia() {
        assertEquals(oczekiwana(1_000_003), suma.suma(tablica(1_000_003), 4));
    }

    @Test
    @DisplayName("3) Więcej wątków niż elementów (3 elementy, 8 wątków) i jeden wątek")
    void malaTablica() {
        assertEquals(6, suma.suma(tablica(3), 8));
        assertEquals(oczekiwana(1000), suma.suma(tablica(1000), 1));
    }
}
