# 09. Synchronizatory i blokady z java.util.concurrent

Ćwiczenia: [12](../cwiczenia/12-pula-polaczen/ZADANIE.md), [13](../cwiczenia/13-konfiguracja-rwlock/ZADANIE.md)

## Synchronizatory

Gotowe klocki do koordynacji wątków. Każdy z nich zastępuje kilkanaście linii `wait`/`notifyAll`.

### CountDownLatch: „poczekaj, aż N rzeczy się wydarzy”

```java
CountDownLatch gotowe = new CountDownLatch(3);

// w każdym z 3 wątków, na końcu pracy
gotowe.countDown();

// w wątku głównym
gotowe.await();                         // czeka, aż licznik spadnie do 0
gotowe.await(5, TimeUnit.SECONDS);      // z limitem, zwraca false po czasie
```

Jednorazowy: licznik nie wraca do N. Typowe użycia: poczekaj na start wszystkich usług; „pistolet startowy” (`new CountDownLatch(1)`, wszystkie wątki robią `await()`, główny jeden `countDown()` i wszystkie ruszają naraz; tak startują wątki w testach w tym repo).

### CyclicBarrier: „spotkajmy się wszyscy w tym punkcie”

```java
CyclicBarrier bariera = new CyclicBarrier(4, () -> System.out.println("runda skończona"));
// każdy z 4 wątków
bariera.await();    // czeka, aż dojdą wszyscy 4; potem wszyscy ruszają dalej
```

Wielokrotnego użytku (cykliczna). Używana w algorytmach „w rundach”.

### Semaphore: „najwyżej N naraz”

```java
Semaphore miejsca = new Semaphore(3);

miejsca.acquire();          // weź zezwolenie; czeka, gdy nie ma wolnych
try {
    uzyjZasobu();
} finally {
    miejsca.release();      // ZAWSZE oddaj
}
```

Semafor nie pamięta, kto wziął zezwolenie. `release()` zawołane bez wcześniejszego `acquire()` po prostu **dodaje** zezwolenie, i od tej chwili naraz może wejść N+1 wątków. Dlatego:

```java
// ŹLE: wyjątek w uzyjZasobu() = zezwolenie stracone na zawsze; po N błędach wszyscy czekają
miejsca.acquire();
uzyjZasobu();
miejsca.release();

// ŹLE: jeśli acquire() rzuci InterruptedException, finally odda zezwolenie, którego nie mieliśmy
try {
    miejsca.acquire();
    uzyjZasobu();
} finally {
    miejsca.release();
}

// DOBRZE: acquire przed try, release w finally
miejsca.acquire();
try {
    uzyjZasobu();
} finally {
    miejsca.release();
}
```

Ten sam wzorzec obowiązuje dla każdej blokady (`lock()` przed `try`, `unlock()` w `finally`).

## Lock i ReentrantLock

`synchronized` jest proste, ale ma ograniczenia: nie można czekać z limitem czasu, nie można przerwać czekania, blokada musi być zwolniona w tym samym bloku. `ReentrantLock` daje to wszystko, w zamian za obowiązek ręcznego zwalniania:

```java
private final ReentrantLock blokada = new ReentrantLock();

void operacja() {
    blokada.lock();
    try {
        // sekcja krytyczna
    } finally {
        blokada.unlock();     // bez finally: wyjątek = blokada zajęta na zawsze
    }
}
```

Dodatkowe możliwości:

```java
if (blokada.tryLock()) { ... }                          // weź, jeśli wolna, nie czekaj
if (blokada.tryLock(100, TimeUnit.MILLISECONDS)) { ... } // czekaj max 100 ms
blokada.lockInterruptibly();                            // czekanie da się przerwać interrupt()
new ReentrantLock(true);                                // „sprawiedliwa”: kolejność zgłoszeń (wolniejsza)
```

`Condition` to odpowiednik `wait`/`notify`, ale można mieć kilka warunków na jednej blokadzie:

```java
private final Condition niePusty = blokada.newCondition();
private final Condition niePelny = blokada.newCondition();

// w wez(), pod blokadą
while (kolejka.isEmpty()) niePusty.await();
...
niePelny.signal();
```

Z osobnymi warunkami można bezpiecznie użyć `signal()` (jak `notify()`), bo na każdym warunku czekają tylko wątki, którym ten sygnał coś zmienia.

Kiedy co: domyślnie `synchronized` (krótsze, nie da się zapomnieć zwolnić). `ReentrantLock`, gdy potrzebujesz `tryLock`, limitu czasu, przerwania albo kilku `Condition`.

## ReadWriteLock: wielu czytelników albo jeden pisarz

Gdy dane są dużo częściej czytane niż zmieniane:

```java
private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

String pobierz(String k) {
    rw.readLock().lock();          // wielu czytelników naraz
    try {
        return mapa.get(k);
    } finally {
        rw.readLock().unlock();
    }
}

void przeladuj(Map<String, String> nowe) {
    rw.writeLock().lock();         // wyłączny: czeka, aż wyjdą wszyscy czytelnicy; w tym czasie nikt nie czyta
    try {
        mapa.clear();
        mapa.putAll(nowe);
    } finally {
        rw.writeLock().unlock();
    }
}
```

Uwagi:

- Blokady do odczytu **nie da się podnieść** do blokady do zapisu: wątek trzymający `readLock` i wołający `writeLock().lock()` zawiśnie na zawsze (czeka, aż wyjdą wszyscy czytelnicy, w tym on sam). W drugą stronę (zapis → odczyt) można.
- Często prościej i szybciej jest w ogóle uniknąć blokady: trzymać **niemutowalną** mapę w polu `volatile` i przy przeładowaniu podmieniać całą referencję (`this.ustawienia = Map.copyOf(nowe)`). Czytelnik bierze referencję raz i ma spójny obraz.
- Jest też `StampedLock` (optymistyczne odczyty), ale to już rzadko potrzebna optymalizacja.

## Pytania sprawdzające

1. Czym się różni `CountDownLatch` od `CyclicBarrier`?
2. Dlaczego `acquire()` semafora stoi przed `try`, a nie w środku?
3. Co się stanie, jeśli `unlock()` nie będzie w `finally`, a sekcja krytyczna rzuci wyjątek?
4. Jakie trzy rzeczy daje `ReentrantLock`, których nie daje `synchronized`?
5. Ilu czytelników i ilu pisarzy może naraz trzymać `ReentrantReadWriteLock`?
6. Co się stanie, gdy wątek z blokadą do odczytu spróbuje wziąć blokadę do zapisu?
