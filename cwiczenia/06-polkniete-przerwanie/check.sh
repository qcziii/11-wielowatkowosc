#!/usr/bin/env bash
# Ćwiczenie 06-polkniete-przerwanie: Monitora nie da się wyłączyć
# Uruchom: ./cwiczenia/06-polkniete-przerwanie/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 06-polkniete-przerwanie: Monitora nie da się wyłączyć"
run_tests "$CW_DIR"
summary
