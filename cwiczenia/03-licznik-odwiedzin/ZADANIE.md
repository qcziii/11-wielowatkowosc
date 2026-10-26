# 03. Licznik odwiedzin gubi wejścia, choć pole jest volatile

Poziom: **łatwy** · Rodzaj: **napraw** · Teoria: [02. Współdzielony stan](../../teoria/02-wspoldzielony-stan.md), [03. Widoczność](../../teoria/03-widocznosc-i-volatile.md)

## Uruchomienie

```bash
./cwiczenia/03-licznik-odwiedzin/check.sh
```

## Zgłoszenie

Statystyki strony pokazują mniej odwiedzin niż log serwera. Ktoś już raz to „naprawiał”: dodał `volatile` do pól licznika, z komentarzem, że teraz wszystkie wątki widzą aktualną wartość. Liczby dalej się nie zgadzają: przy 1,6 mln odwiedzin z 8 wątków licznik pokazuje kilkaset tysięcy mniej.

**Oczekiwane:** `visits()` i `uniqueUsers()` po zakończeniu wszystkich wątków zwracają dokładnie tyle, ile było wywołań.

Naprawę zrób na dwa sposoby (najpierw jeden, sprawdź, potem drugi) i porównaj:

1. `synchronized`,
2. klasy z `java.util.concurrent.atomic`.

**Pytanie dodatkowe:** Co dokładnie gwarantuje `volatile`, a czego nie? Rozpisz `visits++` na kroki, które wykonuje procesor, i pokaż, w którym miejscu dwa wątki gubią jedno zwiększenie.

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
