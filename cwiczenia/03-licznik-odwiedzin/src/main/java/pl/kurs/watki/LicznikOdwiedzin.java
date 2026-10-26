package pl.kurs.watki;

/**
 * Licznik odwiedzin strony. Metodę odwiedz() woła wiele wątków serwera naraz.
 */
public class LicznikOdwiedzin {

    // "volatile, żeby wszystkie wątki widziały aktualną wartość" (z code review)
    private volatile int odwiedziny;
    private volatile int unikalni;

    public void odwiedz(boolean nowyUzytkownik) {
        odwiedziny++;
        if (nowyUzytkownik) {
            unikalni++;
        }
    }

    public int odwiedziny() {
        return odwiedziny;
    }

    public int unikalni() {
        return unikalni;
    }
}
