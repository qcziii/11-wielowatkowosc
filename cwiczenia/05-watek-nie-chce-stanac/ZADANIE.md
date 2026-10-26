# 05. Wątek nie zatrzymuje się po stop()

Poziom: **łatwy** · Rodzaj: **napraw** · Teoria: [03. Widoczność i volatile](../../teoria/03-widocznosc-i-volatile.md)

## Uruchomienie

```bash
./cwiczenia/05-watek-nie-chce-stanac/check.sh
```

## Zgłoszenie

`Generator` liczy kroki symulacji w osobnym wątku. Przycisk „Stop” woła `stop()`, które ustawia flagę `running` na `false`. Pętla sprawdza tę flagę przy każdym obrocie, a mimo to wątek kręci się dalej i zjada rdzeń procesora. Najdziwniejsze: w debuggerze, z pułapką w pętli, wszystko działa.

**Oczekiwane:** po `stop()` wątek generatora kończy się (w testach: w ciągu 2 sekund).

**Pytanie dodatkowe:**

1. Dlaczego z pułapką w debuggerze, albo z `System.out.println` w pętli, błąd znika?
2. `steps` nie jest `volatile`, a test czyta je z innego wątku. Dlaczego tu to jest poprawne? (Podpowiedź: co gwarantuje `join()`.)

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
