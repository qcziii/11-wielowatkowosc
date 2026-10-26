package pl.kurs.threads;

import java.util.Iterator;

/**
 * Processes a stream of orders with the producer-consumer pattern.
 * <ul>
 *     <li>One producer thread reads orders from the source and puts them into a queue of capacity {@code queueCapacity}.
 *         When the queue is full, the producer waits (it does not read the source ahead).</li>
 *     <li>{@code consumerCount} consumer threads take orders from the queue and call {@link OrderHandler#handle}.</li>
 *     <li>An exception from the handler is a failure of that one order: it is counted in the report, the consumer keeps working.</li>
 *     <li>{@link #process} returns when all orders from the source have been handled and all producer
 *         and consumer threads have finished.</li>
 * </ul>
 */
public class OrderProcessor {

    private final int consumerCount;
    private final int queueCapacity;
    private final OrderHandler handler;

    public OrderProcessor(int consumerCount, int queueCapacity, OrderHandler handler) {
        this.consumerCount = consumerCount;
        this.queueCapacity = queueCapacity;
        this.handler = handler;
    }

    /**
     * Processes all orders from the source. The source (iterator) is not safe for multiple threads:
     * only the producer thread reads it.
     */
    public Report process(Iterator<Order> source) throws InterruptedException {
        // TODO
        throw new UnsupportedOperationException("TODO: implement process");
    }
}
