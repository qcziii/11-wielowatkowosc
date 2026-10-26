#!/usr/bin/env bash
# Ćwiczenie 09-zgubione-wyjatki: Import cennika nie zgłasza błędów i gubi wiersze
# Uruchom: ./cwiczenia/09-zgubione-wyjatki/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 09-zgubione-wyjatki: Import cennika nie zgłasza błędów i gubi wiersze"
run_tests "$CW_DIR"
summary
