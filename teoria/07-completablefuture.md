# 07. CompletableFuture

Ćwiczenia: [10](../cwiczenia/10-completable-future/ZADANIE.md)

## Po co, skoro jest Future

Ze zwykłym `Future` jedyne, co możesz zrobić z wynikiem, to **poczekać** na niego (`get()`) i zablokować wątek. `CompletableFuture` pozwala powiedzieć: „gdy wynik będzie gotowy, zrób z nim to i to”, bez blokowania. Składasz z niego łańcuchy i łączysz kilka asynchronicznych operacji w jedną.

## Uruchamianie

```java
CompletableFuture<Klient> klient = CompletableFuture.supplyAsync(() -> klienci.pobierz(id), executor);
CompletableFuture<Void> zapis = CompletableFuture.runAsync(() -> log.zapisz(...), executor);
CompletableFuture<String> gotowy = CompletableFuture.completedFuture("od razu");
```

**Zawsze podawaj executor**, gdy zadanie czeka na sieć, bazę czy dysk. Bez niego zadania idą na `ForkJoinPool.commonPool()`: wspólną pulę całej JVM, z liczbą wątków równą liczbie rdzeni minus jeden. Kilka wolnych wywołań HTTP potrafi ją zapchać, a z nią wszystkie `parallelStream()` w aplikacji.

## Przekształcanie i łączenie

| Metoda | Kiedy | Przykład |
|---|---|---|
| `thenApply(f)` | przekształć wynik (funkcja zwraca zwykłą wartość) | `klient.thenApply(Klient::nazwa)` |
| `thenAccept(c)` | zrób coś z wynikiem, nic nie zwracaj | `f.thenAccept(System.out::println)` |
| `thenCompose(f)` | następny krok też jest asynchroniczny (funkcja zwraca `CompletableFuture`) | `klient.thenCompose(k -> zamowienia(k.id()))` |
| `thenCombine(inny, f)` | połącz dwa **niezależne**, równoległe wyniki | `cena.thenCombine(kurs, (c, k) -> c * k)` |
| `allOf(f1, f2, ...)` | poczekaj na wszystkie (wynik `Void`) | potem `f1.join()` w środku `thenApply` już nie blokuje |
| `anyOf(...)` | pierwszy gotowy | |

`thenApply` z funkcją, która zwraca `CompletableFuture`, da ci `CompletableFuture<CompletableFuture<X>>`. To znak, że potrzebujesz `thenCompose` (jak `map` i `flatMap` w strumieniach).

Równoległość bierze się z tego, że **uruchamiasz wszystko od razu**, a dopiero potem łączysz:

```java
// równolegle: oba wywołania startują w tej samej chwili
var a = CompletableFuture.supplyAsync(() -> uslugaA(), executor);
var b = CompletableFuture.supplyAsync(() -> uslugaB(), executor);
CompletableFuture<Wynik> wynik = a.thenCombine(b, Wynik::new);

// po kolei: B startuje dopiero, gdy A się skończy
CompletableFuture<Wynik> wynik = CompletableFuture.supplyAsync(() -> uslugaA(), executor)
        .thenCompose(x -> CompletableFuture.supplyAsync(() -> uslugaB(x), executor).thenApply(y -> new Wynik(x, y)));
```

Drugi wariant jest dobry tylko wtedy, gdy B naprawdę potrzebuje wyniku A.

Metody bez `Async` (`thenApply`) wykonują funkcję w wątku, który zakończył poprzedni krok (albo w wątku wołającym, jeśli wynik już był gotowy). Wersje `...Async(f, executor)` przenoszą ją na podany executor. Dla krótkich przekształceń bez `Async` jest w porządku.

## Błędy

Wyjątek w dowolnym kroku kończy future **wyjątkowo**, a kolejne `thenApply`/`thenCombine` są pomijane: błąd płynie łańcuchem w dół jak w `try`.

| Metoda | Co robi |
|---|---|
| `exceptionally(e -> wartośćZastępcza)` | tylko przy błędzie: zamień błąd na wartość |
| `handle((wynik, e) -> ...)` | zawsze; dostaje wynik albo błąd (jedno z nich jest `null`) |
| `whenComplete((wynik, e) -> ...)` | zawsze, do efektów ubocznych (log); nie zmienia wyniku |
| `completeOnTimeout(wartość, czas, jedn.)` (Java 9+) | jeśli w czasie nie ma wyniku, użyj wartości |
| `orTimeout(czas, jedn.)` (Java 9+) | jeśli w czasie nie ma wyniku, zakończ `TimeoutException` |

```java
CompletableFuture<BigDecimal> rabat = CompletableFuture.supplyAsync(() -> rabaty.rabat(id), executor)
        .completeOnTimeout(BigDecimal.ZERO, 500, TimeUnit.MILLISECONDS)
        .exceptionally(e -> BigDecimal.ZERO);
```

`CompletionException`: wyjątek rzucony w `supplyAsync` albo w kroku łańcucha jest opakowany w `CompletionException`, zanim trafi do `exceptionally`/`handle` dalszych kroków. Oryginał jest w `getCause()`. Gdy czekasz na wynik:

- `get()` rzuca `ExecutionException`, w `getCause()` oryginalny wyjątek,
- `join()` rzuca `CompletionException` (niekontrolowany), w `getCause()` oryginał.

`completeOnTimeout` i `orTimeout` **nie przerywają** zadania, które dalej działa. Kończą tylko future. Wolne zadanie dalej zajmuje wątek executora, aż samo się skończy.

## Czego nie robić

- `get()`/`join()` w środku łańcucha (na future, który może jeszcze nie być gotowy): blokujesz wątek, cała zaleta znika, a w małej puli łatwo o zakleszczenie.
- Puste `exceptionally(e -> null)`: błąd znika, a dalej leci `NullPointerException` bez śladu przyczyny.
- Zapominanie o zwróconym future: jeśli nikt nie sprawdzi wyniku, wyjątek przepadnie tak samo jak przy `submit` (rozdział 06).

## Pytania sprawdzające

1. Czym się różni `thenApply` od `thenCompose`? Kiedy potrzebujesz `thenCombine`?
2. Na jakich wątkach działa `supplyAsync(supplier)` bez executora i dlaczego to bywa problemem?
3. Jak sprawić, żeby trzy wywołania szły równolegle, a nie po kolei?
4. Co rzuca `get()`, a co `join()`, gdy future skończył się błędem?
5. Czy `completeOnTimeout` zatrzymuje wolne zadanie?
