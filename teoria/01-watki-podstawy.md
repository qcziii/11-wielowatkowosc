# 01. Wątki: podstawy

Ćwiczenia: [01](../cwiczenia/01-start-zamiast-run/ZADANIE.md), [02](../cwiczenia/02-suma-bez-join/ZADANIE.md)

## Proces, wątek, po co to wszystko

Program Javy to jeden **proces**: ma swoją pamięć (stertę, czyli wszystkie obiekty). W procesie działa jeden albo więcej **wątków**. Każdy wątek ma własny stos (zmienne lokalne, parametry, kolejne wywołania metod), ale wszystkie wątki widzą **tę samą stertę**, czyli te same obiekty.

To jest jednocześnie cała zaleta i cały problem wielowątkowości:

- zaleta: wątki mogą łatwo dzielić się danymi (wystarczy referencja do obiektu),
- problem: gdy dwa wątki naraz zmieniają ten sam obiekt, wynik może być zły (rozdziały 02 i 03).

Wątki stosujemy z dwóch powodów:

1. **Żeby użyć wielu rdzeni** do obliczeń (np. suma dużej tablicy w 4 częściach naraz).
2. **Żeby nie stać, gdy się czeka** (na sieć, dysk, bazę): gdy jeden wątek czeka na odpowiedź HTTP, inny może robić coś innego.

## Tworzenie i uruchamianie wątku

Wątek to obiekt klasy `Thread`. Kod, który ma się w nim wykonać, przekazujemy jako `Runnable` (interfejs z jedną metodą `void run()`), najczęściej lambdą:

```java
Runnable zadanie = () -> System.out.println("Działam w: " + Thread.currentThread().getName());

Thread watek = new Thread(zadanie, "moj-watek");
watek.start();   // uruchamia NOWY wątek, który wykona zadanie.run()
```

Inne sposoby, które spotkasz w starszym kodzie:

```java
// dziedziczenie po Thread (gorsze: zadanie zlepione z mechanizmem uruchamiania)
class MojWatek extends Thread {
    @Override public void run() { ... }
}
new MojWatek().start();

// Java 21: fabryki i wątki wirtualne
Thread t1 = Thread.ofPlatform().name("worker-1").start(zadanie);
Thread t2 = Thread.ofVirtual().start(zadanie);   // rozdział 11
```

W praktyce rzadko tworzy się wątki ręcznie: używa się puli wątków (`ExecutorService`, rozdział 06). Ale żeby rozumieć pule, trzeba rozumieć zwykły `Thread`.

### `start()` a `run()`: najczęstszy błąd początkujących

```java
watek.run();     // ŹLE: zwykłe wywołanie metody, w BIEŻĄCYM wątku. Żaden nowy wątek nie powstaje.
watek.start();   // DOBRZE: JVM tworzy nowy wątek i w nim woła run()
```

`run()` to zwykła metoda. Jeśli wołasz ją sam, wykonuje się po kolei, w wątku, który ją wywołał (np. `main`). Kod się kompiluje, działa, daje dobre wyniki, tylko nic nie dzieje się równolegle. Rozpoznasz to po tym, że `Thread.currentThread().getName()` w zadaniu zwraca `main` (albo nazwę wątku testu), a nie nazwę nowego wątku.

`start()` można zawołać na danym obiekcie `Thread` tylko raz. Drugie wywołanie rzuca `IllegalThreadStateException`.

## Czekanie na wątek: `join()`

`start()` wraca od razu, nie czeka, aż wątek skończy pracę. Jeśli potrzebujesz wyniku pracy wątku, musisz na niego poczekać:

```java
long[] wynik = new long[1];
Thread t = new Thread(() -> wynik[0] = policzCosDlugo());
t.start();
// tu wątek t może jeszcze liczyć, wynik[0] może wynosić 0
t.join();        // czekamy, aż t się zakończy
System.out.println(wynik[0]);   // teraz na pewno widzimy wynik
```

Dwie rzeczy, które `join()` daje:

1. **Czekanie:** bieżący wątek stoi, dopóki `t` nie skończy `run()`.
2. **Widoczność:** wszystko, co `t` zapisał w pamięci, jest widoczne po `join()` (to jest relacja *happens-before*, rozdział 03). Bez `join()` nie masz gwarancji, że zobaczysz zapisane wartości, nawet jeśli wątek już skończył.

Warianty: `join(long millis)` czeka maksymalnie tyle milisekund; po nim sprawdź `t.isAlive()`, żeby wiedzieć, czy wątek się skończył.

