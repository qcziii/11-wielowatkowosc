package pl.kurs.threads;

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

    private static final DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("1) Jedna linia: poprawny wpis")
    void singleLine() {
        List<LogEntry> w = new LogParser().parseAll(List.of("2026-03-14 09:26:53 WARN dysk zapełniony w 91%"));
        assertEquals(List.of(new LogEntry(LocalDateTime.of(2026, 3, 14, 9, 26, 53), "WARN", "dysk zapełniony w 91%")), w);
    }

    @Test
    @DisplayName("2) 200 000 linii parsowanych równolegle: każda data poprawna, żadnego wyjątku")
    void parallel() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 0, 0, 0); // bez zmiany czasu letni/zimowy w zakresie
        List<String> lines = new ArrayList<>();
        List<LocalDateTime> expected = new ArrayList<>();
        for (int i = 0; i < 200_000; i++) {
            LocalDateTime t = start.plusSeconds(i * 47L);
            expected.add(t);
            lines.add(PATTERN.format(t) + " INFO zdarzenie " + i);
        }
        List<LogEntry> entries;
        try {
            entries = new LogParser().parseAll(lines);
        } catch (RuntimeException e) {
            fail("Parsowanie rzuciło wyjątek: " + e + (e.getCause() != null ? ", przyczyna: " + e.getCause() : ""));
            return;
        }
        int wrong = 0;
        String example = null;
        for (int i = 0; i < lines.size(); i++) {
            if (!entries.get(i).time().equals(expected.get(i))) {
                if (wrong++ == 0) {
                    example = "linia '" + lines.get(i) + "' dała " + entries.get(i).time();
                }
            }
        }
        assertEquals(0, wrong, "Złe daty w " + wrong + " liniach, np. " + example);
    }
}
