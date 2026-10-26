package pl.kurs.watki;

import java.util.List;
import java.util.function.Function;

/**
 * Przetwarza elementy listy równolegle, na puli wątków o podanym rozmiarze.
 */
public class PrzetwarzanieRownolegle {

    /**
     * Wywołuje {@code funkcja} dla każdego elementu listy, równolegle, w co najwyżej {@code liczbaWatkow} wątkach naraz.
     * <ul>
     *     <li>Wyniki są w tej samej kolejności co elementy wejściowe.</li>
     *     <li>Gdy funkcja rzuci wyjątek dla któregokolwiek elementu, metoda rzuca {@link PrzetwarzanieException},
     *         którego przyczyną ({@code getCause()}) jest ten oryginalny wyjątek.</li>
     *     <li>Po powrocie z metody (normalnym albo z wyjątkiem) nie zostaje żaden działający wątek puli.</li>
     * </ul>
     */
    public <T, R> List<R> przetworz(List<T> dane, Function<T, R> funkcja, int liczbaWatkow) {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj przetworz");
    }
}
