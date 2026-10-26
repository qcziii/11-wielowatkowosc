#!/usr/bin/env bash
# Uruchamia check.sh wszystkich ćwiczeń i wypisuje podsumowanie.
ROOT=$(cd "$(dirname "$0")/.." && pwd)
ok=0; fail=0
for c in "$ROOT"/cwiczenia/*/; do
  name=$(basename "$c")
  if bash "$c/check.sh" > /dev/null 2>&1; then
    echo "[OK]   $name"; ok=$((ok+1))
  else
    echo "[FAIL] $name"; fail=$((fail+1))
  fi
done
echo
echo "Ćwiczenia: $ok OK, $fail FAIL"
[ "$fail" -eq 0 ]
