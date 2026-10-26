package pl.kurs.threads;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Limits the number of database connections used at the same time to {@code maxConnections}.
 * A thread that wants to run an operation while all connections are busy waits for a free one.
 */
public class ConnectionPool {

    private final Semaphore permits;
    private final int maxConnections;
    private final AtomicInteger connectionIds = new AtomicInteger();

    public ConnectionPool(int maxConnections) {
        this.maxConnections = maxConnections;
        this.permits = new Semaphore(maxConnections);
    }

    /**
     * Waits for a free connection, runs the operation on it and gives the connection back.
     * An exception from the operation propagates to the caller.
     *
     * @throws InterruptedException when the thread is interrupted while waiting for a connection
     */
    public <T> T execute(Function<Connection, T> operation) throws InterruptedException {
        permits.acquire();
        Connection connection = new Connection(connectionIds.incrementAndGet());
        T result = operation.apply(connection);
        permits.release();
        return result;
    }

    /** Number of free connections right now. */
    public int freeConnections() {
        return permits.availablePermits();
    }

    public int maxConnections() {
        return maxConnections;
    }
}
