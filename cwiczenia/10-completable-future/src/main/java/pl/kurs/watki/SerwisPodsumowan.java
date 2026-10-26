package pl.kurs.watki;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Składa podsumowanie klienta z trzech usług. Wszystkie wywołania usług idą na podanym executorze.
 */
public class SerwisPodsumowan {

    /** Po tym czasie przestajemy czekać na usługę rabatów i liczymy bez rabatu. */
    public static final Duration LIMIT_RABATU = Duration.ofMillis(500);

    private final Uslugi.Klienci klienci;
    private final Uslugi.Zamowienia zamowienia;
    private final Uslugi.Rabaty rabaty;
    private final Executor executor;

    public SerwisPodsumowan(Uslugi.Klienci klienci, Uslugi.Zamowienia zamowienia, Uslugi.Rabaty rabaty, Executor executor) {
        this.klienci = klienci;
        this.zamowienia = zamowienia;
        this.rabaty = rabaty;
        this.executor = executor;
    }

    /**
     * Zwraca od razu (nie blokuje), wynik przychodzi w CompletableFuture.
     * <ul>
     *     <li>Trzy usługi są wołane równolegle, na {@code executor}.</li>
     *     <li>Gdy usługa rabatów rzuci wyjątek albo nie odpowie w {@link #LIMIT_RABATU}, rabat to 0.</li>
     *     <li>Gdy nie ma klienta, future kończy się wyjątkowo, a przyczyną jest {@link Uslugi.KlientNieZnalezionyException}.</li>
     * </ul>
     */
    public CompletableFuture<Podsumowanie> podsumowanie(long klientId) {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj podsumowanie");
    }
}
