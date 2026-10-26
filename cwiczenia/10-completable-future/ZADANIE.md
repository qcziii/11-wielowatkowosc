# 10. Podsumowanie klienta z trzech usług naraz

Poziom: **średni/trudny** · Rodzaj: **napisz** · Teoria: [07. CompletableFuture](../../teoria/07-completablefuture.md)

## Uruchomienie

```bash
./cwiczenia/10-completable-future/check.sh
```

## Zadanie

Ekran klienta pokazuje podsumowanie otwartych zamówień. Dane są w trzech usługach (`Services`): dane klienta, zamówienia i rabat. Każda odpowiada po ok. 200 ms. Wołane po kolei dają 600 ms, a chcemy ok. 200 ms.

Napisz `SummaryService.summary(customerId)` na `CompletableFuture`:

1. metoda nie blokuje, zwraca `CompletableFuture<Summary>` od razu,
2. trzy usługi wołane równolegle, na executorze podanym w konstruktorze,
3. wynik składasz przez `Summary.of(customer, orders, discount)` (obliczenia są gotowe),
4. gdy usługa rabatów rzuci wyjątek **albo** nie odpowie w `DISCOUNT_TIMEOUT` (500 ms), rabat to `BigDecimal.ZERO`,
5. gdy klienta nie ma, future kończy się wyjątkowo, a `get()` rzuca `ExecutionException` z przyczyną `CustomerNotFoundException`.

Nie używaj `get()` ani `join()` wewnątrz `summary`.

**Pytania dodatkowe:**

1. Na jakich wątkach działa `supplyAsync(...)` bez executora? Dlaczego w kodzie, który czeka na sieć, to zły pomysł?
2. Czym się różnią `thenApply`, `thenCompose` i `thenCombine`? Którego użyłeś i dlaczego?
3. Gdy `whenComplete`/`handle`/`exceptionally` dostaje wyjątek, czasem jest to `CompletionException`. Kiedy i dlaczego?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
