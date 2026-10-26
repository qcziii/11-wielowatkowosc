#!/usr/bin/env bash
# Ćwiczenie 11-licznik-slow: Licznik słów na ConcurrentHashMap zaniża wyniki
# Uruchom: ./cwiczenia/11-licznik-slow/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 11-licznik-slow: Licznik słów na ConcurrentHashMap zaniża wyniki"
run_tests "$CW_DIR"
summary
