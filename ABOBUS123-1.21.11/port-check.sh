#!/data/data/com.termux/files/usr/bin/bash
# ABOBUS123 -> 1.21.11  : build + compact error digest
set -u
LOG=build.log
SUM=errors-summary.txt

./gradlew clean build --no-daemon --stacktrace 2>&1 | tee "$LOG"

{
  echo "=== RESULT ==="
  grep -E "BUILD (SUCCESSFUL|FAILED)" "$LOG" | tail -1
  echo
  echo "=== TOTAL ERRORS ==="
  grep -cE "error:" "$LOG" || echo 0
  echo
  echo "=== TOP ERROR MESSAGES ==="
  grep -E "error:" "$LOG" | sed -E 's/.*error: //' | sed -E 's/[0-9]+/N/g' \
    | sort | uniq -c | sort -rn | head -60
  echo
  echo "=== WORST FILES ==="
  grep -E "error:" "$LOG" | grep -oE "[A-Za-z0-9_]+\.java" \
    | sort | uniq -c | sort -rn | head -40
  echo
  echo "=== NON-COMPILE FAILURES (gradle/loom) ==="
  grep -E "What went wrong|Could not resolve|FAILURE:" -A3 "$LOG" | head -40
} > "$SUM"

echo
echo "-------- summary written to $SUM --------"
cat "$SUM"
