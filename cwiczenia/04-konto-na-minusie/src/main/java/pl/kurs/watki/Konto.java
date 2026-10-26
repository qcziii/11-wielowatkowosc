package pl.kurs.watki;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Konto bez debetu: saldo nigdy nie może spaść poniżej zera.
 * Z konta korzysta naraz wiele wątków (bankomaty, przelewy, karta).
 */
public class Konto {

    private final AtomicInteger saldo;

    public Konto(int saldoPoczatkowe) {
        this.saldo = new AtomicInteger(saldoPoczatkowe);
    }

    public void wplac(int kwota) {
        saldo.addAndGet(kwota);
    }

    /**
     * Wypłaca kwotę, jeśli na koncie jest wystarczająco pieniędzy.
     *
     * @return true, jeśli wypłata się udała; false, jeśli brakuje środków (saldo się nie zmienia)
     */
    public boolean wyplac(int kwota) {
        if (saldo.get() >= kwota) {
            saldo.addAndGet(-kwota);
            return true;
        }
        return false;
    }

    public int saldo() {
        return saldo.get();
    }
}
