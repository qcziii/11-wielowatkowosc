# 02. Współdzielony stan: wyścigi, atomowość, synchronized

Ćwiczenia: [03](../cwiczenia/03-licznik-odwiedzin/ZADANIE.md), [04](../cwiczenia/04-konto-na-minusie/ZADANIE.md)

## Wyścig (race condition)

```java
class Counter {
    private int value;
    void increment() { value++; }
}
```

Dwa wątki wołają `increment()` po 1 000 000 razy. Wynik powinien wynosić 2 000 000, a wychodzi np. 1 312 448, za każdym razem inny. Dlaczego?

`value++` to nie jedna operacja, tylko trzy:

```
1. odczytaj wartosc z pamięci do rejestru   (np. 41)
2. dodaj 1                                   (42)
3. zapisz wynik do pamięci                   (42)
```

Przeplot dwóch wątków:

```
Wątek A: odczytaj 41
Wątek B: odczytaj 41
Wątek A: dodaj 1, zapisz 42
Wątek B: dodaj 1, zapisz 42      <- zwiększenie A zgubione
```

To jest **wyścig**: wynik zależy od tego, jak wątki się przeplotą. Wyścigi są zdradliwe, bo:

- w testach jednowątkowych wszystko działa,
- przy małym obciążeniu prawie zawsze działa,
- błąd pojawia się losowo, „raz na tydzień na produkcji”,
- dodanie `System.out.println` albo debuggera zmienia przeploty i błąd „znika”.

## Sekcja krytyczna i `synchronized`

Fragment kodu, w którym naraz może być tylko jeden wątek, to **sekcja krytyczna**. W Javie najprostszy sposób to `synchronized`:

```java
class Counter {
    private int value;

    synchronized void increment() { value++; }   // lock on this
    synchronized int value() { return value; }
}
```

Każdy obiekt w Javie ma wbudowaną blokadę (*monitor*, *intrinsic lock*). `synchronized` na metodzie instancyjnej bierze blokadę na `this`, na metodzie statycznej na obiekcie `Counter.class`. Można też zablokować dowolny obiekt:

```java
private final Object lock = new Object();

void increment() {
    synchronized (lock) {
        value++;
    }
}
```

Zasady:

1. **Blokada działa tylko wtedy, gdy wszyscy używają tej samej.** Jeśli `increment()` jest `synchronized`, a `decrement()` nie, to `decrement()` może się wykonać w środku `increment()`.
2. **Odczyt też musi być pod blokadą** (albo przez `volatile`, rozdział 03). Inaczej czytający wątek może nie zobaczyć zapisu.
3. Wątek, który czeka na blokadę, ma stan `BLOCKED`. Czeka bez limitu i nie da się go przerwać `interrupt()`.
4. Blokada jest **wielowejściowa** (*reentrant*): wątek, który ją trzyma, może wejść do kolejnej metody `synchronized` na tym samym obiekcie.
5. Blokada jest zwalniana zawsze przy wyjściu z bloku, także przez wyjątek.
6. Nie synchronizuj na obiektach, które ktoś inny może też zablokować albo które się zmieniają: `String` z literału, `Integer`, pole, któremu przypisujesz nowy obiekt.

## Klasy atomowe

Dla prostych liczników i flag jest `java.util.concurrent.atomic`:

```java
private final AtomicInteger value = new AtomicInteger();

void increment() { value.incrementAndGet(); }   // atomic ++
int value() { return value.get(); }
```

Pod spodem nie ma blokady, tylko instrukcja procesora **compare-and-set (CAS)**: „zamień wartość na X, ale tylko jeśli nadal wynosi Y”. Jeśli inny wątek zdążył zmienić wartość, CAS się nie udaje i operacja próbuje jeszcze raz.

Przydatne metody `AtomicInteger`/`AtomicLong`:

```java
get(), set(v)
incrementAndGet(), getAndIncrement(), addAndGet(d)
compareAndSet(expected, newValue)       // true if it succeeded
updateAndGet(x -> x * 2)              // any function, atomically (the function may be called several times!)
accumulateAndGet(v, Math::max)
```

Inne: `AtomicBoolean`, `AtomicReference<T>`, `LongAdder` (szybszy od `AtomicLong` przy bardzo wielu wątkach, które głównie zwiększają licznik, a rzadko go czytają).

## Sprawdź-i-działaj (check-then-act)

Każda pojedyncza operacja na `AtomicInteger` jest atomowa. **Ciąg** operacji już nie:

```java
// WRONG, despite AtomicInteger
if (balance.get() >= amount) {        // 1. check
    balance.addAndGet(-amount);       // 2. act
}
```

Między 1 i 2 inny wątek może wypłacić pieniądze. Dwa wątki widzą saldo 100, oba wypłacają 100, saldo wynosi -100. Ten wzorzec błędu nazywa się **check-then-act**. Jego kuzyn to **read-modify-write** (`x++`, `map.put(k, map.get(k) + 1)`).

Rozwiązania:

```java
// a) one lock around the whole sequence
synchronized boolean withdraw(int amount) {
    if (balance < amount) return false;
    balance -= amount;
    return true;
}

// b) CAS loop: change only if nobody changed it since our read
boolean withdraw(int amount) {
    while (true) {
        int currentBalance = balance.get();
        if (currentBalance < amount) return false;
        if (balance.compareAndSet(currentBalance, currentBalance - amount)) return true;
        // someone got there first: retry with the new value
    }
}
```

Inne typowe check-then-act:

```java
if (!map.containsKey(k)) map.put(k, v);       // use putIfAbsent / computeIfAbsent
if (instance == null) instance = new X();     // lazy initialization, chapter 03
if (!list.isEmpty()) list.remove(0);          // use a queue with poll()
```

## Co jest bezpieczne bez synchronizacji

- **Zmienne lokalne** i parametry: każdy wątek ma swój stos, nikt inny ich nie widzi.
- **Obiekty niemutowalne** (wszystkie pola `final`, stan nie zmienia się po konstruktorze): `String`, `Integer`, `LocalDate`, `BigDecimal`, rekordy z niemutowalnymi polami. Można je dzielić bez ograniczeń.
- **Obiekty używane tylko przez jeden wątek** (*thread confinement*).
- Obiekty z `java.util.concurrent`, ale tylko w zakresie pojedynczych operacji (rozdział 08).

Najlepsza synchronizacja to brak współdzielonego stanu: każdy wątek liczy swoją część, a łączymy wyniki na końcu w jednym wątku.

## Pytania sprawdzające

1. Rozpisz `x++` na kroki i pokaż przeplot dwóch wątków, który gubi jedno zwiększenie.
2. Metoda `increment()` jest `synchronized`, a `value()` nie. Co może pójść nie tak?
3. Na jakim obiekcie bierze blokadę `synchronized` na metodzie statycznej?
4. Co to jest check-then-act? Podaj przykład z kolekcją.
5. Dlaczego `updateAndGet(f)` może wywołać funkcję `f` więcej niż raz? Co z tego wynika dla tej funkcji?
6. Czy obiekt `String` można bezpiecznie przekazywać między wątkami? A `ArrayList`?
