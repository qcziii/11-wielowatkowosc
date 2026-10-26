package pl.kurs.threads;

/**
 * Bank account. The balance changes only in {@link Bank#transfer}, under the lock on the account object.
 */
public class Account {

    private final long number;
    private long balance;

    public Account(long number, long balance) {
        this.number = number;
        this.balance = balance;
    }

    public long number() {
        return number;
    }

    public synchronized long balance() {
        return balance;
    }

    void changeBalance(long delta) {
        balance += delta;
    }

    @Override
    public String toString() {
        return "Account " + number;
    }
}
