package pl.kurs.threads;

import java.math.BigDecimal;

/** External price API. One call is one HTTP request (blocking, about 100 ms). */
public interface PriceSource {

    BigDecimal price(String product);
}
