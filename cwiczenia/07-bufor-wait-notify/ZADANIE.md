# 07. Bufor ograniczony na wait() i notifyAll()

Poziom: **średni** · Rodzaj: **napisz** · Teoria: [05. wait i notifyAll](../../teoria/05-wait-notify.md)

## Uruchomienie

```bash
./cwiczenia/07-bufor-wait-notify/check.sh
```

## Zadanie

Uzupełnij `BuforOgraniczony<T>`: kolejkę o stałej pojemności, z której naraz korzysta wielu producentów i wielu konsumentów. To dokładnie to, co robi `ArrayBlockingQueue`, ale tu masz to napisać sam, na najniższym poziomie: `synchronized`, `wait()` i `notifyAll()`.

- `wloz(e)`: gdy bufor jest pełny, wątek czeka, aż zwolni się miejsce,
- `wez()`: gdy bufor jest pusty, wątek czeka, aż pojawi się element,
- `rozmiar()`: aktualna liczba elementów,
- oba czekające wywołania reagują na przerwanie (`InterruptedException`),
- kolejność FIFO.

Zakaz: nie używaj niczego z `java.util.concurrent` (sprawdza to `check.sh`).

Sprawdzanie warunku w pętli, wybór między `notify()` a `notifyAll()` i to, na jakim obiekcie wołasz `wait()`, to sedno tego zadania. Zanim zaczniesz, przeczytaj rozdział teorii.

**Pytania dodatkowe:**

1. Dlaczego warunek przed `wait()` musi być w `while`, a nie w `if`? Opisz konkretny przeplot dwóch konsumentów, który z `if` kończy się błędem.
2. Co może się stać, jeśli zamiast `notifyAll()` użyjesz `notify()`, przy kilku producentach i kilku konsumentach?
3. Co się stanie, gdy zawołasz `wait()` poza blokiem `synchronized`?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`. Uruchom go kilka razy: błędy w tym zadaniu potrafią wychodzić co trzecie uruchomienie.
