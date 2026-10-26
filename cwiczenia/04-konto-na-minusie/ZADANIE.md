# 04. Konto schodzi poniżej zera, choć saldo jest AtomicInteger

Poziom: **łatwy/średni** · Rodzaj: **napraw** · Teoria: [02. Współdzielony stan](../../teoria/02-wspoldzielony-stan.md)

## Uruchomienie

```bash
./cwiczenia/04-konto-na-minusie/check.sh
```

## Zgłoszenie

Konto nie ma debetu, a mimo to klienci potrafią wypłacić więcej, niż mają. Zdarza się to rzadko, zwykle gdy ktoś płaci kartą w tej samej chwili, w której przychodzi polecenie zapłaty. Saldo jest w `AtomicInteger`, więc zespół uważa, że „to nie może być wątek”.

**Oczekiwane:** saldo nigdy nie spada poniżej zera, a każda udana wypłata zmniejsza saldo dokładnie o swoją kwotę. Nieudana wypłata nie zmienia salda.

**Pytanie dodatkowe:** Każda pojedyncza operacja na `AtomicInteger` jest atomowa. Dlaczego `wyplac` mimo to nie jest? Jak nazywa się ten wzorzec błędu?

## Kiedy skończysz

`check.sh` ma wypisać `WYNIK: wszystkie sprawdzenia przeszły`. Spróbuj dwóch rozwiązań: z `synchronized` i bez żadnej blokady (podpowiedź: `compareAndSet`).
