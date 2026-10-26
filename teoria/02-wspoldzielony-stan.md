# 02. Współdzielony stan: wyścigi, atomowość, synchronized

Ćwiczenia: [03](../cwiczenia/03-licznik-odwiedzin/ZADANIE.md), [04](../cwiczenia/04-konto-na-minusie/ZADANIE.md)

## Wyścig (race condition)

```java
class Licznik {
    private int wartosc;
    void zwieksz() { wartosc++; }
}
```

Dwa wątki wołają `zwieksz()` po 1 000 000 razy. Wynik powinien wynosić 2 000 000, a wychodzi np. 1 312 448, za każdym razem inny. Dlaczego?

`wartosc++` to nie jedna operacja, tylko trzy:

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
class Licznik {
    private int wartosc;

    synchronized void zwieksz() { wartosc++; }   // blokada na obiekcie this
    synchronized int wartosc() { return wartosc; }
}
```

Każdy obiekt w Javie ma wbudowaną blokadę (*monitor*, *intrinsic lock*). `synchronized` na metodzie instancyjnej bierze blokadę na `this`, na metodzie statycznej na obiekcie `Licznik.class`. Można też zablokować dowolny obiekt:

```java
private final Object blokada = new Object();

void zwieksz() {
    synchronized (blokada) {
        wartosc++;
    }
}
```

Zasady:

1. **Blokada działa tylko wtedy, gdy wszyscy używają tej samej.** Jeśli `zwieksz()` jest `synchronized`, a `zmniejsz()` nie, to `zmniejsz()` może się wykonać w środku `zwieksz()`.
2. **Odczyt też musi być pod blokadą** (albo przez `volatile`, rozdział 03). Inaczej czytający wątek może nie zobaczyć zapisu.
3. Wątek, który czeka na blokadę, ma stan `BLOCKED`. Czeka bez limitu i nie da się go przerwać `interrupt()`.
4. Blokada jest **wielowejściowa** (*reentrant*): wątek, który ją trzyma, może wejść do kolejnej metody `synchronized` na tym samym obiekcie.
5. Blokada jest zwalniana zawsze przy wyjściu z bloku, także przez wyjątek.
6. Nie synchronizuj na obiektach, które ktoś inny może też zablokować albo które się zmieniają: `String` z literału, `Integer`, pole, któremu przypisujesz nowy obiekt.

## Klasy atomowe

Dla prostych liczników i flag jest `java.util.concurrent.atomic`:

```java
private final AtomicInteger wartosc = new AtomicInteger();

void zwieksz() { wartosc.incrementAndGet(); }   // atomowe ++
int wartosc() { return wartosc.get(); }
```

Pod spodem nie ma blokady, tylko instrukcja procesora **compare-and-set (CAS)**: „zamień wartość na X, ale tylko jeśli nadal wynosi Y”. Jeśli inny wątek zdążył zmienić wartość, CAS się nie udaje i operacja próbuje jeszcze raz.

Przydatne metody `AtomicInteger`/`AtomicLong`:

```java
get(), set(v)
incrementAndGet(), getAndIncrement(), addAndGet(d)
compareAndSet(oczekiwana, nowa)       // true, jeśli się udało
updateAndGet(x -> x * 2)              // dowolna funkcja, atomowo (funkcja może być wołana kilka razy!)
accumulateAndGet(v, Math::max)
```

Inne: `AtomicBoolean`, `AtomicReference<T>`, `LongAdder` (szybszy od `AtomicLong` przy bardzo wielu wątkach, które głównie zwiększają licznik, a rzadko go czytają).

## Sprawdź-i-działaj (check-then-act)

Każda pojedyncza operacja na `AtomicInteger` jest atomowa. **Ciąg** operacji już nie:

```java
// ŹLE, mimo AtomicInteger
if (saldo.get() >= kwota) {        // 1. sprawdź
    saldo.addAndGet(-kwota);       // 2. działaj
}
```

Między 1 i 2 inny wątek może wypłacić pieniądze. Dwa wątki widzą saldo 100, oba wypłacają 100, saldo wynosi -100. Ten wzorzec błędu nazywa się **check-then-act**. Jego kuzyn to **read-modify-write** (`x++`, `mapa.put(k, mapa.get(k) + 1)`).

Rozwiązania:

```java
// a) jedna blokada na cały ciąg
synchronized boolean wyplac(int kwota) {
    if (saldo < kwota) return false;
    saldo -= kwota;
    return true;
}

// b) pętla CAS: zmień tylko, jeśli nikt nie zmienił od naszego odczytu
boolean wyplac(int kwota) {
    while (true) {
        int obecne = saldo.get();
        if (obecne < kwota) return false;
        if (saldo.compareAndSet(obecne, obecne - kwota)) return true;
        // ktoś nas wyprzedził: próbujemy jeszcze raz z nową wartością
    }
}
```

Inne typowe check-then-act:

```java
if (!mapa.containsKey(k)) mapa.put(k, v);       // użyj putIfAbsent / computeIfAbsent
if (instancja == null) instancja = new X();     // leniwa inicjalizacja, rozdział 03
if (!lista.isEmpty()) lista.remove(0);          // użyj kolejki z poll()
```

## Co jest bezpieczne bez synchronizacji

- **Zmienne lokalne** i parametry: każdy wątek ma swój stos, nikt inny ich nie widzi.
- **Obiekty niemutowalne** (wszystkie pola `final`, stan nie zmienia się po konstruktorze): `String`, `Integer`, `LocalDate`, `BigDecimal`, rekordy z niemutowalnymi polami. Można je dzielić bez ograniczeń.
- **Obiekty używane tylko przez jeden wątek** (*thread confinement*).
- Obiekty z `java.util.concurrent`, ale tylko w zakresie pojedynczych operacji (rozdział 08).

Najlepsza synchronizacja to brak współdzielonego stanu: każdy wątek liczy swoją część, a łączymy wyniki na końcu w jednym wątku.

## Pytania sprawdzające

1. Rozpisz `x++` na kroki i pokaż przeplot dwóch wątków, który gubi jedno zwiększenie.
2. Metoda `zwieksz()` jest `synchronized`, a `wartosc()` nie. Co może pójść nie tak?
3. Na jakim obiekcie bierze blokadę `synchronized` na metodzie statycznej?
4. Co to jest check-then-act? Podaj przykład z kolekcją.
5. Dlaczego `updateAndGet(f)` może wywołać funkcję `f` więcej niż raz? Co z tego wynika dla tej funkcji?
6. Czy obiekt `String` można bezpiecznie przekazywać między wątkami? A `ArrayList`?
