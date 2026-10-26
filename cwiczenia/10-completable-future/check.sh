#!/usr/bin/env bash
# Ćwiczenie 10-completable-future: Podsumowanie klienta z trzech usług naraz
# Uruchom: ./cwiczenia/10-completable-future/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 10-completable-future: Podsumowanie klienta z trzech usług naraz"
run_tests "$CW_DIR"
summary
