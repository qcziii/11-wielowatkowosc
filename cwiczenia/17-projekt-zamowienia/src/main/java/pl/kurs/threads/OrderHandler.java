package pl.kurs.threads;

/** Handling of one order (saving, sending an e-mail...). Takes time and sometimes throws. */
@FunctionalInterface
public interface OrderHandler {

    void handle(Order order) throws Exception;
}
