# 17. Projekt: przetwarzacz zamówień (producent i konsumenci)

Poziom: **trudny** · Rodzaj: **napisz** · Teoria: [08. Kolekcje współbieżne](../../teoria/08-kolekcje-wspolbiezne.md), [06. ExecutorService](../../teoria/06-executory.md)

To zadanie podsumowujące. Łączy to, co ćwiczyłeś wcześniej: kolejkę blokującą, wątki (albo executor), bezpieczne zbieranie wyników, obsługę wyjątków, przerwania i porządne kończenie pracy.

## Uruchomienie

```bash
./cwiczenia/17-projekt-zamowienia/check.sh
```

## Zadanie

Sklep dostaje zamówienia strumieniem (`Iterator<Order>`, w prawdziwym systemie: plik albo kolejka komunikatów). Napisz `OrderProcessor.process`:

1. **Producent:** jeden wątek czyta zamówienia ze źródła i wkłada je do `BlockingQueue` o pojemności `queueCapacity`. Iterator nie jest bezpieczny dla wątków, czyta go tylko producent. Gdy kolejka jest pełna, producent czeka (nie wczytuje całego źródła do pamięci).
2. **Konsumenci:** `consumerCount` wątków wyjmuje zamówienia z kolejki i woła `handler.handle(order)`.
3. **Błędy:** wyjątek z `handle` to błąd tego jednego zamówienia. Liczy się w `Report.errors`, a konsument bierze kolejne zamówienie.
4. **Raport:** liczba obsłużonych bez błędu, liczba błędów i suma wartości (tylko udanych) w każdej kategorii.
5. **Koniec:** `process` wraca, gdy wszystkie zamówienia są obsłużone, a wszystkie wątki (producent i konsumenci) są zakończone. Konsumenci muszą się jakoś dowiedzieć, że więcej zamówień nie będzie.

Puste źródło daje `new Report(0, 0, Map.of())`.

Podpowiedzi (przeczytaj dopiero, gdy utkniesz):

<details>
<summary>Jak konsumenci mają się dowiedzieć, że to koniec?</summary>

Najprostszy sposób to „zatruta pigułka” (poison pill): po ostatnim zamówieniu producent wkłada do kolejki specjalny obiekt-znacznik, po jednym dla każdego konsumenta. Konsument, który go wyjmie, kończy pracę. Nie sprawdzaj „czy kolejka jest pusta”: kolejka może być pusta tylko dlatego, że producent jeszcze nie zdążył.
</details>

<details>
<summary>Jak bezpiecznie zbierać sumy z wielu konsumentów?</summary>

`ConcurrentHashMap.merge(category, value, BigDecimal::add)` dla sum i `AtomicInteger`/`LongAdder` dla liczników. Albo: każdy konsument liczy swój własny raport częściowy (bez współdzielenia), a na końcu łączysz je w jednym wątku.
</details>

**Pytania dodatkowe:**

1. Co się stanie z Twoim rozwiązaniem, jeśli iterator źródła rzuci wyjątek w połowie? Zaproponuj zachowanie i powiedz, jak byś je zaimplementował.
2. Jak zmieniłbyś rozwiązanie, żeby dało się je przerwać z zewnątrz (`Thread.interrupt()` na wątku, który woła `process`)?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`. Pokaż rozwiązanie prowadzącemu i opowiedz, jak działa.
