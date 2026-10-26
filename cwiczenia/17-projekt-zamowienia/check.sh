#!/usr/bin/env bash
# Ćwiczenie 17-projekt-zamowienia: Projekt: przetwarzacz zamówień (producent i konsumenci)
# Uruchom: ./cwiczenia/17-projekt-zamowienia/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 17-projekt-zamowienia: Projekt: przetwarzacz zamówień (producent i konsumenci)"
run_tests "$CW_DIR"
summary
