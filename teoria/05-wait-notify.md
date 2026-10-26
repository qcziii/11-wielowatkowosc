# 05. Czekanie na warunek: wait() i notifyAll()

Ćwiczenia: [07](../cwiczenia/07-bufor-wait-notify/ZADANIE.md)

## Problem

Konsument chce wyjąć element z bufora, ale bufor jest pusty. Co ma zrobić?

```java
// ŹLE: aktywne czekanie (busy waiting) zjada cały rdzeń procesora
while (bufor.isEmpty()) { }
```

Potrzebujemy sposobu, żeby wątek **zasnął**, dopóki warunek nie stanie się prawdziwy, i żeby inny wątek mógł go obudzić. W Javie na najniższym poziomie służą do tego metody klasy `Object`: `wait()`, `notify()` i `notifyAll()`.

## Zasady

```java
synchronized (monitor) {
    while (!warunek) {
        monitor.wait();          // 1. zwalnia blokadę monitora, 2. zasypia, 3. po obudzeniu znowu bierze blokadę
    }
    // tu warunek jest prawdziwy, a my trzymamy blokadę
    zrobCos();
}

// w innym wątku
synchronized (monitor) {
    zmienStan();                 // warunek mógł się zmienić
    monitor.notifyAll();         // obudź wszystkich czekających na tym monitorze
}
```

1. **`wait()` i `notify()` woła się tylko, trzymając blokadę tego samego obiektu.** Inaczej `IllegalMonitorStateException`. W metodzie `synchronized` to `this`, więc piszesz po prostu `wait()` i `notifyAll()`.
2. **`wait()` zwalnia blokadę** na czas spania. Dzięki temu inny wątek może wejść do `synchronized` i zmienić stan.
3. Po obudzeniu wątek **musi znowu zdobyć blokadę**, zanim wróci z `wait()`. Między `notifyAll()` a jego powrotem inne wątki mogą zmienić stan.
4. **Warunek zawsze w pętli `while`, nigdy w `if`.**
5. Domyślnie **`notifyAll()`**, nie `notify()`.
6. `wait()` rzuca `InterruptedException` (rozdział 04).

### Dlaczego `while`, a nie `if`

Trzy powody:

- **Inny wątek był szybszy.** Dwóch konsumentów czeka na pustym buforze. Producent wkłada jeden element i woła `notifyAll()`. Budzą się obaj. Pierwszy bierze element. Drugi wychodzi z `wait()`, a z `if` nie sprawdza już warunku i robi `removeFirst()` na pustej kolejce (wyjątek albo `null`).
- **Wspólny monitor dla różnych warunków.** W buforze na tym samym obiekcie czekają producenci („niepełny”) i konsumenci („niepusty”). `notifyAll()` po włożeniu budzi też producentów, dla których nic się nie zmieniło.
- **Fałszywe przebudzenia** (*spurious wakeups*): specyfikacja pozwala, żeby `wait()` wróciło bez żadnego `notify`. Rzadkie, ale dozwolone.

Pętla `while` obsługuje wszystkie trzy przypadki: po obudzeniu sprawdzasz warunek jeszcze raz i w razie czego zasypiasz dalej.

### Dlaczego `notifyAll()`, a nie `notify()`

`notify()` budzi **jeden, dowolny** wątek czekający na monitorze. Jeśli na tym samym monitorze czekają wątki z różnymi warunkami (producenci i konsumenci), `notify()` może obudzić „złego”:

```
Bufor o pojemności 1, pełny. Czekają: P2 (producent), K1 i K2 (konsumenci, wcześniej było pusto).
K1 bierze element, notify() budzi... P2. Dobrze.
P2 wkłada element, notify() budzi... P1, innego producenta, który właśnie zaczął czekać. Źle.
P1 widzi pełny bufor, zasypia. Nikt nie budzi konsumentów. Wszyscy śpią: zawieszenie.
```

`notify()` jest poprawne tylko, gdy wszyscy czekający czekają na ten sam warunek i każdemu wystarczy jedno obudzenie. W razie wątpliwości `notifyAll()`: kosztuje trochę więcej przebudzeń, ale jest poprawne.

## Przykład: bramka startowa

```java
class Bramka {
    private boolean otwarta;

    synchronized void czekajNaOtwarcie() throws InterruptedException {
        while (!otwarta) {
            wait();
        }
    }

    synchronized void otworz() {
        otwarta = true;
        notifyAll();
    }
}
```

## W praktyce

`wait`/`notify` to klocki z samego dołu. W prawdziwym kodzie prawie zawsze wystarczy gotowa klasa z `java.util.concurrent`, która robi to samo, ale poprawnie i czytelniej:

| Zamiast pisać sam | Użyj |
|---|---|
| bufor producent-konsument | `ArrayBlockingQueue`, `LinkedBlockingQueue` (rozdział 08) |
| bramka, „poczekaj aż N rzeczy się skończy” | `CountDownLatch` (rozdział 09) |
| limit N naraz | `Semaphore` |
| czekanie na wynik innego wątku | `Future`, `CompletableFuture` |
| kilka warunków na jednej blokadzie | `ReentrantLock` + osobne `Condition` (`niePelny`, `niePusty`) |

Mimo to `wait`/`notifyAll` warto raz napisać samemu: wtedy rozumiesz, co robią te klasy w środku i dlaczego mają takie zasady.

## Pytania sprawdzające

1. Dlaczego `wait()` musi być wołane w bloku `synchronized` na tym samym obiekcie?
2. Co robi `wait()` z blokadą, którą wątek trzyma?
3. Opisz przeplot, w którym `if` zamiast `while` przed `wait()` kończy się błędem.
4. Kiedy `notify()` może zawiesić program, a `notifyAll()` nie?
5. Co to jest fałszywe przebudzenie?
6. Jakiej klasy z `java.util.concurrent` użyłbyś zamiast własnego bufora na `wait`/`notifyAll`?
