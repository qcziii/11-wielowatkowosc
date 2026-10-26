#!/usr/bin/env bash
# Ćwiczenie 04-konto-na-minusie: Konto schodzi poniżej zera, choć saldo jest AtomicInteger
# Uruchom: ./cwiczenia/04-konto-na-minusie/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 04-konto-na-minusie: Konto schodzi poniżej zera, choć saldo jest AtomicInteger"
run_tests "$CW_DIR"
summary
