#!/usr/bin/env bash
# Ćwiczenie 08-executor-service: Przetwarzanie listy na puli wątków
# Uruchom: ./cwiczenia/08-executor-service/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 08-executor-service: Przetwarzanie listy na puli wątków"
run_tests "$CW_DIR"
summary
