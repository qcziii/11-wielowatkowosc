# 16. Parser logów zwraca złe daty przy równoległym parsowaniu

Poziom: **łatwy** · Rodzaj: **napraw** · Teoria: [08. Kolekcje współbieżne i klasy niebezpieczne dla wątków](../../teoria/08-kolekcje-wspolbiezne.md)

## Uruchomienie

```bash
./cwiczenia/16-simpledateformat/check.sh
```

## Zgłoszenie

Parser logów działał dobrze, dopóki ktoś nie przyspieszył go zamianą `stream()` na `parallelStream()`. Teraz raz na jakiś czas leci `NumberFormatException` z głębi `SimpleDateFormat`, a co gorsza, część wpisów dostaje daty, których w pliku nie ma (rok 2206, 31 lutego, godzina sprzed minuty...). Bez `parallelStream()` wszystko działa, ale jest za wolno.

**Oczekiwane:** parsowanie zostaje równoległe, każda data jest poprawna, nie ma wyjątków.

**Pytania dodatkowe:**

1. Dlaczego jeden współdzielony `SimpleDateFormat` daje złe wyniki, skoro metoda `parse` nic „nie zapisuje”?
2. Podaj dwa rozwiązania: jedno z `java.time`, drugie bez zmiany klasy formatu. Które wybierasz i dlaczego?
3. Jakie inne klasy z JDK są niebezpieczne przy współdzieleniu między wątkami? Wymień przynajmniej trzy.

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
