# 13. Konfiguracja czytana przez wielu, przeładowywana przez jednego

Poziom: **średni** · Rodzaj: **napisz** · Teoria: [09. Synchronizatory i blokady](../../teoria/09-synchronizatory-i-blokady.md)

## Uruchomienie

```bash
./cwiczenia/13-konfiguracja-rwlock/check.sh
```

## Zadanie

`Configuration` trzyma ustawienia aplikacji w zwykłej `HashMap`. Czyta je każde żądanie HTTP (dużo wątków naraz), a raz na jakiś czas jeden wątek przeładowuje całą konfigurację z pliku. Uzupełnij `get`, `read` i `reload` na `ReentrantReadWriteLock`:

1. czytelnicy nie czekają na siebie nawzajem,
2. przeładowanie czeka, aż obecni czytelnicy skończą, i w trakcie przeładowania nikt nie czyta,
3. czytelnik nigdy nie widzi stanu pośredniego (pół starej, pół nowej konfiguracji albo pustej mapy w trakcie przeładowania),
4. wyjątek rzucony przez kod czytelnika nie zostawia blokady zajętej.

Zakaz: `synchronized`, `volatile`, `ConcurrentHashMap` (sprawdza `check.sh`). Chodzi o przećwiczenie blokad z `java.util.concurrent.locks`.

**Pytania dodatkowe:**

1. Dlaczego samo zamienienie `HashMap` na `ConcurrentHashMap` nie spełnia punktu 3?
2. Jest inne, często lepsze rozwiązanie tego problemu bez żadnej blokady: jakie? (Podpowiedź: niemutowalna mapa i jedna referencja.)
3. Czy wątek trzymający blokadę do odczytu może ją „podnieść” do blokady do zapisu w `ReentrantReadWriteLock`? Co się stanie, jeśli spróbuje?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`.
