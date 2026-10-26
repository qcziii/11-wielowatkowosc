package pl.kurs.watki;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Liczy wystąpienia słów w tekście. Linie są dzielone między wątki, wszystkie wątki zapisują do jednej mapy.
 */
public class LicznikSlow {

    public Map<String, Long> policz(List<String> linie, int liczbaWatkow) throws InterruptedException {
        Map<String, Long> wystapienia = new ConcurrentHashMap<>();
        List<Thread> watki = new ArrayList<>();

        for (int w = 0; w < liczbaWatkow; w++) {
            final int nrWatku = w;
            Thread t = new Thread(() -> {
                for (int i = nrWatku; i < linie.size(); i += liczbaWatkow) {
                    for (String slowo : linie.get(i).toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                        if (slowo.isEmpty()) {
                            continue;
                        }
                        Long ile = wystapienia.get(slowo);
                        if (ile == null) {
                            wystapienia.put(slowo, 1L);
                        } else {
                            wystapienia.put(slowo, ile + 1);
                        }
                    }
                }
            });
            watki.add(t);
            t.start();
        }
        for (Thread t : watki) {
            t.join();
        }
        return wystapienia;
    }
}
