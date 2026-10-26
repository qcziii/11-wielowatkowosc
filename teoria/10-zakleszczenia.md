# 10. Zakleszczenia i diagnoza wątków

Ćwiczenia: [14](../cwiczenia/14-zakleszczenie-przelewy/ZADANIE.md)

## Zakleszczenie (deadlock)

```java
void transfer(Account from, Account to, long amount) {
    synchronized (from) {
        synchronized (to) {
            ...
        }
    }
}
```

Wątek 1: `transfer(A, B)`, wątek 2 w tej samej chwili: `transfer(B, A)`.

```
Wątek 1: bierze A            Wątek 2: bierze B
Wątek 1: czeka na B ...      Wątek 2: czeka na A ...
                 (oba czekają na siebie nawzajem, na zawsze)
```

Objawy: aplikacja stoi, procesor jest bezczynny (wątki są w stanie `BLOCKED`, nie liczą), nowe żądania też stają, bo czekają na te same blokady. Pomaga tylko restart.

## Cztery warunki (Coffman)

Zakleszczenie powstaje tylko wtedy, gdy wszystkie cztery są spełnione naraz:

1. **Wyłączność:** zasób może mieć tylko jeden wątek (blokada).
2. **Trzymaj i czekaj:** wątek trzyma jedną blokadę i czeka na drugą.
3. **Brak wywłaszczenia:** nikt nie może zabrać wątkowi blokady.
4. **Cykl:** wątek 1 czeka na wątek 2, który czeka na wątek 1 (albo dłuższy cykl).

Każda metoda zapobiegania łamie jeden z warunków.

## Jak zapobiegać

**Stała kolejność blokad** (łamie 4, najczęstsze rozwiązanie): wszyscy biorą blokady w tym samym porządku, np. po numerze rachunku. Cykl nie może powstać.

```java
Account first  = from.number() < to.number() ? from : to;
Account second = first == from ? to : from;
synchronized (first) {
    synchronized (second) { ... }
}
```

Potrzebny jest unikalny, stały klucz porządkujący. `System.identityHashCode` może mieć kolizje (wtedy dodatkowa blokada „rozstrzygająca”).

**`tryLock` z limitem czasu** (łamie 3 w praktyce): jeśli nie uda się wziąć drugiej blokady, oddaj pierwszą i spróbuj jeszcze raz za chwilę. Uwaga na *livelock*: dwa wątki mogą się w nieskończoność nawzajem ustępować; pomaga losowa pauza przed ponowieniem.

**Jedna blokada zamiast dwóch** (łamie 2): np. jedna blokada na cały bank. Poprawne, ale wszystkie przelewy idą po kolei, nawet między niezwiązanymi rachunkami.

**Nie wołaj obcego kodu pod blokadą:** listenery, callbacki, metody innych obiektów wołane w `synchronized` mogą wziąć własne blokady w nieznanej kolejności. Skopiuj dane pod blokadą, a obcy kod wołaj już bez niej.

## Inne problemy z „żywotnością”

- **Livelock:** wątki nie stoją, ale nic nie robią użytecznego, bo ciągle na siebie reagują (ustępują sobie nawzajem).
- **Zagłodzenie (starvation):** wątek nigdy nie dostaje zasobu, bo inni ciągle są przed nim (np. „niesprawiedliwa” blokada pod dużym obciążeniem).
- **Zakleszczenie przez pulę:** zadanie w puli czeka na inne zadanie, które nie ma wolnego wątku w tej samej puli (rozdział 06).

## Diagnoza: zrzut wątków (thread dump)

Gdy aplikacja „stoi”, pierwszy krok to zawsze zrzut wątków: co robi każdy wątek, na co czeka, kto trzyma blokady.

```bash
jps -l                       # lista procesów Javy z PID
jcmd <PID> Thread.print      # zrzut wątków
jstack <PID>                 # to samo, starsze narzędzie
```

W IntelliJ: w oknie Run albo Debug ikona aparatu („Get Thread Dump”). Na Windows w konsoli z działającym programem: `Ctrl+Break`; na Linux/macOS `kill -3 <PID>` (zrzut trafia na standardowe wyjście programu).

Przy zakleszczeniu na `synchronized` albo `ReentrantLock` JVM sam to wykrywa i na końcu zrzutu pisze:

```
Found one Java-level deadlock:
=============================
"przelewy-1":
  waiting to lock monitor 0x... (object 0x..., a pl.kurs.threads.Account),
  which is held by "przelewy-0"
"przelewy-0":
  waiting to lock monitor 0x... (object 0x..., a pl.kurs.threads.Account),
  which is held by "przelewy-1"
```

Programowo: `ManagementFactory.getThreadMXBean().findDeadlockedThreads()` (tak robi test ćwiczenia 14).

Na co patrzeć w zrzucie:

- stan wątku (`BLOCKED`, `WAITING`, `TIMED_WAITING`),
- linie `- locked <0x...>` (co wątek trzyma) i `- waiting to lock <0x...>` (na co czeka),
- wiele wątków w tym samym miejscu (`Semaphore.acquire`, `getConnection`, `Future.get`) zwykle oznacza wyczerpany zasób albo zgubione zwolnienie,
- **nazwy wątków:** dlatego warto je nadawać (`new Thread(r, "import-1")`, własna `ThreadFactory`).

Dobra praktyka: zrób 2-3 zrzuty co kilka sekund. Wątek, który w każdym zrzucie jest w tym samym miejscu, naprawdę tam stoi.

## Pytania sprawdzające

1. Wymień cztery warunki konieczne zakleszczenia.
2. Jak stała kolejność blokad zapobiega zakleszczeniu? Którego warunku dotyczy?
3. Czym różni się livelock od deadlocka?
4. Aplikacja przestała odpowiadać. Jakie są Twoje pierwsze trzy kroki diagnozy?
5. Co w zrzucie wątków oznacza `waiting to lock` i `locked`?
6. Dlaczego wołanie listenerów pod blokadą jest ryzykowne?
