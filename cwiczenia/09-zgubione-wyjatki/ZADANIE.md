# 09. Import cennika nie zgłasza błędów i gubi wiersze

Poziom: **średni** (2 błędy) · Rodzaj: **napraw** · Teoria: [06. ExecutorService i Future](../../teoria/06-executory.md)

## Uruchomienie

```bash
./cwiczenia/09-zgubione-wyjatki/check.sh
```

## Zgłoszenie

Import cennika z CSV przetwarza wiersze równolegle na puli 4 wątków. Dwa problemy:

1. Plik miał kilka złych wierszy (cena z przecinkiem, brak ceny, negative price), a raport importu mówi „0 błędów”. W logach też nic nie ma.
2. Na produkcji, gdzie zapis do bazy trwa dłużej, raport pokazuje mniej zaimportowanych wierszy, niż było w pliku, choć wszystkie wiersze były poprawne. Część cen pojawia się w bazie chwilę *po* zakończeniu importu.

**Oczekiwane:**

- `importAll` wraca dopiero wtedy, gdy wszystkie wiersze są przetworzone, niezależnie od tego, jak długo trwa zapis i ile wierszy jest w pliku,
- każdy błędny wiersz jest w `errors()` jako `"line N: opis"` (N od 1, w kolejności wierszy w pliku), gdzie opis to komunikat wyjątku,
- poprawne wiersze są zapisane, a `imported()` się zgadza.

**Pytania dodatkowe:**

1. Gdzie podział się wyjątek z zadania przekazanego do `submit`? Co by się stało, gdyby to było `execute` zamiast `submit`?
2. Co zwraca `awaitTermination` i dlaczego nie wolno tego ignorować?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
