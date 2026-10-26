package pl.kurs.watki;

/**
 * Przelewy między rachunkami. Przelewy wykonuje naraz wiele wątków.
 */
public class Bank {

    /**
     * Przelewa kwotę, jeśli na rachunku źródłowym są środki.
     *
     * @return true, jeśli przelew się wykonał
     */
    public boolean przelej(Rachunek z, Rachunek na, long kwota) {
        synchronized (z) {
            synchronized (na) {
                if (z.saldo() < kwota) {
                    return false;
                }
                z.zmienSaldo(-kwota);
                na.zmienSaldo(kwota);
                return true;
            }
        }
    }
}
