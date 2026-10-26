package pl.kurs.threads;

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
 * Fetches prices of many products at once. Almost all the time is spent waiting for the HTTP response.
 */
public class PriceFetcher {

    private final PriceSource source;

    public PriceFetcher(PriceSource source) {
        this.source = source;
    }

    /** Prices of all products, in list order. */
    public Map<String, BigDecimal> fetchAll(List<String> products) throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<BigDecimal>> results = new ArrayList<>();
            for (String product : products) {
                results.add(pool.submit(() -> source.price(product)));
            }
            Map<String, BigDecimal> prices = new LinkedHashMap<>();
            for (int i = 0; i < products.size(); i++) {
                prices.put(products.get(i), results.get(i).get());
            }
            return prices;
        } finally {
            pool.shutdown();
        }
    }
}
