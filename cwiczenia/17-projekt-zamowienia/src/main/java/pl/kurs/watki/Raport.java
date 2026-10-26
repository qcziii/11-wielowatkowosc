package pl.kurs.watki;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @param przetworzone     liczba zamówień obsłużonych bez błędu
 * @param bledy            liczba zamówień, dla których obsługa rzuciła wyjątek
 * @param sumyWgKategorii  suma wartości obsłużonych bez błędu zamówień, dla każdej kategorii
 */
public record Raport(int przetworzone, int bledy, Map<String, BigDecimal> sumyWgKategorii) {
}
