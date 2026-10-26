# 15. Pobranie 2000 cen trwa pół minuty

Poziom: **łatwy** · Rodzaj: **napraw** · Teoria: [11. Wątki wirtualne](../../teoria/11-watki-wirtualne.md)

## Uruchomienie

```bash
./cwiczenia/15-wirtualne-watki/check.sh
```

## Zgłoszenie

`PriceFetcher` pobiera ceny produktów z zewnętrznego API. Jedno żądanie to ok. 100 ms czekania na sieć, procesor w tym czasie nic nie robi. Na puli 8 wątków 2000 produktów to 2000 × 100 ms / 8 = 25 sekund. Ktoś zaproponował pulę 2000 wątków, ale na serwerze to 2000 wątków systemowych z własnymi stosami, na każde wywołanie.

**Oczekiwane:**

- 2000 produktów w mniej niż 3 sekundy,
- wywołania API idą w **wątkach wirtualnych** (Java 21+),
- po powrocie nic z pobierania nie zostaje uruchomione.

**Pytania dodatkowe:**

1. Dlaczego wątków wirtualnych nie wkłada się do puli (`newFixedThreadPool` z fabryką wątków wirtualnych)? Jak w takim razie ograniczyć liczbę równoczesnych żądań do API, gdyby API pozwalało tylko na 100 naraz?
2. Czy wątki wirtualne przyspieszyłyby obliczenia, które cały czas zajmują procesor (np. liczenie skrótów SHA)? Dlaczego?
3. Co to jest „przypięcie” (pinning) wątku wirtualnego i co się w tej sprawie zmieniło w Javie 24?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
