package pl.kurs.watki;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bufor o stałej pojemności dla wielu producentów i wielu konsumentów.
 * <ul>
 *     <li>{@link #wloz(Object)} czeka, gdy bufor jest pełny,</li>
 *     <li>{@link #wez()} czeka, gdy bufor jest pusty,</li>
 *     <li>elementy wychodzą w kolejności włożenia (FIFO).</li>
 * </ul>
 * Zaimplementuj na synchronized, wait() i notifyAll(), bez klas z java.util.concurrent.
 */
public class BuforOgraniczony<T> {

    private final Deque<T> elementy = new ArrayDeque<>();
    private final int pojemnosc;

    public BuforOgraniczony(int pojemnosc) {
        if (pojemnosc <= 0) {
            throw new IllegalArgumentException("pojemnosc musi być dodatnia: " + pojemnosc);
        }
        this.pojemnosc = pojemnosc;
    }

    /** Wkłada element na koniec. Gdy bufor jest pełny, czeka, aż zwolni się miejsce. */
    public void wloz(T element) throws InterruptedException {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj wloz");
    }

    /** Wyjmuje element z początku. Gdy bufor jest pusty, czeka, aż ktoś coś włoży. */
    public T wez() throws InterruptedException {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj wez");
    }

    /** Aktualna liczba elementów w buforze. */
    public int rozmiar() {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj rozmiar");
    }

    public int pojemnosc() {
        return pojemnosc;
    }
}
