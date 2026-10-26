# 14. Przelewy w obie strony zawieszają bank

Poziom: **średni** · Rodzaj: **napraw** · Teoria: [10. Zakleszczenia](../../teoria/10-zakleszczenia.md)

## Uruchomienie

```bash
./cwiczenia/14-zakleszczenie-przelewy/check.sh
```

## Zgłoszenie

`Bank.transfer` blokuje oba rachunki (`synchronized` na rachunku źródłowym i docelowym), żeby nikt w tym czasie nie zmienił ich sald. Testy jednowątkowe przechodzą. Na produkcji, mniej więcej raz dziennie, system przelewów staje: wątki nie zużywają procesora, nic nie robią, a nowe przelewy też stają. Pomaga tylko restart.

Zanim cokolwiek zmienisz, **zobacz problem na żywo**: uruchom test w IntelliJ, poczekaj, aż zawiśnie, i zrób zrzut wątków (w IntelliJ ikona aparatu „Get Thread Dump” w oknie Run/Debug, albo `jcmd`/`jstack` z terminala, opis w teorii). Znajdź w zrzucie słowa `Found one Java-level deadlock`.

**Oczekiwane:**

- przelewy z wielu wątków, w dowolnych kierunkach, nigdy się nie zawieszają,
- suma pieniędzy w banku się nie zmienia, saldo nie spada poniżej zera,
- przelew na ten sam rachunek działa (i nic nie zmienia).

Nie rób jednej globalnej blokady na cały bank: przelewy między niezależnymi rachunkami mają iść równolegle.

**Pytania dodatkowe:**

1. Jakie cztery warunki muszą być spełnione jednocześnie, żeby powstało zakleszczenie? Który z nich łamie Twoje rozwiązanie?
2. Jak to samo rozwiązać na `ReentrantLock.tryLock` z limitem czasu? Jaki nowy problem może się wtedy pojawić?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
