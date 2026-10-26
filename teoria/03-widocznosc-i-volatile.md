# 03. Widoczność, volatile i happens-before

Ćwiczenia: [05](../cwiczenia/05-watek-nie-chce-stanac/ZADANIE.md), [03](../cwiczenia/03-licznik-odwiedzin/ZADANIE.md)

## Problem: wątek nie widzi zmiany

```java
class Generator implements Runnable {
    private boolean dziala = true;

    public void run() {
        while (dziala) { /* licz */ }
    }

    public void zatrzymaj() { dziala = false; }   // wołane z innego wątku
}
```

Wywołanie `zatrzymaj()` z innego wątku często **nie zatrzymuje** pętli. To nie jest wyścig w sensie z rozdziału 02 (zapisuje tylko jeden wątek). To problem **widoczności**: nic nie gwarantuje, że wątek generatora kiedykolwiek zobaczy zapis innego wątku.

Dlaczego tak się dzieje:

- kompilator JIT widzi, że w pętli nikt nie zmienia `dziala`, więc może odczytać pole raz i zamienić pętlę na `if (dziala) while (true) {...}`,
- procesory mają pamięci podręczne (cache) i bufory zapisu, a wartości mogą siedzieć w rejestrach.

Java nie obiecuje, że zapis w jednym wątku jest widoczny w innym, **chyba że** coś te dwa wątki synchronizuje.

Dlaczego z debuggerem albo `System.out.println` w pętli „działa”: `println` ma w środku `synchronized`, a debugger wyłącza optymalizacje JIT. Kod zaczyna działać przypadkiem, a to nie jest naprawa.

## `volatile`

```java
private volatile boolean dziala = true;
```

`volatile` gwarantuje:

1. **Widoczność:** każdy odczyt pola `volatile` widzi ostatni zapis z dowolnego wątku. JIT nie może „zapamiętać” wartości poza pętlą.
2. **Uporządkowanie:** wszystko, co wątek zapisał *przed* zapisem do pola `volatile`, jest widoczne dla wątku, który *potem* odczytał to pole (patrz happens-before niżej).
3. Atomowy odczyt i zapis także dla `long` i `double` (bez `volatile` zapis 64-bitowej liczby teoretycznie może się rozpaść na dwa).

`volatile` **nie** gwarantuje atomowości operacji złożonych:

```java
private volatile int licznik;
licznik++;     // DALEJ wyścig: odczyt i zapis są osobno, każdy z osobna widoczny
```

Kiedy `volatile` wystarcza:

- flaga ustawiana przez jeden wątek i czytana przez inne (`dziala`, `zamknieto`),
- publikowanie referencji do niemutowalnego obiektu (`volatile Konfiguracja aktualna;` podmieniana w całości),
- gdy tylko jeden wątek pisze, a inni czytają.

Kiedy nie wystarcza: gdy nowa wartość zależy od starej (`++`, `+=`, check-then-act) i pisze więcej niż jeden wątek. Wtedy `synchronized` albo klasy atomowe.

## Happens-before

Model pamięci Javy (JMM) mówi, kiedy zapis w jednym wątku jest **na pewno** widoczny w innym. Jeśli akcja A *happens-before* akcja B, to B widzi skutki A. Najważniejsze reguły:

| Reguła | Co daje |
|---|---|
| kolejność w jednym wątku | wątek widzi swoje zapisy w kolejności programu |
| zwolnienie blokady → wzięcie tej samej blokady | wszystko zapisane przed `}` bloku `synchronized` widzi ten, kto potem wejdzie do `synchronized` na tym samym obiekcie |
| zapis `volatile` → odczyt tego samego pola | jak wyżej, dla pola volatile |
| `t.start()` → pierwsza akcja wątku `t` | nowy wątek widzi wszystko, co rodzic zapisał przed `start()` |
| ostatnia akcja wątku `t` → powrót z `t.join()` | po `join()` widać wszystko, co `t` zapisał |
| zadanie przekazane do `ExecutorService` | wątek puli widzi stan sprzed `submit`; wołający widzi wynik po `Future.get()` |
| akcje w `java.util.concurrent` | np. `put` w `BlockingQueue` → `take` tego elementu; `countDown()` → powrót z `await()` |

Reguły są **przechodnie**. Dlatego w ćwiczeniu 05 pole `kroki` nie musi być `volatile`: wątek generatora zapisuje je, kończy się, a test czyta je po `join()`.

Z tego wynika praktyczna zasada: **każdy dostęp do współdzielonego, zmiennego stanu musi przejść przez jakąś krawędź happens-before.** Zwykle zapewnia ją `synchronized`, `volatile`, klasy atomowe, kolekcje współbieżne albo `join`/`Future.get`.

## Bezpieczna publikacja

Obiekt utworzony w jednym wątku i przekazany innemu przez zwykłe pole może być widoczny **częściowo**: drugi wątek widzi referencję, ale jeszcze nie wartości pól ustawionych w konstruktorze. Bezpieczne sposoby przekazania obiektu:

- pole `static final` zainicjowane przy ładowaniu klasy,
- pole `volatile` albo `AtomicReference`,
- pole czytane i zapisywane pod tą samą blokadą,
- włożenie do kolekcji współbieżnej (`ConcurrentHashMap`, `BlockingQueue`),
- przekazanie przed `start()` albo przez `submit()`.

Pola **`final`** mają osobną gwarancję: jeśli konstruktor nie „wypuścił” `this` na zewnątrz (np. nie zarejestrował się jako listener), to każdy wątek, który zobaczy referencję do obiektu, zobaczy też poprawne wartości pól `final`. Dlatego obiekty niemutowalne są takie wygodne.

## Leniwa inicjalizacja (singleton)

```java
// ŹLE: check-then-act i brak widoczności
private static Usluga instancja;
static Usluga pobierz() {
    if (instancja == null) instancja = new Usluga();
    return instancja;
}
```

Poprawne warianty, od najprostszego:

```java
// 1. inicjalizacja przy ładowaniu klasy
private static final Usluga INSTANCJA = new Usluga();

// 2. holder: klasa wewnętrzna ładowana dopiero przy pierwszym użyciu (JVM gwarantuje bezpieczeństwo)
private static class Holder { static final Usluga INSTANCJA = new Usluga(); }
static Usluga pobierz() { return Holder.INSTANCJA; }

// 3. double-checked locking: TYLKO z volatile
private static volatile Usluga instancja;
static Usluga pobierz() {
    Usluga u = instancja;
    if (u == null) {
        synchronized (Usluga.class) {
            u = instancja;
            if (u == null) instancja = u = new Usluga();
        }
    }
    return u;
}
```

Bez `volatile` wariant 3 jest błędny: inny wątek może zobaczyć referencję do obiektu, którego konstruktor się jeszcze „nie skończył” z punktu widzenia pamięci.

## Pytania sprawdzające

1. Dlaczego pętla `while (!stop)` może się nie zatrzymać, choć inny wątek ustawił `stop = true`?
2. Co gwarantuje `volatile`? Dlaczego `volatile int licznik; licznik++` dalej jest błędem?
3. Wymień cztery sposoby utworzenia relacji happens-before między dwoma wątkami.
4. Wątek zapisuje wynik do zwykłego pola i się kończy. Główny wątek robi `join()` i czyta pole. Czy to poprawne? Dlaczego?
5. Dlaczego double-checked locking bez `volatile` jest błędny?
6. Dlaczego obiekty niemutowalne z polami `final` można przekazywać między wątkami bez synchronizacji?
