#!/usr/bin/env bash
# Ćwiczenie 14-zakleszczenie-przelewy: Przelewy w obie strony zawieszają bank
# Uruchom: ./cwiczenia/14-zakleszczenie-przelewy/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 14-zakleszczenie-przelewy: Przelewy w obie strony zawieszają bank"
run_tests "$CW_DIR"
summary
