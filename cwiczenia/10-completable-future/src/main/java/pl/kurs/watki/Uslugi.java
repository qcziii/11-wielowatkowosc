package pl.kurs.watki;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trzy zewnętrzne usługi. Każde wywołanie jest blokujące i trwa (w testach ok. 200 ms).
 */
public final class Uslugi {

    private Uslugi() {
    }

    public interface Klienci {
        /** @throws KlientNieZnalezionyException gdy klienta nie ma */
        Klient pobierz(long klientId);
    }

    public interface Zamowienia {
        List<Zamowienie> pobierzOtwarte(long klientId);
    }

    public interface Rabaty {
        /** Rabat jako ułamek, np. 0.10 to 10%. Usługa bywa wolna i bywa niedostępna (rzuca wyjątek). */
        BigDecimal rabat(long klientId);
    }

    public record Klient(long id, String nazwa) {
    }

    public record Zamowienie(String numer, BigDecimal wartosc) {
    }

    public static class KlientNieZnalezionyException extends RuntimeException {
        public KlientNieZnalezionyException(long id) {
            super("Nie ma klienta " + id);
        }
    }
}
