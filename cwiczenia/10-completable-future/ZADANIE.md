# 10. Podsumowanie klienta z trzech usług naraz

Poziom: **średni/trudny** · Rodzaj: **napisz** · Teoria: [07. CompletableFuture](../../teoria/07-completablefuture.md)

## Uruchomienie

```bash
./cwiczenia/10-completable-future/check.sh
```

## Zadanie

Ekran klienta pokazuje podsumowanie otwartych zamówień. Dane są w trzech usługach (`Uslugi`): dane klienta, zamówienia i rabat. Każda odpowiada po ok. 200 ms. Wołane po kolei dają 600 ms, a chcemy ok. 200 ms.

Napisz `SerwisPodsumowan.podsumowanie(klientId)` na `CompletableFuture`:

1. metoda nie blokuje, zwraca `CompletableFuture<Podsumowanie>` od razu,
2. trzy usługi wołane równolegle, na executorze podanym w konstruktorze,
3. wynik składasz przez `Podsumowanie.z(klient, zamowienia, rabat)` (obliczenia są gotowe),
4. gdy usługa rabatów rzuci wyjątek **albo** nie odpowie w `LIMIT_RABATU` (500 ms), rabat to `BigDecimal.ZERO`,
5. gdy klienta nie ma, future kończy się wyjątkowo, a `get()` rzuca `ExecutionException` z przyczyną `KlientNieZnalezionyException`.

Nie używaj `get()` ani `join()` wewnątrz `podsumowanie`.

**Pytania dodatkowe:**

1. Na jakich wątkach działa `supplyAsync(...)` bez executora? Dlaczego w kodzie, który czeka na sieć, to zły pomysł?
2. Czym się różnią `thenApply`, `thenCompose` i `thenCombine`? Którego użyłeś i dlaczego?
3. Gdy `whenComplete`/`handle`/`exceptionally` dostaje wyjątek, czasem jest to `CompletionException`. Kiedy i dlaczego?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
