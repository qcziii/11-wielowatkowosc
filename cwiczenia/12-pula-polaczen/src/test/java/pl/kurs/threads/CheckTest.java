package pl.kurs.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    /**
     * Runs the operation in a separate thread and waits up to 2 s for it to finish.
     * Returns the result or the exception thrown by execute().
     */
    private static Object withinTimeout(ConnectionPool pool, java.util.function.Function<Connection, ?> op) throws Exception {
        AtomicReference<Object> result = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                result.set(pool.execute(op));
            } catch (Throwable e) {
                result.set(e);
            }
        });
        t.setDaemon(true);
        t.start();
        t.join(2000);
        if (t.isAlive()) {
            t.interrupt();
            fail("Operacja czeka na połączenie ponad 2 s, choć nikt nie używa puli (wolne połączenia: "
                    + pool.freeConnections() + " z " + pool.maxConnections() + ")");
        }
        return result.get();
    }

    @Test
    @DisplayName("1) Naraz co najwyżej 3 operacje (pula 3, 12 wątków)")
    void limit() throws Exception {
        ConnectionPool pool = new ConnectionPool(3);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Thread t = new Thread(() -> {
                try {
                    pool.execute(p -> {
                        max.accumulateAndGet(active.incrementAndGet(), Math::max);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        active.decrementAndGet();
                        return null;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(3, max.get(), "Naraz działało " + max.get() + " operacji");
        assertEquals(3, pool.freeConnections());
    }

    @Test
    @DisplayName("2) Po 5 operacjach z wyjątkiem pula dalej działa i ma 3 wolne połączenia")
    void afterErrors() throws Exception {
        ConnectionPool pool = new ConnectionPool(3);
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            Object result = withinTimeout(pool, p -> {
                throw new IllegalStateException("błąd SQL " + idx);
            });
            assertInstanceOf(IllegalStateException.class, result, "Wyjątek z operacji ma lecieć do wywołującego");
        }
        assertEquals("ok", withinTimeout(pool, p -> "ok"));
        assertEquals(3, pool.freeConnections(), "Pula po błędach ma złą liczbę wolnych połączeń");
    }

    @Test
    @DisplayName("3) Wątek przerwany w czasie czekania na połączenie nie psuje licznika wolnych połączeń")
    void interruptedWhileWaiting() throws Exception {
        ConnectionPool pool = new ConnectionPool(1);
        CountDownLatch taken = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            try {
                pool.execute(p -> {
                    taken.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        holder.start();
        assertTrue(taken.await(2, TimeUnit.SECONDS));

        AtomicReference<Throwable> waiterResult = new AtomicReference<>();
        Thread waiter = new Thread(() -> {
            try {
                pool.execute(p -> "nie powinno się wykonać");
            } catch (Throwable e) {
                waiterResult.set(e);
            }
        });
        waiter.start();
        while (waiter.getState() != Thread.State.WAITING) {
            Thread.sleep(5);
        }
        waiter.interrupt();
        waiter.join(2000);
        assertInstanceOf(InterruptedException.class, waiterResult.get(), "Przerwany wątek ma dostać InterruptedException");

        release.countDown();
        holder.join(2000);
        assertEquals(1, pool.freeConnections(), "Pula na 1 połączenie ma teraz " + pool.freeConnections() + " wolnych");
    }
}
