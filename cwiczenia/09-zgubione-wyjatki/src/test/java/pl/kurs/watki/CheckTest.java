package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    /** Cennik w pamięci, zapis trwa podaną liczbę milisekund. */
    static class WolnyCennik implements Cennik {
        final Map<String, BigDecimal> ceny = new ConcurrentHashMap<>();
        final long ms;

        WolnyCennik(long ms) {
            this.ms = ms;
        }

        @Override
        public void zapisz(String produkt, BigDecimal cena) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("zapis przerwany", e);
            }
            ceny.put(produkt, cena);
        }
    }

    private static List<String> poprawne(int n) {
        List<String> w = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            w.add("produkt-" + i + ";" + i + ".99");
        }
        return w;
    }

    @Test
    @DisplayName("1) Same poprawne wiersze, szybki zapis: wszystko zaimportowane")
    void szybki() throws Exception {
        WolnyCennik c = new WolnyCennik(0);
        WynikImportu w = new ImportCennika(c).importuj(poprawne(10));
        assertEquals(10, w.zaimportowane());
        assertEquals(List.of(), w.bledy());
        assertEquals(new BigDecimal("7.99"), c.ceny.get("produkt-7"));
    }

    @Test
    @DisplayName("2) Wolny zapis (60 wierszy po 30 ms): import kończy się dopiero po zapisaniu wszystkich")
    void wolny() throws Exception {
        WolnyCennik c = new WolnyCennik(30);
        WynikImportu w = new ImportCennika(c).importuj(poprawne(60));
        assertEquals(60, w.zaimportowane(), "Import wrócił przed zapisaniem wszystkich wierszy");
        assertEquals(60, c.ceny.size());
    }

    @Test
    @DisplayName("3) Błędne wiersze są zgłaszane w wyniku, w kolejności wierszy, poprawne zaimportowane")
    void bledy() throws Exception {
        WolnyCennik c = new WolnyCennik(5);
        List<String> wiersze = new ArrayList<>(poprawne(12));
        wiersze.set(2, "mysz;12,50");      // wiersz 3: przecinek
        wiersze.set(6, "klawiatura");      // wiersz 7: brak ceny
        wiersze.set(10, "monitor;-5");     // wiersz 11: ujemna cena
        WynikImportu w = new ImportCennika(c).importuj(wiersze);
        assertEquals(9, w.zaimportowane());
        assertEquals(3, w.bledy().size(), "Oczekiwano 3 błędów, jest: " + w.bledy());
        assertTrue(w.bledy().get(0).startsWith("wiersz 3: "), w.bledy().toString());
        assertTrue(w.bledy().get(1).startsWith("wiersz 7: "), w.bledy().toString());
        assertTrue(w.bledy().get(2).startsWith("wiersz 11: "), w.bledy().toString());
        assertTrue(w.bledy().get(2).contains("ujemna cena"), "Komunikat ma zawierać opis z wyjątku: " + w.bledy().get(2));
    }

    @Test
    @DisplayName("4) Dużo błędów naraz (2000 wierszy, co drugi zły): wszystkie zgłoszone, w kolejności wierszy")
    void duzoBledow() throws Exception {
        WolnyCennik c = new WolnyCennik(0);
        List<String> wiersze = new ArrayList<>();
        for (int i = 1; i <= 2000; i++) {
            wiersze.add(i % 2 == 0 ? "zly-" + i + ";abc" : "produkt-" + i + ";" + i);
        }
        WynikImportu w = new ImportCennika(c).importuj(wiersze);
        assertEquals(1000, w.zaimportowane());
        assertEquals(1000, w.bledy().size(), "Liczba zgłoszonych błędów");
        for (int i = 0; i < 1000; i++) {
            String oczekiwanyPoczatek = "wiersz " + (2 * i + 2) + ": ";
            String blad = w.bledy().get(i);
            assertTrue(blad != null && blad.startsWith(oczekiwanyPoczatek),
                    "Błąd nr " + (i + 1) + " ma się zaczynać od '" + oczekiwanyPoczatek + "', jest: " + blad);
        }
    }
}
