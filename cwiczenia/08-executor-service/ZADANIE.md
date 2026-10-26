# 08. Przetwarzanie listy na puli wątków

Poziom: **średni** · Rodzaj: **napisz** · Teoria: [06. ExecutorService i Future](../../teoria/06-executory.md)

## Uruchomienie

```bash
./cwiczenia/08-executor-service/check.sh
```

## Zadanie

Napisz `ParallelProcessor.process(data, function, threadCount)`: wywołaj funkcję dla każdego elementu listy na puli `threadCount` wątków i zwróć listę wyników.

Wymagania (każde sprawdza osobny test):

1. wyniki są w kolejności danych wejściowych, choć zadania kończą się w innej kolejności,
2. naraz działa co najwyżej `threadCount` wywołań, i rzeczywiście tyle (nie jeden),
3. po powrocie z metody nie zostaje żaden żywy wątek puli,
4. gdy funkcja rzuci wyjątek, metoda rzuca `ProcessingException`, a jej `getCause()` to **oryginalny** wyjątek z funkcji (nie `ExecutionException`); pula ma być zamknięta także w tym przypadku,
5. pusta lista daje pustą listę.

Nie twórz wątków ręcznie (`new Thread`). Nie używaj `parallelStream()`: nie da się nim ustawić liczby wątków w prosty sposób.

**Pytania dodatkowe:**

1. Co się dzieje z programem, jeśli zapomnisz zamknąć pulę z `Executors.newFixedThreadPool`?
2. Czym się różni `shutdown()` od `shutdownNow()`, i co robi `close()` (Java 19+)?
3. Dlaczego `Future.get()` opakowuje wyjątek w `ExecutionException`?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
