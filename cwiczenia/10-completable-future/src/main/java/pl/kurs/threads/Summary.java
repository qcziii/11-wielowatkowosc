package pl.kurs.threads;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Summary of a customer's open orders. You can leave this class unchanged: the calculations are done.
 */
public record Summary(String customer, int orderCount, BigDecimal sum, BigDecimal discount, BigDecimal amountDue) {

    public static Summary of(Services.Customer customer, List<Services.Order> orders, BigDecimal discount) {
        BigDecimal sum = orders.stream().map(Services.Order::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amountDue = sum.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP);
        return new Summary(customer.name(), orders.size(), sum, discount, amountDue);
    }
}
