# 12. Pula połączeń zawiesza się po kilku błędach

Poziom: **średni** · Rodzaj: **napraw** · Teoria: [09. Synchronizatory i blokady](../../teoria/09-synchronizatory-i-blokady.md)

## Uruchomienie

```bash
./cwiczenia/12-pula-polaczen/check.sh
```

## Zgłoszenie

`PulaPolaczen` pilnuje, żeby naraz było używane najwyżej N połączeń z bazą (`Semaphore` z N zezwoleniami). Działa, dopóki nie pojawią się błędy SQL. Po kilku nieudanych zapytaniach aplikacja przestaje odpowiadać: wszystkie wątki czekają na połączenie, choć baza jest bezczynna. Zrzut wątków pokazuje dziesiątki wątków w `Semaphore.acquire`.

**Oczekiwane:**

- naraz co najwyżej N operacji,
- wyjątek z operacji leci dalej do wywołującego, a pula zostaje sprawna,
- wątek przerwany w czasie czekania na połączenie dostaje `InterruptedException` i nie zmienia liczby wolnych połączeń.

Uwaga, trzeci punkt łatwo zepsuć przy naprawianiu pierwszego błędu.

**Pytanie dodatkowe:** Gdzie dokładnie powinno stać `acquire()` względem `try`, i dlaczego nie w środku `try`?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
