package pl.kurs.watki;

import java.util.ArrayList;
import java.util.List;

/**
 * Uruchamia każde zadanie w osobnym wątku i czeka, aż wszystkie się skończą.
 */
public class UruchamiaczZadan {

    /**
     * Uruchamia wszystkie zadania równolegle (każde w swoim wątku) i wraca dopiero,
     * gdy wszystkie się zakończą.
     */
    public void uruchomRownolegle(List<Runnable> zadania) throws InterruptedException {
        List<Thread> watki = new ArrayList<>();
        int nr = 1;
        for (Runnable zadanie : zadania) {
            Thread watek = new Thread(zadanie, "zadanie-" + nr++);
            watki.add(watek);
            watek.run();
        }
        for (Thread watek : watki) {
            watek.join();
        }
    }
}
