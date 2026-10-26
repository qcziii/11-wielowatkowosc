# Wielowątkowość w Javie: powtórka i ćwiczenia

Repo ma dwie części:

1. **Teoria** (`teoria/`): 11 krótkich rozdziałów od `Thread` i `join` do wątków wirtualnych, z przykładami, typowymi błędami i pytaniami sprawdzającymi. Do tego jednostronicowa [ściąga](SCIAGA.md).
2. **Ćwiczenia** (`cwiczenia/`): 17 zadań z automatycznym sprawdzaniem. W większości dostajesz kod z błędem współbieżności i opis zgłoszenia („napraw”), w kilku piszesz coś od zera („napisz”). Każde ćwiczenie ma test, który przejdzie dopiero po poprawnym rozwiązaniu.

Zalecany sposób pracy: przeczytaj rozdział teorii, odpowiedz sobie na pytania sprawdzające na jego końcu, potem zrób ćwiczenia, które się do niego odwołują.

## Wymagania

- Java 21+ i Maven 3.9+
- `bash` (na Windows: Git Bash) do uruchamiania `check.sh`
- internet przy pierwszym uruchomieniu (Maven pobiera JUnit)

Bez Springa, bez bazy, bez Dockera: czysta Java.

## Jak pracować

```bash
# automatyczne sprawdzenie jednego ćwiczenia, z katalogu głównego repo
./cwiczenia/01-start-zamiast-run/check.sh
```

Przeczytaj `ZADANIE.md` w katalogu ćwiczenia, zobacz problem, znajdź przyczynę, popraw i sprawdź. Ćwiczenie jest rozwiązane, gdy `check.sh` wypisze `WYNIK: wszystkie sprawdzenia przeszły`.

`check.sh` kompiluje ćwiczenie i uruchamia jego testy (`mvn test`, klasa `CheckTest`). Test możesz też uruchomić w IntelliJ (zielona strzałka przy `CheckTest`), także w debuggerze. Projekt otwierasz w IntelliJ jako projekt Maven: plik `pom.xml` w katalogu głównym otwiera wszystkie ćwiczenia naraz.

**Błędy współbieżności są losowe.** Jeśli test przeszedł raz, uruchom go jeszcze kilka razy. Rozwiązanie, które przechodzi „zwykle”, nie jest rozwiązaniem. Z drugiej strony testy są tak napisane, że zepsuty kod nie przechodzi praktycznie nigdy.

## Teoria

| # | Rozdział | Ćwiczenia |
|---|---|---|
| 01 | [Wątki: podstawy](teoria/01-watki-podstawy.md) (`Thread`, `start`, `join`, stany, demony) | 01, 02 |
| 02 | [Współdzielony stan](teoria/02-wspoldzielony-stan.md) (wyścigi, `synchronized`, klasy atomowe, check-then-act) | 03, 04 |
| 03 | [Widoczność i volatile](teoria/03-widocznosc-i-volatile.md) (happens-before, bezpieczna publikacja) | 05, 03 |
| 04 | [Przerywanie wątków](teoria/04-przerywanie.md) (`interrupt`, `InterruptedException`) | 06 |
| 05 | [wait i notifyAll](teoria/05-wait-notify.md) (czekanie na warunek) | 07 |
| 06 | [ExecutorService i Future](teoria/06-executory.md) (pule, wyjątki, zamykanie) | 08, 09 |
| 07 | [CompletableFuture](teoria/07-completablefuture.md) | 10 |
| 08 | [Kolekcje współbieżne](teoria/08-kolekcje-wspolbiezne.md) (`ConcurrentHashMap`, `BlockingQueue`, `SimpleDateFormat`) | 11, 16, 17 |
| 09 | [Synchronizatory i blokady](teoria/09-synchronizatory-i-blokady.md) (`CountDownLatch`, `Semaphore`, `ReentrantLock`, `ReadWriteLock`) | 12, 13 |
| 10 | [Zakleszczenia i diagnoza](teoria/10-zakleszczenia.md) (deadlock, zrzut wątków) | 14 |
| 11 | [Wątki wirtualne](teoria/11-watki-wirtualne.md) (Java 21+) | 15 |

## Ćwiczenia

