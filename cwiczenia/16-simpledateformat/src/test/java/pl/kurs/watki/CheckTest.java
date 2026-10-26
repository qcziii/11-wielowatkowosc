package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private static final DateTimeFormatter WZOR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("1) Jedna linia: poprawny wpis")
    void jednaLinia() {
        List<Wpis> w = new ParserLogow().parsuj(List.of("2026-03-14 09:26:53 WARN dysk zapełniony w 91%"));
        assertEquals(List.of(new Wpis(LocalDateTime.of(2026, 3, 14, 9, 26, 53), "WARN", "dysk zapełniony w 91%")), w);
    }

    @Test
    @DisplayName("2) 200 000 linii parsowanych równolegle: każda data poprawna, żadnego wyjątku")
    void rownolegle() {
        LocalDateTime poczatek = LocalDateTime.of(2026, 4, 1, 0, 0, 0); // bez zmiany czasu letni/zimowy w zakresie
        List<String> linie = new ArrayList<>();
        List<LocalDateTime> oczekiwane = new ArrayList<>();
        for (int i = 0; i < 200_000; i++) {
            LocalDateTime t = poczatek.plusSeconds(i * 47L);
            oczekiwane.add(t);
            linie.add(WZOR.format(t) + " INFO zdarzenie " + i);
        }
        List<Wpis> wpisy;
        try {
            wpisy = new ParserLogow().parsuj(linie);
        } catch (RuntimeException e) {
            fail("Parsowanie rzuciło wyjątek: " + e + (e.getCause() != null ? ", przyczyna: " + e.getCause() : ""));
            return;
        }
        int zle = 0;
        String przyklad = null;
        for (int i = 0; i < linie.size(); i++) {
            if (!wpisy.get(i).czas().equals(oczekiwane.get(i))) {
                if (zle++ == 0) {
                    przyklad = "linia '" + linie.get(i) + "' dała " + wpisy.get(i).czas();
                }
            }
        }
        assertEquals(0, zle, "Złe daty w " + zle + " liniach, np. " + przyklad);
    }
}
