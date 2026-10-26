package pl.kurs.threads;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Imports a price list from a CSV file (rows "product;price"). Rows are processed in parallel,
 * because saving to the price list is slow.
 */
public class PriceListImport {

    private final PriceList priceList;

    public PriceListImport(PriceList priceList) {
        this.priceList = priceList;
    }

    public ImportResult importAll(List<String> rows) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        AtomicInteger imported = new AtomicInteger();
        List<String> errors = new ArrayList<>();

        for (String row : rows) {
            pool.submit(() -> {
                String[] fields = row.split(";");
                BigDecimal price = new BigDecimal(fields[1].trim());
                if (price.signum() < 0) {
                    throw new IllegalArgumentException("negative price " + price);
                }
                priceList.save(fields[0].trim(), price);
                imported.incrementAndGet();
            });
        }

        pool.shutdown();
        pool.awaitTermination(200, TimeUnit.MILLISECONDS);
        return new ImportResult(imported.get(), errors);
    }
}
