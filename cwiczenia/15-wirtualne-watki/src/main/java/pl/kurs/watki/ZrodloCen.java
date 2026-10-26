package pl.kurs.watki;

import java.math.BigDecimal;

/** Zewnętrzne API z cenami. Jedno wywołanie to jedno żądanie HTTP (blokujące, ok. 100 ms). */
public interface ZrodloCen {

    BigDecimal cena(String produkt);
}
