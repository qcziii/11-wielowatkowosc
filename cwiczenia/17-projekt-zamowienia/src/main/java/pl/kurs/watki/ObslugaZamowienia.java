package pl.kurs.watki;

/** Obsługa jednego zamówienia (zapis, wysyłka maila...). Trwa i czasem rzuca wyjątek. */
@FunctionalInterface
public interface ObslugaZamowienia {

    void obsluz(Zamowienie zamowienie) throws Exception;
}
