#!/usr/bin/env bash
# Ćwiczenie 12-pula-polaczen: Pula połączeń zawiesza się po kilku błędach
# Uruchom: ./cwiczenia/12-pula-polaczen/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 12-pula-polaczen: Pula połączeń zawiesza się po kilku błędach"
run_tests "$CW_DIR"
summary
