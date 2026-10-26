package pl.kurs.threads;

import java.util.List;
import java.util.function.Function;

/**
 * Processes list elements in parallel, on a thread pool of the given size.
 */
public class ParallelProcessor {

    /**
     * Calls {@code function} for every list element, in parallel, in at most {@code threadCount} threads at a time.
     * <ul>
     *     <li>The results are in the same order as the input elements.</li>
     *     <li>When the function throws for any element, the method throws {@link ProcessingException}
     *         whose cause ({@code getCause()}) is that original exception.</li>
     *     <li>After the method returns (normally or with an exception) no pool thread is left running.</li>
     * </ul>
     */
    public <T, R> List<R> process(List<T> data, Function<T, R> function, int threadCount) {
        // TODO
        throw new UnsupportedOperationException("TODO: implement process");
    }
}
