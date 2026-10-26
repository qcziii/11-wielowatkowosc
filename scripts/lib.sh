#!/usr/bin/env bash
# Wspólne funkcje dla skryptów check.sh w ćwiczeniach.
# Wymagają bash, Mavena (mvn) i Javy 21. Na Windows: Git Bash.

PASS=0
FAIL=0

ok()  { echo "  [OK]   $1"; PASS=$((PASS+1)); }
bad() { echo "  [FAIL] $1"; FAIL=$((FAIL+1)); }

# Zamienia encje XML z raportu surefire na zwykłe znaki.
xml_decode() {
  sed -e 's/&#10;/\n/g' -e 's/&#13;//g' -e 's/&quot;/"/g' -e "s/&apos;/'/g" \
      -e 's/&lt;/</g' -e 's/&gt;/>/g' -e 's/&amp;/\&/g'
}

# run_tests <katalog ćwiczenia>
# Kompiluje ćwiczenie i uruchamia jego testy (mvn test), potem wypisuje wynik każdego testu.
run_tests() {
  local dir log reports
  dir=$(cd "$1" && pwd)
  log="$dir/target/check-mvn.log"
  reports="$dir/target/surefire-reports"
  mkdir -p "$dir/target"
  rm -rf "$reports"
  echo "  ... kompilacja i testy (mvn test), pierwsze uruchomienie pobiera zależności"
  (cd "$dir" && mvn -B -q test > "$log" 2>&1)

  if ! ls "$reports"/TEST-*.xml > /dev/null 2>&1; then
    if grep -q 'COMPILATION ERROR' "$log"; then
      bad "Projekt się nie kompiluje. Błędy kompilacji:"
      sed -n '/COMPILATION ERROR/,/Failed to execute goal/p' "$log" | grep -vE 'COMPILATION ERROR|Failed to execute goal|^\[INFO\]' \
        | sed -e "s#$dir/##g" -e 's#\\#/#g' | head -12 | sed 's/^/           /'
    else
      bad "Testy nie wystartowały. Ostatnie błędy Mavena:"
      grep -E '^\[ERROR\]' "$log" | grep -vE 'Re-run Maven|Help 1|^\[ERROR\] *$|-> \[Help|For more information|full stack trace|full debug logging|http://cwiki' \
        | head -12 | sed 's/^/           /'
    fi
    echo "           (pełny log: target/check-mvn.log w katalogu ćwiczenia)"
    return
  fi

  local line status name msg
  while IFS=$'\t' read -r status name msg; do
    name=$(printf '%s' "$name" | xml_decode)
    if [ "$status" = "OK" ]; then
      ok "$name"
    else
      bad "$name"
      printf '%s\n' "$msg" | xml_decode | sed '/^[[:space:]]*$/d' | head -8 | sed 's/^/           /'
    fi
  done < <(cat "$reports"/TEST-*.xml | awk '
    /<testcase / {
      name = ""; if (match($0, /name="[^"]*"/)) name = substr($0, RSTART + 6, RLENGTH - 7)
      st = "OK"; msg = ""
      if ($0 ~ /\/>[[:space:]]*$/) { print st "\t" name "\t"; inCase = 0 } else { inCase = 1 }
      next
    }
    inCase && /<(failure|error)[ >]/ {
      st = "FAIL"
      if (match($0, /message="[^"]*"/)) msg = substr($0, RSTART + 9, RLENGTH - 10)
      else msg = "(brak komunikatu, szczegóły w target/surefire-reports)"
    }
    inCase && /<\/testcase>/ { print st "\t" name "\t" msg; inCase = 0 }
  ')
}

# forbid <katalog ćwiczenia> <wzorzec grep -E> <opis>
# Sprawdza, że w kodzie ćwiczenia (src/main) nie ma zakazanej konstrukcji.
forbid() {
  local hits
  hits=$(grep -rnE "$2" "$1/src/main/java" 2>/dev/null | sed -e "s#$1/##")
  if [ -n "$hits" ]; then
    bad "$3"
    printf '%s\n' "$hits" | head -5 | sed 's/^/           /'
  else
    ok "$3"
  fi
}

summary() {
  echo
  echo "Wynik: $PASS OK, $FAIL FAIL"
  if [ "$FAIL" -eq 0 ] && [ "$PASS" -gt 0 ]; then
    echo "WYNIK: wszystkie sprawdzenia przeszły"
  else
    echo "WYNIK: są błędy"
    exit 1
  fi
}
