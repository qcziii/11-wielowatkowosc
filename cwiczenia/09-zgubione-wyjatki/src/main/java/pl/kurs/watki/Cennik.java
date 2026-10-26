package pl.kurs.watki;

import java.math.BigDecimal;

/** Miejsce zapisu cen (w prawdziwej aplikacji: baza danych, zapis trwa). */
public interface Cennik {

    void zapisz(String produkt, BigDecimal cena);
}
