#!/usr/bin/env bash
# Fast controls for scripts/mutation.sh. The fixtures simulate a cached PIT report; PIT is not run.
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "$script_dir/.."

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

threshold="$(sed -n 's/^nepdateMutationThreshold=//p' gradle.properties)"
max_surviving="$(sed -n 's/^nepdateMaxSurviving=//p' gradle.properties)"
if [ -z "$threshold" ] || [ -z "$max_surviving" ]; then
  echo "mutation controls: the thresholds are missing from gradle.properties"
  exit 1
fi

write_report() {
  local killed="$1" timed_out="$2" survived="$3" no_coverage="$4" path="$5" i
  {
    echo "<mutations>"
    for ((i = 0; i < killed; i++)); do echo "<mutation status='KILLED'></mutation>"; done
    for ((i = 0; i < timed_out; i++)); do echo "<mutation status='TIMED_OUT'></mutation>"; done
    for ((i = 0; i < survived; i++)); do echo "<mutation status='SURVIVED'></mutation>"; done
    for ((i = 0; i < no_coverage; i++)); do echo "<mutation status='NO_COVERAGE'></mutation>"; done
    echo "</mutations>"
  } > "$path"
}

expect_failure() {
  local report="$1" message="$2" console="${3:-}" output status
  set +e
  output="$(NEPDATE_MUTATION_CACHED=1 NEPDATE_MUTATION_REPORT="$report" \
    NEPDATE_MUTATION_CONSOLE="$console" "$script_dir/mutation.sh" 2>&1)"
  status=$?
  set -e
  if [ "$status" -ne 1 ]; then
    printf 'expected exit 1, got %s\n%s\n' "$status" "$output" >&2
    exit 1
  fi
  if ! printf '%s\n' "$output" | grep -qF "$message"; then
    printf 'expected message: %s\ngot:\n%s\n' "$message" "$output" >&2
    exit 1
  fi
}

# A report that passes: everything detected.
write_report 500 0 0 0 "$tmp/pass.xml"
if ! NEPDATE_MUTATION_CACHED=1 NEPDATE_MUTATION_REPORT="$tmp/pass.xml" \
  "$script_dir/mutation.sh" > "$tmp/pass.out" 2>&1; then
  printf 'expected the all-killed report to pass\n%s\n' "$(cat "$tmp/pass.out")" >&2
  exit 1
fi

# A report with one more missed mutant than the budget.
write_report 500 0 $((max_surviving + 1)) 0 "$tmp/over.xml"
expect_failure "$tmp/over.xml" \
  "mutation: $((max_surviving + 1)) missed mutations exceeds max surviving $max_surviving"

# A report under the score threshold but inside the surviving budget.
if [ "$threshold" -gt 0 ] && [ "$threshold" -lt 100 ]; then
  detected=$(( (threshold * max_surviving) / (100 - threshold) / 2 ))
  if [ "$detected" -lt 1 ]; then
    detected=1
  fi
  write_report "$detected" 0 "$max_surviving" 0 "$tmp/low.xml"
  score=$((detected * 100 / (detected + max_surviving)))
  if [ "$score" -lt "$threshold" ]; then
    expect_failure "$tmp/low.xml" "mutation: score ${score}% is below threshold ${threshold}%"
  fi
fi

# PIT rounds its printed percentage and the gate floors the same ratio: 803 of 823 prints as 98%
# and floors to 97%, and the gate must accept that. The counts still have to match.
write_report 803 0 20 0 "$tmp/rounded.xml"
printf '%s\n' '>> Generated 823 mutations Killed 803 (98%)' > "$tmp/rounded.log"
if ! NEPDATE_MUTATION_CACHED=1 NEPDATE_MUTATION_REPORT="$tmp/rounded.xml" \
  NEPDATE_MUTATION_CONSOLE="$tmp/rounded.log" "$script_dir/mutation.sh" > "$tmp/rounded.out" 2>&1; then
  printf 'expected the rounded console percentage to pass\n%s\n' "$(cat "$tmp/rounded.out")" >&2
  exit 1
fi

printf '%s\n' '>> Generated 823 mutations Killed 802 (97%)' > "$tmp/count.log"
expect_failure "$tmp/rounded.xml" \
  "mutation: XML report (803/823) disagrees with PIT (802/823)" "$tmp/count.log"

printf "<mutations><mutation status='KILLED'></mutations>\n" > "$tmp/malformed.xml"
expect_failure "$tmp/malformed.xml" "mutation: report is not well-formed XML"

: > "$tmp/empty.xml"
expect_failure "$tmp/empty.xml" "mutation: report is not well-formed XML"

echo "mutation controls: pass"
