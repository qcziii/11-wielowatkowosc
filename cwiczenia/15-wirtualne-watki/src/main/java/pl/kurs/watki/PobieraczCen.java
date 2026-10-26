package pl.kurs.watki;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Pobiera ceny wielu produktów naraz. Prawie cały czas to czekanie na odpowiedź HTTP.
 */
public class PobieraczCen {

    private final ZrodloCen zrodlo;

    public PobieraczCen(ZrodloCen zrodlo) {
        this.zrodlo = zrodlo;
    }

    /** Ceny wszystkich produktów, w kolejności listy. */
    public Map<String, BigDecimal> pobierzWszystkie(List<String> produkty) throws InterruptedException, ExecutionException {
        ExecutorService pula = Executors.newFixedThreadPool(8);
        try {
            List<Future<BigDecimal>> wyniki = new ArrayList<>();
            for (String produkt : produkty) {
                wyniki.add(pula.submit(() -> zrodlo.cena(produkt)));
            }
            Map<String, BigDecimal> ceny = new LinkedHashMap<>();
            for (int i = 0; i < produkty.size(); i++) {
                ceny.put(produkty.get(i), wyniki.get(i).get());
            }
            return ceny;
        } finally {
            pula.shutdown();
        }
    }
}
