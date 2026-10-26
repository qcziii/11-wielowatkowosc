#!/usr/bin/env bash
# Ćwiczenie 05-watek-nie-chce-stanac: Wątek nie zatrzymuje się po zatrzymaj()
# Uruchom: ./cwiczenia/05-watek-nie-chce-stanac/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 05-watek-nie-chce-stanac: Wątek nie zatrzymuje się po zatrzymaj()"
run_tests "$CW_DIR"
summary
