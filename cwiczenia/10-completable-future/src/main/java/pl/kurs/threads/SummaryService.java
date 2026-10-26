package pl.kurs.threads;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Builds a customer summary from three services. All service calls run on the given executor.
 */
public class SummaryService {

    /** After this time we stop waiting for the discount service and calculate without a discount. */
    public static final Duration DISCOUNT_TIMEOUT = Duration.ofMillis(500);

    private final Services.Customers customers;
    private final Services.Orders orders;
    private final Services.Discounts discounts;
    private final Executor executor;

    public SummaryService(Services.Customers customers, Services.Orders orders, Services.Discounts discounts, Executor executor) {
        this.customers = customers;
        this.orders = orders;
        this.discounts = discounts;
        this.executor = executor;
    }

    /**
     * Returns immediately (does not block), the result arrives in the CompletableFuture.
     * <ul>
     *     <li>The three services are called in parallel, on {@code executor}.</li>
     *     <li>When the discount service throws or does not answer within {@link #DISCOUNT_TIMEOUT}, the discount is 0.</li>
     *     <li>When there is no customer, the future completes exceptionally with {@link Services.CustomerNotFoundException} as the cause.</li>
     * </ul>
     */
    public CompletableFuture<Summary> summary(long customerId) {
        // TODO
        throw new UnsupportedOperationException("TODO: implement summary");
    }
}
