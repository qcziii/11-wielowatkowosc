#!/usr/bin/env bash
# Ćwiczenie 16-simpledateformat: Parser logów zwraca złe daty przy równoległym parsowaniu
# Uruchom: ./cwiczenia/16-simpledateformat/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 16-simpledateformat: Parser logów zwraca złe daty przy równoległym parsowaniu"
run_tests "$CW_DIR"
summary
