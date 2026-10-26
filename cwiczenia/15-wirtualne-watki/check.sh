#!/usr/bin/env bash
# Ćwiczenie 15-wirtualne-watki: Pobranie 2000 cen trwa pół minuty
# Uruchom: ./cwiczenia/15-wirtualne-watki/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 15-wirtualne-watki: Pobranie 2000 cen trwa pół minuty"
run_tests "$CW_DIR"
summary
