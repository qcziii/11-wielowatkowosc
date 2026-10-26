package pl.kurs.threads;

import java.math.BigDecimal;

public record Order(long id, String category, BigDecimal value) {
}
