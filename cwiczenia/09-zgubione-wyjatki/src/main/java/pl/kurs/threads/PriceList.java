package pl.kurs.threads;

import java.math.BigDecimal;

/** Where prices are stored (in a real application: a database, saving takes time). */
public interface PriceList {

    void save(String product, BigDecimal price);
}
