# 11. Wątki wirtualne (Java 21+)

Ćwiczenia: [15](../cwiczenia/15-wirtualne-watki/ZADANIE.md)

## Problem

Zwykły wątek Javy (*platformowy*) to wątek systemu operacyjnego: drogi w tworzeniu, z dużym stosem, a system obsłuży ich najwyżej kilka tysięcy. Typowa aplikacja serwerowa większość czasu **czeka**: na bazę, na inną usługę po HTTP, na dysk. Wątek, który czeka, nic nie liczy, ale dalej zajmuje cały wątek systemowy. Przy 10 000 równoczesnych żądań, każde czekające 100 ms na bazę, potrzebujesz 10 000 wątków.

Dotąd wyjściem było programowanie asynchroniczne (`CompletableFuture`, programowanie reaktywne): wydajne, ale trudne do pisania, czytania i debugowania.

## Wątki wirtualne

Wątek wirtualny (Java 21, JEP 444) to wątek Javy, którym zarządza JVM, a nie system. Działa „na” zwykłym wątku platformowym (*nośnym*, carrier), ale **gdy czeka** (`sleep`, I/O z gniazda, `BlockingQueue.take`, `Lock.lock`...), JVM zdejmuje go z nośnika i nośnik w tym czasie wykonuje inny wątek wirtualny. Gdy odpowiedź przyjdzie, wątek wirtualny wraca na któryś z nośników.

Skutek: możesz mieć **miliony** wątków wirtualnych, a kod piszesz zwyczajnie, blokująco, „jeden wątek na żądanie”.

```java
Thread t = Thread.ofVirtual().name("customer-", 0).start(() -> handle());
Thread.startVirtualThread(() -> handle());

try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
    for (String url : urls) {
        ex.submit(() -> get(url));     // a new virtual thread for EVERY task
    }
}   // close() waits for all of them

Thread.currentThread().isVirtual();
```

Wszystko, co znasz z poprzednich rozdziałów, działa tak samo: `synchronized`, `volatile`, `join`, `interrupt`, `Future`, kolekcje współbieżne. Wątki wirtualne nie zmieniają **zasad** współbieżności, tylko jej **koszt**. Wyścigi i zakleszczenia są dokładnie takie same.

## Kiedy pomagają, a kiedy nie

| Zadanie | Wątki wirtualne |
|---|---|
| dużo równoczesnych zadań, które głównie czekają (HTTP, baza, pliki, kolejki) | tak, to ich główne zastosowanie |
| obliczenia, które cały czas zajmują procesor (kompresja, skróty, grafika) | nie przyspieszą: nośników jest tyle, ile rdzeni. Użyj puli platformowej o rozmiarze ≈ liczba rdzeni |
| kilka długo działających wątków w tle | bez znaczenia |

## Zasady

**Nie wkładaj wątków wirtualnych do puli.** Pula istnieje dlatego, że wątki są drogie; wirtualne są tanie. Jeden wątek wirtualny na zadanie, tworzony i porzucany.

**Do ograniczenia równoległości użyj semafora, nie rozmiaru puli.** Gdy API pozwala na 100 równoczesnych żądań:

```java
Semaphore limit = new Semaphore(100);
try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
    for (String p : products) {
        ex.submit(() -> {
            limit.acquire();
            try {
                return api.price(p);
            } finally {
                limit.release();
            }
        });
    }
}
```

**Ostrożnie z `ThreadLocal`:** przy milionach wątków każda kopia wartości to pamięć. Do przekazywania kontekstu (np. id żądania) w Javie 25 jest `ScopedValue`.

**Przypinanie (pinning).** Wątek wirtualny jest „przypięty” do nośnika, gdy nie da się go zdjąć w czasie czekania. Wtedy blokuje cały wątek nośny, jak zwykły wątek platformowy.

- Java 21-23: przypina **czekanie wewnątrz `synchronized`** (np. `sleep` albo zapytanie HTTP w metodzie `synchronized`) i w `Object.wait()`. Przy kilku takich miejscach wszystkie nośniki mogą zostać zajęte i aplikacja staje. Zalecenie dla tych wersji: w miejscach z długim czekaniem zamień `synchronized` na `ReentrantLock`. Diagnoza: `-Djdk.tracePinnedThreads=full`.
- Java 24+ (JEP 491): `synchronized` i `wait()` już **nie** przypinają. Przypina nadal kod natywny (JNI, Foreign Function API).

## Pytania sprawdzające

1. Co się dzieje z wątkiem wirtualnym, gdy czeka na odpowiedź z sieci?
2. Czy wątki wirtualne przyspieszą liczenie skrótów SHA-256 dla miliona plików w pamięci? Dlaczego?
3. Dlaczego nie robi się puli wątków wirtualnych? Jak ograniczyć liczbę równoczesnych żądań?
4. Czy wątki wirtualne usuwają wyścigi i potrzebę `synchronized`?
5. Co to jest przypinanie i co w tej sprawie zmieniła Java 24?
