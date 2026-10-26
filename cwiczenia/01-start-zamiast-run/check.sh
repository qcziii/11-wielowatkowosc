#!/usr/bin/env bash
# Ćwiczenie 01-start-zamiast-run: Zadania miały iść równolegle, a idą po kolei
# Uruchom: ./cwiczenia/01-start-zamiast-run/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 01-start-zamiast-run: Zadania miały iść równolegle, a idą po kolei"
run_tests "$CW_DIR"
summary
