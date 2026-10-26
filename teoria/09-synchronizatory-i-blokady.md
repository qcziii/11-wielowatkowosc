# 09. Synchronizatory i blokady z java.util.concurrent

Ćwiczenia: [12](../cwiczenia/12-pula-polaczen/ZADANIE.md), [13](../cwiczenia/13-konfiguracja-rwlock/ZADANIE.md)

## Synchronizatory

Gotowe klocki do koordynacji wątków. Każdy z nich zastępuje kilkanaście linii `wait`/`notifyAll`.

### CountDownLatch: „poczekaj, aż N rzeczy się wydarzy”

```java
CountDownLatch done = new CountDownLatch(3);

// in each of the 3 threads, when done
done.countDown();

// in the main thread
done.await();                         // waits until the count reaches 0
done.await(5, TimeUnit.SECONDS);      // with a timeout, returns false when it expires
```

Jednorazowy: licznik nie wraca do N. Typowe użycia: poczekaj na start wszystkich usług; „pistolet startowy” (`new CountDownLatch(1)`, wszystkie wątki robią `await()`, główny jeden `countDown()` i wszystkie ruszają naraz; tak startują wątki w testach w tym repo).

### CyclicBarrier: „spotkajmy się wszyscy w tym punkcie”

```java
CyclicBarrier barrier = new CyclicBarrier(4, () -> System.out.println("round finished"));
// each of the 4 threads
barrier.await();    // waits until all 4 arrive; then all of them continue
```

Wielokrotnego użytku (cykliczna). Używana w algorytmach „w rundach”.

### Semaphore: „najwyżej N naraz”

```java
Semaphore permits = new Semaphore(3);

permits.acquire();          // take a permit; waits if none are free
try {
    useResource();
} finally {
    permits.release();      // ALWAYS give it back
}
```

Semafor nie pamięta, kto wziął zezwolenie. `release()` zawołane bez wcześniejszego `acquire()` po prostu **dodaje** zezwolenie, i od tej chwili naraz może wejść N+1 wątków. Dlatego:

```java
// WRONG: an exception in useResource() = permit lost forever; after N errors everyone waits
permits.acquire();
useResource();
permits.release();

// WRONG: if acquire() throws InterruptedException, finally releases a permit we never had
try {
    permits.acquire();
    useResource();
} finally {
    permits.release();
}

// RIGHT: acquire before try, release in finally
permits.acquire();
try {
    useResource();
} finally {
    permits.release();
}
```

Ten sam wzorzec obowiązuje dla każdej blokady (`lock()` przed `try`, `unlock()` w `finally`).

## Lock i ReentrantLock

`synchronized` jest proste, ale ma ograniczenia: nie można czekać z limitem czasu, nie można przerwać czekania, blokada musi być zwolniona w tym samym bloku. `ReentrantLock` daje to wszystko, w zamian za obowiązek ręcznego zwalniania:

```java
private final ReentrantLock lock = new ReentrantLock();

void operation() {
    lock.lock();
    try {
        // critical section
    } finally {
        lock.unlock();     // without finally: an exception = lock held forever
    }
}
```

Dodatkowe możliwości:

```java
if (lock.tryLock()) { ... }                          // take it if free, do not wait
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) { ... } // wait up to 100 ms
lock.lockInterruptibly();                            // waiting can be interrupted with interrupt()
new ReentrantLock(true);                                // "fair": first come, first served (slower)
```

`Condition` to odpowiednik `wait`/`notify`, ale można mieć kilka warunków na jednej blokadzie:

```java
private final Condition notEmpty = lock.newCondition();
private final Condition notFull = lock.newCondition();

// in take(), under the lock
while (queue.isEmpty()) notEmpty.await();
...
notFull.signal();
```

Z osobnymi warunkami można bezpiecznie użyć `signal()` (jak `notify()`), bo na każdym warunku czekają tylko wątki, którym ten sygnał coś zmienia.

Kiedy co: domyślnie `synchronized` (krótsze, nie da się zapomnieć zwolnić). `ReentrantLock`, gdy potrzebujesz `tryLock`, limitu czasu, przerwania albo kilku `Condition`.

## ReadWriteLock: wielu czytelników albo jeden pisarz

Gdy dane są dużo częściej czytane niż zmieniane:

```java
private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

String get(String k) {
    rw.readLock().lock();          // many readers at once
    try {
        return map.get(k);
    } finally {
        rw.readLock().unlock();
    }
}

void reload(Map<String, String> newSettings) {
    rw.writeLock().lock();         // exclusive: waits until all readers leave; nobody reads meanwhile
    try {
        map.clear();
        map.putAll(newSettings);
    } finally {
        rw.writeLock().unlock();
    }
}
```

Uwagi:

- Blokady do odczytu **nie da się podnieść** do blokady do zapisu: wątek trzymający `readLock` i wołający `writeLock().lock()` zawiśnie na zawsze (czeka, aż wyjdą wszyscy czytelnicy, w tym on sam). W drugą stronę (zapis → odczyt) można.
- Często prościej i szybciej jest w ogóle uniknąć blokady: trzymać **niemutowalną** mapę w polu `volatile` i przy przeładowaniu podmieniać całą referencję (`this.settings = Map.copyOf(newSettings)`). Czytelnik bierze referencję raz i ma spójny obraz.
- Jest też `StampedLock` (optymistyczne odczyty), ale to już rzadko potrzebna optymalizacja.

## Pytania sprawdzające

1. Czym się różni `CountDownLatch` od `CyclicBarrier`?
2. Dlaczego `acquire()` semafora stoi przed `try`, a nie w środku?
3. Co się stanie, jeśli `unlock()` nie będzie w `finally`, a sekcja krytyczna rzuci wyjątek?
4. Jakie trzy rzeczy daje `ReentrantLock`, których nie daje `synchronized`?
5. Ilu czytelników i ilu pisarzy może naraz trzymać `ReentrantReadWriteLock`?
6. Co się stanie, gdy wątek z blokadą do odczytu spróbuje wziąć blokadę do zapisu?
