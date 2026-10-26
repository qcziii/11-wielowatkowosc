# 06. Monitora nie da się wyłączyć

Poziom: **średni** · Rodzaj: **napraw** · Teoria: [04. Przerywanie wątków](../../teoria/04-przerywanie.md)

## Uruchomienie

```bash
./cwiczenia/06-polkniete-przerwanie/check.sh
```

## Zgłoszenie

`MonitorDysku` co 100 ms odczytuje czujnik. Przy zamykaniu aplikacji wołamy `stop()`, który przerywa wątek monitora (`interrupt()`) i czeka na jego koniec. W logu pojawia się `Monitor: przerwano oczekiwanie`, ale nigdy `Monitor: koniec pracy`, `stop()` zwraca `false`, a aplikacja nie chce się zamknąć. Pętla przecież sprawdza `isInterrupted()`...

**Oczekiwane:**

- `stop()` kończy wątek monitora, niezależnie od tego, czy monitor akurat śpi, czy odczytuje czujnik,
- po `stop()` nie ma już nowych pomiarów.

Nie dodawaj własnej flagi `volatile boolean`: zrób to na mechanizmie przerwań, tak jak jest zaprojektowane.

**Pytanie dodatkowe:** Co dzieje się z flagą przerwania wątku w chwili, gdy `Thread.sleep` rzuca `InterruptedException`? Jakie są dwie poprawne reakcje na `InterruptedException`, a która jest prawie zawsze zła?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
