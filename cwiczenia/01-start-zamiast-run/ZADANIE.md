# 01. Zadania miały iść równolegle, a idą po kolei

Poziom: **łatwy** · Rodzaj: **napraw** · Teoria: [01. Wątki: podstawy](../../teoria/01-watki-podstawy.md)

## Uruchomienie

```bash
# automatyczne sprawdzenie, z katalogu głównego repo
./cwiczenia/01-start-zamiast-run/check.sh
```

## Zgłoszenie

`UruchamiaczZadan.uruchomRownolegle` miał uruchamiać każde zadanie w osobnym wątku. Wątki są tworzone, mają nawet ładne nazwy `zadanie-1`, `zadanie-2`..., ale trzy zadania po 300 ms trwają razem prawie sekundę, a w logach wszystkie zadania wypisują nazwę wątku `main`.

**Oczekiwane:**

- każde zadanie wykonuje się w swoim wątku, wszystkie w tym samym czasie,
- metoda wraca dopiero wtedy, gdy wszystkie zadania się skończą.

**Pytanie dodatkowe:** Dlaczego `watek.join()` na wątku, którego nikt nie uruchomił, nie zawiesza programu, tylko od razu wraca?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`. Jeśli coś przechodzi „czasem”, to nie jest jeszcze rozwiązane: uruchom `check.sh` kilka razy.
