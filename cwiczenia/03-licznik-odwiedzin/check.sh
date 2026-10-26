#!/usr/bin/env bash
# Ćwiczenie 03-licznik-odwiedzin: Licznik odwiedzin gubi wejścia, choć pole jest volatile
# Uruchom: ./cwiczenia/03-licznik-odwiedzin/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 03-licznik-odwiedzin: Licznik odwiedzin gubi wejścia, choć pole jest volatile"
run_tests "$CW_DIR"
summary
