package pl.kurs.watki;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Podsumowanie otwartych zamówień klienta. Tę klasę możesz zostawić bez zmian: obliczenia są gotowe.
 */
public record Podsumowanie(String klient, int liczbaZamowien, BigDecimal suma, BigDecimal rabat, BigDecimal doZaplaty) {

    public static Podsumowanie z(Uslugi.Klient klient, List<Uslugi.Zamowienie> zamowienia, BigDecimal rabat) {
        BigDecimal suma = zamowienia.stream().map(Uslugi.Zamowienie::wartosc).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal doZaplaty = suma.multiply(BigDecimal.ONE.subtract(rabat)).setScale(2, RoundingMode.HALF_UP);
        return new Podsumowanie(klient.nazwa(), zamowienia.size(), suma, rabat, doZaplaty);
    }
}
