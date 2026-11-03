#!/usr/bin/env bash
# Mutation gate: PIT on the core only, with the thresholds in gradle.properties (ADR-0010).
# A run takes about four minutes, so mutation.yml runs it on a schedule and scripts/ci.sh leaves it
# out.
set -euo pipefail

cd "$(dirname "$0")/.."

# The report override and the cached flag exist for the controls in scripts/test-mutation.sh.
report="${NEPDATE_MUTATION_REPORT:-nepdate/build/reports/pitest/mutations.xml}"
cached="${NEPDATE_MUTATION_CACHED:-0}"
threshold="$(sed -n 's/^nepdateMutationThreshold=//p' gradle.properties)"
max_surviving="$(sed -n 's/^nepdateMaxSurviving=//p' gradle.properties)"
if [ -z "$threshold" ] || [ -z "$max_surviving" ]; then
  echo "mutation: nepdateMutationThreshold or nepdateMaxSurviving is missing from gradle.properties"
  exit 1
fi

log="$(mktemp)"
trap 'rm -f "$log"' EXIT

status=0
if [ "$cached" = "1" ]; then
  echo "mutation: using cached report ${report}"
else
  set +e
  ./gradlew --console=plain :nepdate-java:pitest 2>&1 | tee "$log"
  status=${PIPESTATUS[0]}
  set -e
fi

if [ "$status" -ne 0 ]; then
  echo "mutation: PIT failed with exit code ${status}"
  exit "$status"
fi
if [ ! -f "$report" ]; then
  echo "mutation: report not found at ${report}"
  exit 1
fi
if ! grep -q '^<mutations' "$report" || ! grep -q '^</mutations>' "$report"; then
  echo "mutation: report is not well-formed XML: ${report}"
  exit 1
fi

count() {
  grep -c "$1" "$report" || true
}

total="$(count '<mutation ')"
statuses="$(count "status='")"
killed="$(count "status='KILLED'")"
timed_out="$(count "status='TIMED_OUT'")"
memory_error="$(count "status='MEMORY_ERROR'")"
survived="$(count "status='SURVIVED'")"
no_coverage="$(count "status='NO_COVERAGE'")"
# PIT counts a timed-out and a memory-error mutant as detected in its own score, so the gate does
# the same; the report still shows both counts so a rise cannot hide in the total.
detected=$((killed + timed_out + memory_error))
missed=$((survived + no_coverage))
unclassified=$((total - detected - missed))

if [ "$total" -eq 0 ]; then
  echo "mutation: report has no mutations at ${report}"
  exit 1
fi
if [ "$statuses" -ne "$total" ]; then
  echo "mutation: report is not well-formed XML: ${report}"
  exit 1
fi
if [ "$unclassified" -ne 0 ]; then
  echo "mutation: ${unclassified} mutations have a status the gate does not classify"
  exit 1
fi

score=$((detected * 100 / total))

# PIT prints this line on a real run. A restored-from-cache report prints nothing, so the XML is
# the source of truth and the console is a cross-check when it is present.
line="$(grep -E '^>> Generated [0-9]+ mutations Killed [0-9]+ \([0-9]+%\)$' "$log" | tail -n 1 || true)"
if [ -n "$line" ]; then
  read -r console_total console_detected console_score <<< "$(printf '%s\n' "$line" |
    sed -E 's/^>> Generated ([0-9]+) mutations Killed ([0-9]+) \(([0-9]+)%\)$/\1 \2 \3/')"
  if [ "$console_total" -ne "$total" ] || [ "$console_detected" -ne "$detected" ] ||
    [ "$console_score" -ne "$score" ]; then
    echo "mutation: XML report (${detected}/${total}, ${score}%) disagrees with PIT (${console_detected}/${console_total}, ${console_score}%)"
    exit 1
  fi
fi

if [ "$missed" -gt "$max_surviving" ]; then
  echo "mutation: ${missed} missed mutations exceeds max surviving ${max_surviving}"
  exit 1
fi
if [ "$score" -lt "$threshold" ]; then
  echo "mutation: score ${score}% is below threshold ${threshold}%"
  exit 1
fi

echo "mutation: score ${score}% (threshold ${threshold}%, max surviving ${max_surviving}, timed out ${timed_out}, memory errors ${memory_error})"

