package pl.kurs.watki;

/**
 * Generator działa w osobnym wątku i w pętli liczy kolejne kroki symulacji,
 * dopóki ktoś z innego wątku nie wywoła zatrzymaj().
 */
public class Generator implements Runnable {

    private boolean dziala = true;
    private long kroki;

    @Override
    public void run() {
        while (dziala) {
            kroki++;
        }
    }

    /** Wołane z innego wątku (np. z wątku obsługi przycisku „Stop”). */
    public void zatrzymaj() {
        dziala = false;
    }

    /** Liczba wykonanych kroków. Czytana po zakończeniu wątku generatora. */
    public long kroki() {
        return kroki;
    }
}
