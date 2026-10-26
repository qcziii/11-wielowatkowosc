package pl.kurs.watki;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Ogranicza liczbę naraz używanych połączeń z bazą do {@code maksimum}.
 * Wątek, który chce wykonać operację, gdy wszystkie połączenia są zajęte, czeka na wolne.
 */
public class PulaPolaczen {

    private final Semaphore wolne;
    private final int maksimum;
    private final AtomicInteger numery = new AtomicInteger();

    public PulaPolaczen(int maksimum) {
        this.maksimum = maksimum;
        this.wolne = new Semaphore(maksimum);
    }

    /**
     * Czeka na wolne połączenie, wykonuje na nim operację i oddaje połączenie.
     * Wyjątek z operacji leci dalej do wywołującego.
     *
     * @throws InterruptedException gdy wątek zostanie przerwany w czasie czekania na połączenie
     */
    public <T> T wykonaj(Function<Polaczenie, T> operacja) throws InterruptedException {
        wolne.acquire();
        Polaczenie polaczenie = new Polaczenie(numery.incrementAndGet());
        T wynik = operacja.apply(polaczenie);
        wolne.release();
        return wynik;
    }

    /** Liczba wolnych połączeń w tej chwili. */
    public int wolnePolaczenia() {
        return wolne.availablePermits();
    }

    public int maksimum() {
        return maksimum;
    }
}
