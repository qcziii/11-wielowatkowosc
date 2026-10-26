# 08. Kolekcje współbieżne i klasy niebezpieczne dla wątków

Ćwiczenia: [11](../cwiczenia/11-licznik-slow/ZADANIE.md), [16](../cwiczenia/16-simpledateformat/ZADANIE.md), [17](../cwiczenia/17-projekt-zamowienia/ZADANIE.md)

## Zwykłe kolekcje nie są bezpieczne

`ArrayList`, `HashMap`, `HashSet`, `ArrayDeque`, `LinkedList` zakładają jeden wątek naraz. Równoległe zapisy dają: zgubione elementy, `ArrayIndexOutOfBoundsException` w `ArrayList.add`, `null` w środku listy, a w `HashMap` uszkodzoną strukturę (w starych wersjach Javy nawet nieskończoną pętlę). Iteracja w czasie, gdy inny wątek zmienia kolekcję, zwykle kończy się `ConcurrentModificationException`, ale **nie jest to gwarantowane**: czasem po prostu dostaniesz złe dane.

## Możliwości

| Potrzeba | Klasa |
|---|---|
| mapa dla wielu wątków | `ConcurrentHashMap` |
| posortowana mapa / zbiór | `ConcurrentSkipListMap`, `ConcurrentSkipListSet` |
| zbiór | `ConcurrentHashMap.newKeySet()` |
| lista, dużo odczytów, mało zapisów (np. listenery) | `CopyOnWriteArrayList` |
| kolejka bez blokowania | `ConcurrentLinkedQueue` |
| kolejka producent-konsument | `ArrayBlockingQueue` (ograniczona), `LinkedBlockingQueue` |
| opakowanie istniejącej kolekcji | `Collections.synchronizedList(...)`, `synchronizedMap(...)` |

## ConcurrentHashMap: bezpieczna, ale tylko pojedyncze operacje

„Thread-safe” znaczy: każda **pojedyncza** operacja (`get`, `put`, `remove`) jest atomowa, a struktura mapy nigdy się nie zepsuje. **Nie** znaczy, że ciąg operacji jest atomowy:

```java
// WRONG: read-modify-write, loses increments despite ConcurrentHashMap
Long count = map.get(word);
map.put(word, count == null ? 1 : count + 1);

// WRONG: check-then-act
if (!map.containsKey(k)) {
    map.put(k, create());
}
```

Do operacji złożonych `ConcurrentHashMap` ma metody, które robią je atomowo dla danego klucza:

```java
map.merge(word, 1L, Long::sum);                     // counter
map.computeIfAbsent(k, key -> new ArrayList<>());  // create if absent (function called at most once)
map.compute(k, (key, old) -> ...);               // any change
map.putIfAbsent(k, v);
map.replace(k, oldValue, newValue);                  // CAS on the value
```

Funkcje przekazane do `compute*`/`merge` muszą być **krótkie** i nie mogą zmieniać tej samej mapy (mapa w tym czasie blokuje fragment tablicy).

Dla liczników przy dużym ruchu: `ConcurrentHashMap<String, LongAdder>` i `map.computeIfAbsent(k, x -> new LongAdder()).increment()`.

Inne fakty:

- `ConcurrentHashMap` nie przyjmuje `null` ani jako klucza, ani jako wartości (wtedy `get` zwracające `null` jest jednoznaczne).
- Iteratory nie rzucają `ConcurrentModificationException`; pokazują stan „gdzieś z czasu iteracji” (*weakly consistent*).
- `size()` przy równoczesnych zmianach jest tylko przybliżeniem.

## Collections.synchronizedXxx

Opakowanie, w którym każda metoda jest `synchronized`. Dwie pułapki:

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

// 1. iteration must be done manually under the list's lock
synchronized (list) {
    for (String s : list) { ... }
}

// 2. so must compound operations
synchronized (list) {
    if (!list.contains(x)) list.add(x);
}
```

## BlockingQueue: producent i konsument

Kolejka, która **czeka**: `put` czeka, gdy (ograniczona) kolejka jest pełna, `take` czeka, gdy jest pusta. To gotowy bufor z rozdziału 05.

| | Rzuca wyjątek | Zwraca wartość specjalną | Czeka | Czeka z limitem |
|---|---|---|---|---|
| włóż | `add(e)` | `offer(e)` → `false` | `put(e)` | `offer(e, time, unit)` |
| wyjmij | `remove()` | `poll()` → `null` | `take()` | `poll(time, unit)` |

Pułapka: `offer` na pełnej kolejce **po cichu nie wkłada** elementu (zwraca `false`). Jeśli tego nie sprawdzisz, element przepada.

Wzorzec producent-konsument:

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);   // bounded: the producer cannot flood memory

// producer
for (Task t : source) queue.put(t);
for (int i = 0; i < consumerCount; i++) queue.put(POISON_PILL);   // a "poison pill" for each consumer

// consumer
while (true) {
    Task t = queue.take();
    if (t == POISON_PILL) break;
    handle(t);
}
```

Nie kończ konsumenta warunkiem „kolejka pusta”: może być pusta tylko na chwilę, bo producent jeszcze nie zdążył.

## Klasy z JDK niebezpieczne przy współdzieleniu

Nie tylko kolekcje. Wiele klas z JDK ma w środku zmienny stan i **nie może** być współdzielona między wątkami:

| Niebezpieczna | Bezpieczna alternatywa |
|---|---|
| `SimpleDateFormat`, `DateFormat`, `NumberFormat`, `DecimalFormat` | `DateTimeFormatter` (niemutowalny), osobny obiekt na wątek, `ThreadLocal` |
| `java.util.Calendar`, `Date` (mutowalna) | `LocalDate`, `LocalDateTime`, `Instant` (niemutowalne) |
| `StringBuilder` | lokalna zmienna w każdym wątku (`StringBuffer` jest synchronizowany, ale rzadko to ma sens) |
| `java.util.Random` | bezpieczny, ale wolny pod obciążeniem; lepszy `ThreadLocalRandom.current()` |
| `MessageDigest`, `Cipher` | osobny obiekt na wątek / na operację |
| `HashMap`, `ArrayList`, ... | patrz wyżej |

`SimpleDateFormat.parse()` wygląda jak czysta funkcja, ale w trakcie parsowania zapisuje wyniki pośrednie we własnym polu `Calendar`. Dwa wątki naraz nadpisują sobie te pola: wychodzą złe daty albo `NumberFormatException` z głębi biblioteki. Klasyczny błąd: `private static final SimpleDateFormat FORMAT`, a potem `parallelStream()` albo serwer WWW.

Zasada ogólna: zanim współdzielisz obiekt, sprawdź w javadocu, czy klasa jest *thread-safe*. Jeśli javadoc nic nie mówi, zakładaj, że nie jest.

## Pytania sprawdzające

1. Co znaczy, że `ConcurrentHashMap` jest thread-safe, a czego nie obiecuje?
2. Napisz licznik słów na `ConcurrentHashMap` bez `synchronized`.
3. Czym się różni `offer` od `put` w `BlockingQueue`? Kiedy `offer` gubi element?
4. Jak konsument ma się dowiedzieć, że producent skończył?
5. Dlaczego `static final SimpleDateFormat` używany z wielu wątków daje złe daty?
6. Jak bezpiecznie iterować po `Collections.synchronizedList`?
