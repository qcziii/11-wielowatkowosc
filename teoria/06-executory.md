# 06. ExecutorService, Callable i Future

Ćwiczenia: [08](../cwiczenia/08-executor-service/ZADANIE.md), [09](../cwiczenia/09-zgubione-wyjatki/ZADANIE.md), [17](../cwiczenia/17-projekt-zamowienia/ZADANIE.md)

## Po co pula wątków

Tworzenie wątku platformowego jest drogie (wątek systemu operacyjnego, stos rzędu 1 MB pamięci wirtualnej). Gdyby każde żądanie HTTP tworzyło nowy wątek, przy dużym ruchu serwer by się udusił. **Pula wątków** (`ExecutorService`) trzyma kilka wątków, które biorą zadania z kolejki i wykonują je po kolei. Oddzielamy *co* zrobić (zadanie) od *jak i gdzie* to wykonać (pula).

## Tworzenie puli

```java
ExecutorService pool = Executors.newFixedThreadPool(4);            // fixed 4 threads, unbounded queue
ExecutorService pool = Executors.newSingleThreadExecutor();         // 1 thread: tasks one after another
ExecutorService pool = Executors.newCachedThreadPool();             // creates threads as needed (careful: no limit)
ScheduledExecutorService s = Executors.newScheduledThreadPool(2);   // delayed / periodic tasks
ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor(); // Java 21: one virtual thread per task (chapter 11)
```

Ile wątków? Dla obliczeń (CPU) mniej więcej liczba rdzeni (`Runtime.getRuntime().availableProcessors()`). Dla zadań, które głównie czekają (sieć, baza), więcej, albo od razu wątki wirtualne.

## Runnable, Callable, Future

```java
// Runnable: returns nothing, cannot throw a checked exception
pool.execute(() -> System.out.println("hi"));

// Callable<T>: returns a result and may throw exceptions
Callable<Integer> task = () -> count();
Future<Integer> future = pool.submit(task);

// ... do something else in the meantime ...

Integer result = future.get();               // waits for the result (blocks)
Integer result = future.get(2, TimeUnit.SECONDS);   // waits up to 2 s, then TimeoutException
```

`Future` to „pokwitowanie” za zadanie:

| Metoda | Co robi |
|---|---|
| `get()` | czeka na koniec i zwraca wynik; jeśli zadanie rzuciło wyjątek: `ExecutionException` z tym wyjątkiem w `getCause()` |
| `get(time, unit)` | jak wyżej, z limitem; po limicie `TimeoutException` (zadanie dalej działa!) |
| `isDone()` | czy zadanie się skończyło (normalnie, wyjątkiem albo anulowane) |
| `cancel(true)` | anuluje; `true` = przerwij wątek, jeśli zadanie już działa (rozdział 04) |

Wyniki wielu zadań w kolejności zgłoszenia: trzymaj listę `Future` i wołaj `get()` po kolei. Albo `invokeAll(callables)`, które zwraca listę `Future` w kolejności zadań i czeka na wszystkie.

## Gdzie znikają wyjątki

```java
pool.submit(() -> { throw new IllegalStateException("bum"); });   // silence
pool.execute(() -> { throw new IllegalStateException("bum"); });  // stack trace on System.err, the pool thread dies and is replaced
```

`submit` łapie każdy wyjątek z zadania i chowa go w zwróconym `Future`. **Jeśli nikt nie zawoła `get()`, wyjątek przepada bez śladu.** To częste źródło błędów typu „import mówi, że wszystko OK, a połowy danych nie ma”.

Zasada: jeśli używasz `submit`, zawsze zrób coś z `Future`. `get()` rzuca `ExecutionException`, a oryginalny wyjątek jest w `e.getCause()`:

```java
try {
    future.get();
} catch (ExecutionException e) {
    Throwable rootCause = e.getCause();
    ...
}
```

## Zamykanie puli

Wątki puli nie są demonami. **Niezamknięta pula trzyma JVM przy życiu** i zjada zasoby (przy puli tworzonej w każdym wywołaniu metody: wyciek wątków).

| Metoda | Co robi |
|---|---|
| `shutdown()` | nie przyjmuje nowych zadań; zgłoszone (także czekające w kolejce) wykonają się do końca. **Nie czeka**, wraca od razu |
| `shutdownNow()` | nie przyjmuje nowych; czekające w kolejce wyrzuca (zwraca ich listę), działające **przerywa** (`interrupt`) |
| `awaitTermination(time, unit)` | czeka, aż wszystkie zadania się skończą, maksymalnie podany czas. Zwraca `true`, jeśli się skończyły, `false`, jeśli minął czas |
| `close()` (Java 19+) | `shutdown()` i czekanie bez limitu na koniec wszystkich zadań. `ExecutorService` jest `AutoCloseable` |

Klasyczny wzorzec (Java 8-17):

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
try {
    ... submit ...
} finally {
    pool.shutdown();
    if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
        pool.shutdownNow();
    }
}
```

Od Javy 19 najprościej:

```java
try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
    ... submit ...
}   // close(): shutdown + wait for all tasks
```

Pułapka: wynik `awaitTermination` **trzeba sprawdzić**. Jeśli zignorujesz `false`, kod idzie dalej, choć zadania dalej działają (ćwiczenie 09).

Długo żyjąca pula (np. jedna na całą aplikację, jako pole albo bean) jest w porządku, ale wtedy zamyka się ją przy zamykaniu aplikacji, a nie po każdym użyciu.

## Pułapki

- **Nieograniczona kolejka:** `newFixedThreadPool` ma kolejkę bez limitu. Jeśli zadania przychodzą szybciej, niż się wykonują, kolejka rośnie aż do `OutOfMemoryError`. Gdy to możliwe, użyj `new ThreadPoolExecutor(...)` z ograniczoną kolejką i polityką odrzucania.
- **Zadanie czeka na inne zadanie z tej samej puli:** w puli 1-wątkowej zadanie A robi `submit(B).get()`. B nigdy nie ruszy, bo jedyny wątek czeka na B. To zakleszczenie przez brak wątków (*thread starvation deadlock*).
- **`ThreadLocal` w puli:** wątki są używane wielokrotnie, więc wartość z poprzedniego zadania może „przeciec” do następnego. Czyść w `finally`.
- **Nazwy wątków:** `pool-3-thread-7` nic nie mówi w logach. Własna `ThreadFactory` (albo `Thread.ofPlatform().name("import-", 0).factory()`) z nazwami pomaga przy diagnozie.

## Pytania sprawdzające

1. Czym się różni `execute` od `submit`? Co się dzieje z wyjątkiem w każdym z nich?
2. Co rzuca `Future.get()`, gdy zadanie rzuciło wyjątek, i jak dostać się do oryginalnego wyjątku?
3. Czym się różni `shutdown()` od `shutdownNow()`? Czy `shutdown()` czeka na zakończenie zadań?
4. Co zwraca `awaitTermination` i co zrobić, gdy zwróci `false`?
5. Co się stanie z programem, który utworzył `newFixedThreadPool(4)` i nigdy jej nie zamknął?
6. Dlaczego w puli z jednym wątkiem `submit(...).get()` wewnątrz zadania może zawiesić program?
