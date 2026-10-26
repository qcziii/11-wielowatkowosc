# Ściąga: wielowątkowość w Javie

## Podstawy

```java
Thread t = new Thread(() -> praca(), "nazwa");
t.start();                    // NIE t.run()
t.join();                     // czekaj na koniec + widoczność zapisów t
t.join(1000); t.isAlive();    // z limitem
t.interrupt();                // poproś o przerwanie
Thread.sleep(100);            // usypia BIEŻĄCY wątek
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
lock.lock();             // przed try
try { ... } finally { lock.unlock(); }

semafor.acquire();       // przed try
try { ... } finally { semafor.release(); }
```

## wait / notifyAll

```java
synchronized (this) {
    while (!warunek) wait();       // while, nie if
    ...
    notifyAll();                   // nie notify()
}
```

## InterruptedException

```java
catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // przywróć flagę
    return;                               // i zakończ (albo: throws InterruptedException)
}
```

Nigdy nie połykaj (pusty catch, sam log).

## ExecutorService

```java
try (ExecutorService pula = Executors.newFixedThreadPool(4)) {       // Java 19+: close() czeka
    Future<Integer> f = pula.submit(() -> policz());
    f.get();                         // ExecutionException → e.getCause() to oryginał
}
// starsze: shutdown(); if (!awaitTermination(...)) shutdownNow();
```

`submit` chowa wyjątek w `Future`: bez `get()` przepada.

## CompletableFuture

```java
var a = supplyAsync(() -> uslugaA(), executor);    // zawsze z executorem dla I/O
var b = supplyAsync(() -> uslugaB(), executor);
a.thenCombine(b, Wynik::new)                       // dwa równoległe
 .thenApply(w -> ...)                              // przekształć
 .thenCompose(w -> kolejnyAsync(w))                // kolejny krok async
 .completeOnTimeout(domyslna, 500, MILLISECONDS)
 .exceptionally(e -> zastepcza);                   // e często to CompletionException
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
