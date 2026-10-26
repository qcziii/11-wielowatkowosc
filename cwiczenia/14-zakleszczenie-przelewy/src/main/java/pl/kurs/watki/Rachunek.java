package pl.kurs.watki;

/**
 * Rachunek bankowy. Saldo zmienia się tylko w {@link Bank#przelej}, pod blokadą na obiekcie rachunku.
 */
public class Rachunek {

    private final long numer;
    private long saldo;

    public Rachunek(long numer, long saldo) {
        this.numer = numer;
        this.saldo = saldo;
    }

    public long numer() {
        return numer;
    }

    public synchronized long saldo() {
        return saldo;
    }

    void zmienSaldo(long o) {
        saldo += o;
    }

    @Override
    public String toString() {
        return "Rachunek " + numer;
    }
}
