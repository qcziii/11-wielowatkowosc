package pl.kurs.watki;

/**
 * Liczy sumę elementów tablicy, dzieląc ją na części. Każdą część sumuje osobny wątek.
 */
public class SumaRownolegla {

    public long suma(long[] tablica, int liczbaWatkow) {
        long[] sumyCzesci = new long[liczbaWatkow];
        Thread[] watki = new Thread[liczbaWatkow];
        int rozmiarCzesci = tablica.length / liczbaWatkow;

        for (int i = 0; i < liczbaWatkow; i++) {
            final int nr = i;
            final int od = i * rozmiarCzesci;
            final int doWylacznie = od + rozmiarCzesci;
            watki[i] = new Thread(() -> {
                long s = 0;
                for (int j = od; j < doWylacznie; j++) {
                    s += tablica[j];
                }
                sumyCzesci[nr] = s;
            }, "suma-" + i);
            watki[i].start();
        }

        long wynik = 0;
        for (long s : sumyCzesci) {
            wynik += s;
        }
        return wynik;
    }
}
