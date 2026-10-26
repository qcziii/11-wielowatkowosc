package pl.kurs.threads;

/**
 * Transfers between accounts. Transfers are executed by many threads at the same time.
 */
public class Bank {

    /**
     * Transfers the amount if the source account has enough funds.
     *
     * @return true if the transfer was executed
     */
    public boolean transfer(Account from, Account to, long amount) {
        synchronized (from) {
            synchronized (to) {
                if (from.balance() < amount) {
                    return false;
                }
                from.changeBalance(-amount);
                to.changeBalance(amount);
                return true;
            }
        }
    }
}
