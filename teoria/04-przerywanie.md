# 04. Przerywanie wątków

Ćwiczenia: [06](../cwiczenia/06-polkniete-przerwanie/ZADANIE.md)

## Wątku nie da się zabić, można go poprosić

W Javie nie ma bezpiecznego sposobu na „zabicie” wątku z zewnątrz (`Thread.stop()` jest od dawna wycofane i w nowych wersjach rzuca `UnsupportedOperationException`). Zatrzymanie wątku to zawsze **współpraca**: jeden wątek prosi, drugi sam sprawdza prośbę i sam się kończy.

Do tej prośby służy mechanizm przerwań:

```java
t.interrupt();                          // set the "interrupted" flag on thread t
Thread.currentThread().isInterrupted()  // check the current thread's flag (does not change it)
Thread.interrupted()                    // static: check AND CLEAR the current thread's flag
```

`interrupt()` nie przerywa niczego siłą. Ustawia flagę i budzi wątek, jeśli ten czeka w metodzie blokującej.

## Co się dzieje przy `interrupt()`

| Wątek w chwili przerwania | Skutek |
|---|---|
| liczy (pętla, obliczenia) | tylko flaga = true; wątek musi sam ją sprawdzić |
| śpi/czeka: `sleep`, `wait`, `join`, `BlockingQueue.take/put`, `Lock.lockInterruptibly`, `Future.get`, `Semaphore.acquire`... | metoda rzuca `InterruptedException` i **czyści flagę** (flaga = false) |
| czeka na `synchronized` (`BLOCKED`) albo na zwykłe `Lock.lock()` | nic, czeka dalej (flaga = true) |
| czeka na zwykłe I/O z `java.io` (np. `InputStream.read` z gniazda) | zwykle nic; trzeba zamknąć strumień albo gniazdo |

Najważniejszy wiersz to drugi: **`InterruptedException` kasuje flagę.** Kto złapał wyjątek i nic z nim nie zrobił, ten zgubił informację o przerwaniu.

## Jak poprawnie reagować na `InterruptedException`

Błąd, który w kodzie spotkasz najczęściej:

```java
// WRONG: swallowed interruption
while (!Thread.currentThread().isInterrupted()) {
    doSomething();
    try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        log.warn("interrupted");   // the flag is already false, the loop keeps spinning
    }
}
```

Dwie poprawne reakcje:

```java
// 1. Propagate: declare throws InterruptedException and do not catch it.
//    Best for "library" methods that do not decide the thread's fate.
void awaitData() throws InterruptedException {
    Thread.sleep(100);
}

// 2. If you cannot rethrow (e.g. in Runnable.run): restore the flag and stop working.
try {
    Thread.sleep(100);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // restore the flag so that the caller sees it
    return;                               // or break; the loop will see the flag anyway
}
```

Zła reakcja: złapać i zignorować (albo tylko zalogować). Wyjątek od tej reguły jest jeden: kod, który *jest* właścicielem wątku i świadomie decyduje, że to przerwanie oznacza „zakończ”, a potem się kończy.

Też źle: opakowanie w `RuntimeException` bez przywrócenia flagi. Jeśli musisz rzucić wyjątek niekontrolowany, przywróć flagę przed `throw`.

## Własna flaga czy przerwanie?

Flaga `volatile boolean running` (rozdział 03) też działa, ale nie obudzi wątku, który śpi w `sleep(10_000)` albo czeka w `take()`. Przerwanie budzi go od razu. Dlatego:

- pętla, która tylko liczy: flaga `volatile` albo `isInterrupted()`, obie w porządku,
- pętla, która czeka (sleep, kolejka, `Future.get`): przerwanie.

Pule wątków (`shutdownNow()`, `Future.cancel(true)`) zatrzymują zadania **przez przerwanie**. Zadanie, które połyka `InterruptedException`, nie da się anulować, a pula nie da się zamknąć.

## Pytania sprawdzające

1. Co robi `t.interrupt()` z wątkiem, który liczy w pętli, a co z wątkiem, który śpi w `sleep`?
2. Co się dzieje z flagą przerwania, gdy `sleep` rzuca `InterruptedException`?
3. Jakie są dwie poprawne reakcje na `InterruptedException`?
4. Czym różni się `Thread.interrupted()` od `isInterrupted()`?
5. Dlaczego zadanie, które połyka `InterruptedException`, psuje `shutdownNow()`?
