package pl.kurs.threads;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fixed-capacity buffer for many producers and many consumers.
 * <ul>
 *     <li>{@link #put(Object)} waits while the buffer is full,</li>
 *     <li>{@link #take()} waits while the buffer is empty,</li>
 *     <li>elements come out in insertion order (FIFO).</li>
 * </ul>
 * Implement it with synchronized, wait() and notifyAll(), without classes from java.util.concurrent.
 */
public class BoundedBuffer<T> {

    private final Deque<T> elements = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
    }

    /** Adds an element at the end. While the buffer is full, waits until there is room. */
    public void put(T element) throws InterruptedException {
        // TODO
        throw new UnsupportedOperationException("TODO: implement put");
    }

    /** Removes an element from the front. While the buffer is empty, waits until someone puts something. */
    public T take() throws InterruptedException {
        // TODO
        throw new UnsupportedOperationException("TODO: implement take");
    }

    /** Current number of elements in the buffer. */
    public int size() {
        // TODO
        throw new UnsupportedOperationException("TODO: implement size");
    }

    public int capacity() {
        return capacity;
    }
}
