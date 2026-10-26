package pl.kurs.watki;

/** Błąd przetwarzania jednego z elementów. Przyczyną jest wyjątek rzucony przez funkcję. */
public class PrzetwarzanieException extends RuntimeException {

    public PrzetwarzanieException(String komunikat, Throwable przyczyna) {
        super(komunikat, przyczyna);
    }
}