| # | Ćwiczenie | Rodzaj | Poziom |
|---|---|---|---|
| 01 | [Zadania miały iść równolegle, a idą po kolei](cwiczenia/01-start-zamiast-run/ZADANIE.md) | napraw | łatwy |
| 02 | [Suma liczona w kilku wątkach wychodzi za każdym razem inna](cwiczenia/02-suma-bez-join/ZADANIE.md) | napraw | łatwy (2 błędy) |
| 03 | [Licznik odwiedzin gubi wejścia, choć pole jest volatile](cwiczenia/03-licznik-odwiedzin/ZADANIE.md) | napraw | łatwy |
| 04 | [Konto schodzi poniżej zera, choć saldo jest AtomicInteger](cwiczenia/04-konto-na-minusie/ZADANIE.md) | napraw | łatwy/średni |
| 05 | [Wątek nie zatrzymuje się po zatrzymaj()](cwiczenia/05-watek-nie-chce-stanac/ZADANIE.md) | napraw | łatwy |
| 06 | [Monitora nie da się wyłączyć](cwiczenia/06-polkniete-przerwanie/ZADANIE.md) | napraw | średni |
| 07 | [Bufor ograniczony na wait() i notifyAll()](cwiczenia/07-bufor-wait-notify/ZADANIE.md) | napisz | średni |
| 08 | [Przetwarzanie listy na puli wątków](cwiczenia/08-executor-service/ZADANIE.md) | napisz | średni |
| 09 | [Import cennika nie zgłasza błędów i gubi wiersze](cwiczenia/09-zgubione-wyjatki/ZADANIE.md) | napraw | średni (2 błędy) |
| 10 | [Podsumowanie klienta z trzech usług naraz](cwiczenia/10-completable-future/ZADANIE.md) | napisz | średni/trudny |
| 11 | [Licznik słów na ConcurrentHashMap zaniża wyniki](cwiczenia/11-licznik-slow/ZADANIE.md) | napraw | łatwy |
| 12 | [Pula połączeń zawiesza się po kilku błędach](cwiczenia/12-pula-polaczen/ZADANIE.md) | napraw | średni |
| 13 | [Konfiguracja czytana przez wielu, przeładowywana przez jednego](cwiczenia/13-konfiguracja-rwlock/ZADANIE.md) | napisz | średni |
| 14 | [Przelewy w obie strony zawieszają bank](cwiczenia/14-zakleszczenie-przelewy/ZADANIE.md) | napraw | średni |
| 15 | [Pobranie 2000 cen trwa pół minuty](cwiczenia/15-wirtualne-watki/ZADANIE.md) | napraw | łatwy |
| 16 | [Parser logów zwraca złe daty przy równoległym parsowaniu](cwiczenia/16-simpledateformat/ZADANIE.md) | napraw | łatwy |
| 17 | [Projekt: przetwarzacz zamówień (producent i konsumenci)](cwiczenia/17-projekt-zamowienia/ZADANIE.md) | napisz | trudny |

## Zasady

- Zmieniasz kod w `src/main` ćwiczenia. Nie zmieniaj testów (`src/test`), `check.sh` ani `scripts/`.
- Wymagania z `ZADANIE.md` są częścią zadania (np. „bez `synchronized`”, „bez globalnej blokady”). Niektóre sprawdza `check.sh`.
- Zanim cokolwiek zmienisz, **wyjaśnij sobie, dlaczego kod nie działa**: który przeplot wątków albo jaka reguła z teorii. Poprawka „na próbę” (dorzucenie `volatile`, `synchronized` albo `sleep` tu i tam) zwykle tylko ukrywa błąd.
- `Thread.sleep` nie jest synchronizacją. Jeśli rozwiązanie działa dopiero po dodaniu `sleep`, to nie działa.
- Każde `ZADANIE.md` ma pytania dodatkowe. Odpowiedz na nie, zanim uznasz ćwiczenie za skończone: na omówieniu padną właśnie te pytania.
- Pracuj na własnej gałęzi, żeby łatwo wrócić do wersji wyjściowej (`git checkout -- cwiczenia/NN-...`).

Wszystkie ćwiczenia kompiluje naraz `mvn compile` w katalogu głównym. Wszystkie testy naraz: `./scripts/check-all.sh` (na początku wszystkie są czerwone, i tak ma być; trwa kilka minut).
