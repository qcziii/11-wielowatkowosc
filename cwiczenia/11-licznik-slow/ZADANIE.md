# 11. Licznik słów na ConcurrentHashMap zaniża wyniki

Poziom: **łatwy** · Rodzaj: **napraw** · Teoria: [08. Kolekcje współbieżne](../../teoria/08-kolekcje-wspolbiezne.md)

## Uruchomienie

```bash
./cwiczenia/11-licznik-slow/check.sh
```

## Zgłoszenie

`WordCounter` dzieli linie tekstu między wątki. Na początku była tam `HashMap` i program potrafił się wysypać albo zawiesić, więc ktoś zamienił ją na `ConcurrentHashMap`. Od tamtej pory nic się nie wysypuje, ale wyniki są za małe: słowo, które występuje 300 000 razy, ma licznik 280 000 z kawałkiem, przy każdym uruchomieniu inny.

**Oczekiwane:** liczby wystąpień są dokładne przy dowolnej liczbie wątków. Rozwiązanie bez `synchronized` i bez własnych blokad.

**Pytanie dodatkowe:** `ConcurrentHashMap` jest „thread-safe”. Co dokładnie to znaczy, a czego nie obiecuje? Wymień trzy metody tej mapy, które robią operację złożoną atomowo.

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
