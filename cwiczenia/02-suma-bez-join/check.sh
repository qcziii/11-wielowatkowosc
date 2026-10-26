#!/usr/bin/env bash
# Ćwiczenie 02-suma-bez-join: Suma liczona w kilku wątkach wychodzi za każdym razem inna
# Uruchom: ./cwiczenia/02-suma-bez-join/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 02-suma-bez-join: Suma liczona w kilku wątkach wychodzi za każdym razem inna"
run_tests "$CW_DIR"
summary
