package pl.kurs.watki;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Importuje cennik z pliku CSV (wiersze "produkt;cena"). Wiersze są przetwarzane równolegle,
 * bo zapis do cennika jest wolny.
 */
public class ImportCennika {

    private final Cennik cennik;

    public ImportCennika(Cennik cennik) {
        this.cennik = cennik;
    }

    public WynikImportu importuj(List<String> wiersze) throws InterruptedException {
        ExecutorService pula = Executors.newFixedThreadPool(4);
        AtomicInteger zaimportowane = new AtomicInteger();
        List<String> bledy = new ArrayList<>();

        for (String wiersz : wiersze) {
            pula.submit(() -> {
                String[] pola = wiersz.split(";");
                BigDecimal cena = new BigDecimal(pola[1].trim());
                if (cena.signum() < 0) {
                    throw new IllegalArgumentException("ujemna cena " + cena);
                }
                cennik.zapisz(pola[0].trim(), cena);
                zaimportowane.incrementAndGet();
            });
        }

        pula.shutdown();
        pula.awaitTermination(200, TimeUnit.MILLISECONDS);
        return new WynikImportu(zaimportowane.get(), bledy);
    }
}
