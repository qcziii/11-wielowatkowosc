package pl.kurs.watki;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Ustawienia aplikacji. Czyta je naraz wiele wątków (każde żądanie HTTP), a od czasu do czasu
 * jeden wątek przeładowuje całość z pliku.
 * <ul>
 *     <li>Czytelnicy nie blokują się nawzajem: wiele wątków może czytać w tym samym czasie.</li>
 *     <li>Przeładowanie jest wyłączne: w trakcie {@link #przeladuj(Map)} nikt nie czyta,
 *         więc nikt nie zobaczy w połowie starej, a w połowie nowej konfiguracji.</li>
 *     <li>Wyjątek w trakcie czytania nie może zablokować konfiguracji.</li>
 * </ul>
 * Zaimplementuj na {@link java.util.concurrent.locks.ReentrantReadWriteLock}.
 */
public class Konfiguracja {

    private final Map<String, String> ustawienia = new HashMap<>();

    public Konfiguracja(Map<String, String> poczatkowe) {
        ustawienia.putAll(poczatkowe);
    }

    /** Wartość jednego ustawienia albo null. */
    public String pobierz(String klucz) {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj pobierz");
    }

    /**
     * Wykonuje funkcję na spójnym widoku wszystkich ustawień (np. żeby odczytać kilka powiązanych wartości naraz).
     * Funkcja nie może zmieniać mapy.
     */
    public <T> T czytaj(Function<Map<String, String>, T> funkcja) {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj czytaj");
    }

    /** Zastępuje wszystkie ustawienia nowymi. */
    public void przeladuj(Map<String, String> nowe) {
        // TODO: wyczyść ustawienia i włóż nowe, tak żeby nikt nie zobaczył stanu pośredniego
        throw new UnsupportedOperationException("TODO: zaimplementuj przeladuj");
    }
}
