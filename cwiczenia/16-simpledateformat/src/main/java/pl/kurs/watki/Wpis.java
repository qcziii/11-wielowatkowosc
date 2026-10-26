package pl.kurs.watki;

import java.time.LocalDateTime;

public record Wpis(LocalDateTime czas, String poziom, String tresc) {
}