`join()` na wątku, którego nikt nie uruchomił (stan `NEW`), wraca od razu: wątek „nie żyje”, więc nie ma na co czekać. To dlatego błąd `run()` zamiast `start()` często nie zawiesza programu.

## Stany wątku

`t.getState()` zwraca jeden z sześciu stanów:

| Stan | Co znaczy |
|---|---|
| `NEW` | utworzony, `start()` jeszcze nie zawołane |
| `RUNNABLE` | działa albo jest gotowy do działania (czeka na przydział procesora) |
| `BLOCKED` | czeka na wejście do bloku `synchronized`, który trzyma inny wątek |
| `WAITING` | czeka bez limitu czasu: `wait()`, `join()`, `LockSupport.park()` (np. w `Lock.lock()`, `BlockingQueue.take()`) |
| `TIMED_WAITING` | czeka z limitem: `sleep(ms)`, `wait(ms)`, `join(ms)` |
| `TERMINATED` | `run()` się skończyło (normalnie albo wyjątkiem) |

Stany przydają się przy diagnozie: w zrzucie wątków (rozdział 10) od razu widać, kto czeka, na co i kto trzyma blokadę.

## Przydatne metody

```java
Thread.currentThread()          // obiekt bieżącego wątku
Thread.currentThread().getName()
Thread.sleep(100);              // uśpij BIEŻĄCY wątek na 100 ms (rzuca InterruptedException, rozdział 04)
t.isAlive()                     // czy wątek wystartował i jeszcze się nie skończył
t.setDaemon(true);              // przed start(): wątek demon
t.interrupt();                  // poproś wątek o przerwanie (rozdział 04)
```

`Thread.sleep` to metoda **statyczna**: zawsze usypia bieżący wątek. `t.sleep(100)` się kompiluje, ale usypia ten wątek, który to wywołał, nie `t`.

### Wątki demony

JVM kończy działanie, gdy skończą się wszystkie wątki **nie-demony**. Wątek `main` nie jest demonem, zwykły `new Thread` też nie (dziedziczy to po wątku, który go utworzył). Wątki demony (np. wątki GC) nie trzymają JVM przy życiu: gdy zostaną tylko one, JVM się kończy i po prostu je ucina.

Praktyczny skutek: jeśli utworzysz pulę wątków i jej nie zamkniesz, program się nie skończy, bo wątki puli (nie-demony) dalej czekają na zadania.

## Wyjątek w wątku

Wyjątek rzucony w `run()` **nie leci do wątku, który zawołał `start()`**. Kończy tylko ten jeden wątek; domyślnie jego stos jest wypisywany na `System.err`:

```java
Thread t = new Thread(() -> { throw new IllegalStateException("bum"); });
t.start();
t.join();
System.out.println("main działa dalej, o wyjątku wie tylko System.err");
```

Jeśli wynik albo wyjątek ma wrócić do wołającego, potrzebujesz `Future` (rozdział 06) albo `CompletableFuture` (rozdział 07). Można też ustawić `t.setUncaughtExceptionHandler(...)`.

## Podział pracy między wątki

Klasyczny schemat (ćwiczenie 02): tablica długości `n` dzielona na `k` części.

```java
int rozmiar = n / k;
for (int i = 0; i < k; i++) {
    int od = i * rozmiar;
    int doWyl = (i == k - 1) ? n : od + rozmiar;   // ostatnia część bierze resztę z dzielenia!
    ...
}
```

Każdy wątek pisze do **własnej** komórki tablicy wyników (`sumy[i]`), więc wątki nie piszą do tej samej zmiennej. Wyniki sumujemy dopiero po `join()` wszystkich wątków.

Zmienne z lambdy muszą być *effectively final*. Dlatego w pętli robi się kopię: `final int nr = i;`. Nie da się w lambdzie zrobić `suma += x` na zmiennej lokalnej z zewnątrz, i dobrze, bo to byłby wyścig (rozdział 02).

## Pytania sprawdzające

1. Czym różni się `t.run()` od `t.start()`? Jak w działającym programie poznać, że ktoś pomylił jedno z drugim?
2. Co robi `join()`, oprócz czekania?
3. Program uruchamia wątek, który liczy w nieskończonej pętli, i kończy `main`. Czy program się zakończy? A jeśli wątek jest demonem?
4. Wątek rzucił wyjątek w `run()`. Gdzie ten wyjątek trafia?
5. Jaki stan ma wątek, który czeka w `Thread.sleep(1000)`? A który czeka na wejście do `synchronized`?
6. Dlaczego `Thread.sleep()` jest statyczna i co z tego wynika?
