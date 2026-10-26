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
// ŹLE: read-modify-write, gubi zwiększenia mimo ConcurrentHashMap
Long ile = mapa.get(slowo);
mapa.put(slowo, ile == null ? 1 : ile + 1);

// ŹLE: check-then-act
if (!mapa.containsKey(k)) {
    mapa.put(k, stworz());
}
```

Do operacji złożonych `ConcurrentHashMap` ma metody, które robią je atomowo dla danego klucza:

```java
mapa.merge(slowo, 1L, Long::sum);                     // licznik
mapa.computeIfAbsent(k, klucz -> new ArrayList<>());  // utwórz, jeśli nie ma (funkcja wołana najwyżej raz)
mapa.compute(k, (klucz, stara) -> ...);               // dowolna zmiana
mapa.putIfAbsent(k, v);
mapa.replace(k, staraWartosc, nowa);                  // CAS na wartości
```

Funkcje przekazane do `compute*`/`merge` muszą być **krótkie** i nie mogą zmieniać tej samej mapy (mapa w tym czasie blokuje fragment tablicy).

Dla liczników przy dużym ruchu: `ConcurrentHashMap<String, LongAdder>` i `mapa.computeIfAbsent(k, x -> new LongAdder()).increment()`.

Inne fakty:

- `ConcurrentHashMap` nie przyjmuje `null` ani jako klucza, ani jako wartości (wtedy `get` zwracające `null` jest jednoznaczne).
- Iteratory nie rzucają `ConcurrentModificationException`; pokazują stan „gdzieś z czasu iteracji” (*weakly consistent*).
- `size()` przy równoczesnych zmianach jest tylko przybliżeniem.

## Collections.synchronizedXxx

Opakowanie, w którym każda metoda jest `synchronized`. Dwie pułapki:

```java
List<String> lista = Collections.synchronizedList(new ArrayList<>());

// 1. iteracja musi być ręcznie pod blokadą listy
synchronized (lista) {
    for (String s : lista) { ... }
}

// 2. operacje złożone też
synchronized (lista) {
    if (!lista.contains(x)) lista.add(x);
}
```

## BlockingQueue: producent i konsument

Kolejka, która **czeka**: `put` czeka, gdy (ograniczona) kolejka jest pełna, `take` czeka, gdy jest pusta. To gotowy bufor z rozdziału 05.

| | Rzuca wyjątek | Zwraca wartość specjalną | Czeka | Czeka z limitem |
|---|---|---|---|---|
| włóż | `add(e)` | `offer(e)` → `false` | `put(e)` | `offer(e, czas, jedn.)` |
| wyjmij | `remove()` | `poll()` → `null` | `take()` | `poll(czas, jedn.)` |

Pułapka: `offer` na pełnej kolejce **po cichu nie wkłada** elementu (zwraca `false`). Jeśli tego nie sprawdzisz, element przepada.

Wzorzec producent-konsument:

```java
BlockingQueue<Zadanie> kolejka = new ArrayBlockingQueue<>(100);   // ograniczona: producent nie zaleje pamięci

// producent
for (Zadanie z : zrodlo) kolejka.put(z);
for (int i = 0; i < liczbaKonsumentow; i++) kolejka.put(KONIEC);   // „zatruta pigułka” dla każdego konsumenta

// konsument
while (true) {
    Zadanie z = kolejka.take();
    if (z == KONIEC) break;
    obsluz(z);
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
