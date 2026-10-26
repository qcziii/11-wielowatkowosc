#!/usr/bin/env bash
# Ćwiczenie 13-konfiguracja-rwlock: Konfiguracja czytana przez wielu, przeładowywana przez jednego
# Uruchom: ./cwiczenia/13-konfiguracja-rwlock/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 13-konfiguracja-rwlock: Konfiguracja czytana przez wielu, przeładowywana przez jednego"
run_tests "$CW_DIR"
forbid "$CW_DIR" "synchronized|ConcurrentHashMap|volatile" "Konfiguracja używa ReadWriteLock, bez synchronized, volatile i ConcurrentHashMap"
summary
