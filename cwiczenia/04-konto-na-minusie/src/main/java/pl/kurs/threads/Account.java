package pl.kurs.threads;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Account without overdraft: the balance must never drop below zero.
 * The account is used by many threads at the same time (ATMs, transfers, card payments).
 */
public class Account {

    private final AtomicInteger balance;

    public Account(int initialBalance) {
        this.balance = new AtomicInteger(initialBalance);
    }

    public void deposit(int amount) {
        balance.addAndGet(amount);
    }

    /**
     * Withdraws the amount if there is enough money in the account.
     *
     * @return true if the withdrawal succeeded; false if funds are insufficient (the balance does not change)
     */
    public boolean withdraw(int amount) {
        if (balance.get() >= amount) {
            balance.addAndGet(-amount);
            return true;
        }
        return false;
    }

    public int balance() {
        return balance.get();
    }
}
