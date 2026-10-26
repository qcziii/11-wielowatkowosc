package pl.kurs.watki;

import java.math.BigDecimal;

public record Zamowienie(long id, String kategoria, BigDecimal wartosc) {
}
