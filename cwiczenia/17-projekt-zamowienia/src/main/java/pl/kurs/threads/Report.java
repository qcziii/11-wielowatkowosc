package pl.kurs.threads;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @param processed      number of orders handled without an error
 * @param errors         number of orders for which the handler threw an exception
 * @param sumsByCategory sum of values of orders handled without an error, for each category
 */
public record Report(int processed, int errors, Map<String, BigDecimal> sumsByCategory) {
}
