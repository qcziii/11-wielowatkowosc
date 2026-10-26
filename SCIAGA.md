# Ściąga: wielowątkowość w Javie

## Podstawy

```java
Thread t = new Thread(() -> work(), "name");
t.start();                    // NOT t.run()
t.join();                     // wait for t to finish + see its writes
t.join(1000); t.isAlive();    // with a timeout
t.interrupt();                // ask it to stop (interrupt)
Thread.sleep(100);            // puts the CURRENT thread to sleep
Thread.currentThread().getName()
```

## Wspólne dane: co wybrać

| Sytuacja | Narzędzie |
|---|---|
| licznik, flaga, jedna liczba | `AtomicInteger`/`AtomicLong`/`AtomicBoolean`, `LongAdder` |
| flaga stop ustawiana z innego wątku | `volatile boolean` albo `interrupt()` |
| kilka pól zmienianych razem, check-then-act | `synchronized` / `ReentrantLock` |
| mapa | `ConcurrentHashMap` + `merge` / `compute` / `computeIfAbsent` |
| lista z rzadkimi zmianami | `CopyOnWriteArrayList` |
| producent-konsument | `ArrayBlockingQueue` (`put`/`take`) |
| „czekaj, aż N skończy” | `CountDownLatch` |
| „najwyżej N naraz” | `Semaphore` |
| dużo odczytów, rzadkie zapisy | `ReentrantReadWriteLock` albo niemutowalny obiekt w `volatile` |
| daty | `DateTimeFormatter`, `LocalDateTime` (nie `SimpleDateFormat`) |

## volatile a synchronized

| | `volatile` | `synchronized` / atomowe |
|---|---|---|
| widoczność | tak | tak |
| atomowość `x++`, check-then-act | **nie** | tak |

## Wzorce z finally

```java
lock.lock();             // before try
try { ... } finally { lock.unlock(); }

semafor.acquire();       // before try
try { ... } finally { semafor.release(); }
```

## wait / notifyAll

```java
synchronized (this) {
    while (!condition) wait();       // while, not if
    ...
    notifyAll();                   // not notify()
}
```

## InterruptedException

```java
catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // restore the flag
    return;                               // and finish (or: throws InterruptedException)
}
```

Nigdy nie połykaj (pusty catch, sam log).

## ExecutorService

```java
try (ExecutorService pool = Executors.newFixedThreadPool(4)) {       // Java 19+: close() waits
    Future<Integer> f = pool.submit(() -> count());
    f.get();                         // ExecutionException → e.getCause() is the original
}
// older style: shutdown(); if (!awaitTermination(...)) shutdownNow();
```

`submit` chowa wyjątek w `Future`: bez `get()` przepada.

## CompletableFuture

```java
var a = supplyAsync(() -> serviceA(), executor);    // always with an executor for I/O
var b = supplyAsync(() -> serviceB(), executor);
a.thenCombine(b, Result::new)                       // two in parallel
 .thenApply(w -> ...)                              // transform
 .thenCompose(w -> nextAsync(w))                // next async step
 .completeOnTimeout(defaultValue, 500, MILLISECONDS)
 .exceptionally(e -> fallback);                   // e is often a CompletionException
```

`get()` → `ExecutionException`, `join()` → `CompletionException`.

## Zakleszczenie

Cztery warunki: wyłączność, trzymaj-i-czekaj, brak wywłaszczenia, cykl. Lekarstwo: **stała kolejność blokad** (np. po id), `tryLock` z limitem, nie wołać obcego kodu pod blokadą.

Diagnoza: `jps -l`, `jcmd <PID> Thread.print` (albo `jstack`), w IntelliJ „Get Thread Dump”. Szukaj `Found one Java-level deadlock`.

## Wątki wirtualne (Java 21+)

```java
try (var ex = Executors.newVirtualThreadPerTaskExecutor()) { ex.submit(...); }
Thread.ofVirtual().start(r);
```

Dla zadań, które **czekają** (I/O). Nie przyspieszają obliczeń. Nie robi się z nich puli; limit równoległości przez `Semaphore`.

## Klasy niebezpieczne przy współdzieleniu

`ArrayList`, `HashMap`, `HashSet`, `SimpleDateFormat`, `DecimalFormat`, `Calendar`, `StringBuilder`, `MessageDigest`.
