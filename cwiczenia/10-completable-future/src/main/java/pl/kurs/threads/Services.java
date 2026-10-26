package pl.kurs.threads;

import java.math.BigDecimal;
import java.util.List;

/**
 * Three external services. Every call is blocking and takes time (about 200 ms in the tests).
 */
public final class Services {

    private Services() {
    }

    public interface Customers {
        /** @throws CustomerNotFoundException when there is no such customer */
        Customer get(long customerId);
    }

    public interface Orders {
        List<Order> fetchOpen(long customerId);
    }

    public interface Discounts {
        /** Discount as a fraction, e.g. 0.10 means 10%. The service is sometimes slow and sometimes unavailable (throws). */
        BigDecimal discount(long customerId);
    }

    public record Customer(long id, String name) {
    }

    public record Order(String number, BigDecimal value) {
    }

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(long id) {
            super("No customer " + id);
        }
    }
}
