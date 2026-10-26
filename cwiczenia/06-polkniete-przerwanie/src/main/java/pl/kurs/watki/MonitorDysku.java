package pl.kurs.watki;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

/**
 * Co 100 ms sprawdza zajętość dysku i zapamiętuje ostatni odczyt.
 * Działa w osobnym wątku, który start() uruchamia, a stop() wyłącza.
 */
public class MonitorDysku {

    private final IntSupplier czujnik;
    private final AtomicInteger pomiary = new AtomicInteger();
    private volatile int ostatniOdczyt = -1;
    private Thread watek;

    public MonitorDysku(IntSupplier czujnik) {
        this.czujnik = czujnik;
    }

    public void start() {
        watek = new Thread(this::petla, "monitor-dysku");
        watek.start();
    }

    private void petla() {
        while (!Thread.currentThread().isInterrupted()) {
            ostatniOdczyt = czujnik.getAsInt();
            pomiary.incrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("Monitor: przerwano oczekiwanie");
            }
        }
        System.out.println("Monitor: koniec pracy");
    }

    /**
     * Wyłącza monitor i czeka na zakończenie jego wątku (maksymalnie 1 s).
     *
     * @return true, jeśli wątek monitora się zakończył
     */
    public boolean stop() throws InterruptedException {
        watek.interrupt();
        watek.join(1000);
        return !watek.isAlive();
    }

    public int pomiary() {
        return pomiary.get();
    }

    public int ostatniOdczyt() {
        return ostatniOdczyt;
    }
}
