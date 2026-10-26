package pl.kurs.threads;

import java.time.LocalDateTime;

public record LogEntry(LocalDateTime time, String level, String message) {
}
