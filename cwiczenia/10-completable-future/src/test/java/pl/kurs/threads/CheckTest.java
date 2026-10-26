package pl.kurs.threads;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final AtomicInteger idx = new AtomicInteger();
    private final ExecutorService executor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "shop-" + idx.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    /** Names of the threads the services were called on. */
    private final Set<String> serviceThreads = ConcurrentHashMap.newKeySet();

    @AfterEach
    void close() {
        executor.shutdownNow();
    }

    private void work(long ms) {
        serviceThreads.add(Thread.currentThread().getName());
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }

    private final Services.Customers customers = id -> {
        work(200);
        if (id == 404) {
            throw new Services.CustomerNotFoundException(id);
        }
        return new Services.Customer(id, "Klient " + id);
    };

    private final Services.Orders orders = id -> {
        work(200);
        return List.of(new Services.Order("Z1", new BigDecimal("100.00")), new Services.Order("Z2", new BigDecimal("50.00")));
    };

    private Services.Discounts discount(long ms, BigDecimal value, boolean error) {
        return id -> {
            work(ms);
            if (error) {
                throw new IllegalStateException("usługa rabatów niedostępna");
            }
            return value;
        };
    }

    private Summary get(SummaryService s, long id) throws Exception {
        return s.summary(id).get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("1) Wynik poprawny, trzy usługi wołane równolegle (ok. 200 ms, nie 600 ms)")
    void parallel() throws Exception {
        SummaryService s = new SummaryService(customers, orders, discount(200, new BigDecimal("0.10"), false), executor);
        long start = System.nanoTime();
        Summary p = get(s, 7);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(new Summary("Klient 7", 2, new BigDecimal("150.00"), new BigDecimal("0.10"), new BigDecimal("135.00")), p);
        assertTrue(ms < 450, "Usługi po 200 ms wołane równolegle to ok. 200 ms, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("2) summary() wraca od razu, nie czeka na usługi")
    void doesNotBlock() {
        SummaryService s = new SummaryService(customers, orders, discount(200, BigDecimal.ZERO, false), executor);
        long start = System.nanoTime();
        CompletableFuture<Summary> f = s.summary(7);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < 100, "summary() blokowało przez " + ms + " ms");
        assertFalse(f.isDone(), "Wynik gotowy od razu? Usługi trwają 200 ms");
    }

    @Test
    @DisplayName("3) Usługi wołane na podanym executorze (wątki shop-N), nie na ForkJoinPool.commonPool")
    void executor() throws Exception {
        SummaryService s = new SummaryService(customers, orders, discount(200, BigDecimal.ZERO, false), executor);
        get(s, 7);
        assertFalse(serviceThreads.isEmpty());
        assertTrue(serviceThreads.stream().allMatch(n -> n.startsWith("shop-")), "Usługi wołane w wątkach: " + serviceThreads);
    }

    @Test
    @DisplayName("4) Usługa rabatów rzuca wyjątek: rabat 0, wynik dalej jest")
    void discountFails() throws Exception {
        SummaryService s = new SummaryService(customers, orders, discount(50, null, true), executor);
        Summary p = get(s, 7);
        assertEquals(0, p.discount().signum(), "Rabat ma być 0, jest " + p.discount());
        assertEquals(new BigDecimal("150.00"), p.amountDue());
    }

    @Test
    @DisplayName("5) Usługa rabatów odpowiada po 3 s: po 500 ms liczymy bez rabatu")
    void discountSlow() throws Exception {
        SummaryService s = new SummaryService(customers, orders, discount(3000, new BigDecimal("0.50"), false), executor);
        long start = System.nanoTime();
        Summary p = get(s, 7);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(0, p.discount().signum(), "Rabat ma być 0, jest " + p.discount());
        assertTrue(ms < 900, "Po 500 ms bez odpowiedzi rabatu wynik ma być od razu, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("6) Brak klienta: future kończy się wyjątkiem, przyczyna CustomerNotFoundException")
    void missingCustomer() {
        SummaryService s = new SummaryService(customers, orders, discount(200, BigDecimal.ZERO, false), executor);
        ExecutionException e = assertThrows(ExecutionException.class, () -> s.summary(404).get(5, TimeUnit.SECONDS));
        assertInstanceOf(Services.CustomerNotFoundException.class, e.getCause(), "Przyczyna: " + e.getCause());
    }
}
