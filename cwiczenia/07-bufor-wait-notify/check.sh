#!/usr/bin/env bash
# Ćwiczenie 07-bufor-wait-notify: Bufor ograniczony na wait() i notifyAll()
# Uruchom: ./cwiczenia/07-bufor-wait-notify/check.sh (z dowolnego katalogu). Kompiluje ćwiczenie i uruchamia jego testy.
CW_DIR=$(cd "$(dirname "$0")" && pwd)
source "$CW_DIR/../../scripts/lib.sh"
echo "Ćwiczenie 07-bufor-wait-notify: Bufor ograniczony na wait() i notifyAll()"
run_tests "$CW_DIR"
forbid "$CW_DIR" "java\\.util\\.concurrent\\.[A-Za-z*]" "Bufor nie używa java.util.concurrent (tylko synchronized, wait, notifyAll)"
summary
