# 02. Suma liczona w kilku wątkach wychodzi za każdym razem inna

Poziom: **łatwy** (2 błędy) · Rodzaj: **napraw** · Teoria: [01. Wątki: podstawy](../../teoria/01-watki-podstawy.md)

## Uruchomienie

```bash
./cwiczenia/02-suma-bez-join/check.sh
```

## Zgłoszenie

`ParallelSum.sum` dzieli tablicę na tyle części, ile jest wątków, i każdą część sumuje w osobnym wątku. Dla tablicy `1, 2, ..., 8 000 000` raz wychodzi 0, raz jakaś połowa wyniku, a raz dobrze. Kolega dopisał w pętli `System.out.println` i „naprawiło się”, ale tylko u niego.

Po pierwszej poprawce zostaje drugi, mniej widoczny błąd: dla tablicy o długości 1 000 003 i 4 wątków wynik jest za mały, i to zawsze o tyle samo.

**Oczekiwane:** wynik jest zawsze poprawny, dla dowolnej długości tablicy i dowolnej liczby wątków od 1 w górę.

**Pytanie dodatkowe:** Dlaczego `System.out.println` w pętli potrafi „naprawić” pierwszy błąd? Czy to jest naprawa?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`. Uruchom go kilka razy.
