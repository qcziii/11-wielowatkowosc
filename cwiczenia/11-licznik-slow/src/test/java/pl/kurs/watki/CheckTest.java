package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final LicznikSlow licznik = new LicznikSlow();

    @Test
    @DisplayName("1) Jeden wątek: liczy poprawnie, bez względu na wielkość liter")
    void jedenWatek() throws Exception {
        Map<String, Long> m = licznik.policz(List.of("Ala ma kota.", "Kot ma Alę, ala ma psa"), 1);
        assertEquals(3L, m.get("ma"));
        assertEquals(2L, m.get("ala"));
        assertEquals(1L, m.get("kota"));
        assertEquals(1L, m.get("kot"));
    }

    @Test
    @DisplayName("2) 8 wątków, 200 000 linii z kilkoma powtarzającymi się słowami: liczby dokładne")
    void wieleWatkow() throws Exception {
        List<String> linie = new ArrayList<>();
        for (int i = 0; i < 200_000; i++) {
            linie.add(i % 2 == 0 ? "tak nie tak" : "nie, może TAK");
        }
        Map<String, Long> m = licznik.policz(linie, 8);
        assertEquals(300_000L, m.get("tak"), "Słowo 'tak'");
        assertEquals(200_000L, m.get("nie"), "Słowo 'nie'");
        assertEquals(100_000L, m.get("może"), "Słowo 'może'");
        assertEquals(3, m.size());
    }
}
