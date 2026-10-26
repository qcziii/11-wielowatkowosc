package pl.kurs.threads;

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

    /** In-memory PriceList, save takes the given number of milliseconds. */
    static class SlowPriceList implements PriceList {
        final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();
        final long ms;

        SlowPriceList(long ms) {
            this.ms = ms;
        }

        @Override
        public void save(String product, BigDecimal price) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("zapis przerwany", e);
            }
            prices.put(product, price);
        }
    }

    private static List<String> validRows(int n) {
        List<String> w = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            w.add("produkt-" + i + ";" + i + ".99");
        }
        return w;
    }

    @Test
    @DisplayName("1) Same poprawne wiersze, szybki zapis: wszystko zaimportowane")
    void fast() throws Exception {
        SlowPriceList c = new SlowPriceList(0);
        ImportResult w = new PriceListImport(c).importAll(validRows(10));
        assertEquals(10, w.imported());
        assertEquals(List.of(), w.errors());
        assertEquals(new BigDecimal("7.99"), c.prices.get("produkt-7"));
    }

    @Test
    @DisplayName("2) Wolny zapis (60 wierszy po 30 ms): import kończy się dopiero po zapisaniu wszystkich")
    void slow() throws Exception {
        SlowPriceList c = new SlowPriceList(30);
        ImportResult w = new PriceListImport(c).importAll(validRows(60));
        assertEquals(60, w.imported(), "Import wrócił przed zapisaniem wszystkich wierszy");
        assertEquals(60, c.prices.size());
    }

    @Test
    @DisplayName("3) Błędne wiersze są zgłaszane w wyniku, w kolejności wierszy, poprawne zaimportowane")
    void errors() throws Exception {
        SlowPriceList c = new SlowPriceList(5);
        List<String> rows = new ArrayList<>(validRows(12));
        rows.set(2, "mysz;12,50");      // row 3: comma
        rows.set(6, "klawiatura");      // row 7: no price
        rows.set(10, "monitor;-5");     // row 11: negative price
        ImportResult w = new PriceListImport(c).importAll(rows);
        assertEquals(9, w.imported());
        assertEquals(3, w.errors().size(), "Oczekiwano 3 błędów, jest: " + w.errors());
        assertTrue(w.errors().get(0).startsWith("line 3: "), w.errors().toString());
        assertTrue(w.errors().get(1).startsWith("line 7: "), w.errors().toString());
        assertTrue(w.errors().get(2).startsWith("line 11: "), w.errors().toString());
        assertTrue(w.errors().get(2).contains("negative price"), "Komunikat ma zawierać opis z wyjątku: " + w.errors().get(2));
    }

    @Test
    @DisplayName("4) Dużo błędów naraz (2000 wierszy, co drugi zły): wszystkie zgłoszone, w kolejności wierszy")
    void manyErrors() throws Exception {
        SlowPriceList c = new SlowPriceList(0);
        List<String> rows = new ArrayList<>();
        for (int i = 1; i <= 2000; i++) {
            rows.add(i % 2 == 0 ? "zly-" + i + ";abc" : "produkt-" + i + ";" + i);
        }
        ImportResult w = new PriceListImport(c).importAll(rows);
        assertEquals(1000, w.imported());
        assertEquals(1000, w.errors().size(), "Liczba zgłoszonych błędów");
        for (int i = 0; i < 1000; i++) {
            String expectedPrefix = "line " + (2 * i + 2) + ": ";
            String error = w.errors().get(i);
            assertTrue(error != null && error.startsWith(expectedPrefix),
                    "Błąd nr " + (i + 1) + " ma się zaczynać od '" + expectedPrefix + "', jest: " + error);
        }
    }
}
