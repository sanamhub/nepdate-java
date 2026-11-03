#!/usr/bin/env bash
# Docs gate: prose rules, links, the API inventory and no machine paths.
set -euo pipefail

cd "$(dirname "$0")/.."

files=(
  README.md CHANGELOG.md CONTRIBUTING.md SECURITY.md CODE_OF_CONDUCT.md
  docs/*.md docs/adr/*.md docs/runbooks/*.md docs/mutation/*.md
)
issues=0

fail() {
  echo "check-docs: $1"
  issues=$((issues + 1))
}

# 1. No em dashes (U+2014).
if grep -n '—' "${files[@]}" 2>/dev/null; then
  fail "em dash found (use 'to', a comma, or parentheses instead)"
fi

# 2. No machine paths in committed prose.
if grep -nE '[A-Za-z]:[\\/](github|devcache|Users)|/d/devcache' "${files[@]}" 2>/dev/null; then
  fail "a machine path is in the documentation"
fi

# 3. No task IDs, AI attribution or working-notes language in the public repository.
if grep -nEi '\b[A-Z][0-9]-[0-9]{2}\b|checkpoint|\bAI\b|\bagent\b' "${files[@]}" 2>/dev/null; then
  fail "a task ID, an AI word or 'checkpoint' is in the documentation"
fi

# 4. Every relative markdown link resolves.
for f in "${files[@]}"; do
  dir="$(dirname "$f")"
  while IFS= read -r link; do
    [ -z "$link" ] && continue
    path="${link%%#*}"
    case "$path" in
      http://*|https://*|mailto:*|'') continue ;;
    esac
    if [ ! -e "$dir/$path" ]; then
      fail "broken link in $f -> $link"
    fi
  done < <(grep -oE '\]\([^)]+\)' "$f" | sed -e 's/^](//' -e 's/)$//')
done

# 5. Every public type of the two published projects is named in docs/API.md.
types="$(grep -RhE '^public (final |abstract )?(class|enum|record|interface) ' \
  nepdate/src/main/java nepdate-patro/src/main/java |
  sed -E 's/.*(class|enum|record|interface) ([A-Za-z0-9_]+).*/\2/' | sort -u)"
for type in $types; do
  if ! grep -q "\b$type\b" docs/API.md; then
    fail "docs/API.md does not document the public type $type"
  fi
done

# 6. The README names the two artifacts, the hub, the C# original and the licence.
for needle in 'io.github.sanamhub:nepdate-java' 'io.github.sanamhub:nepdate-java-patro' \
  'sanamhub/nepdate' 'RajuPrasai/NepDate' 'MIT'; do
  if ! grep -qF "$needle" README.md; then
    fail "README.md does not mention $needle"
  fi
done

# 7. The changelog has an Unreleased section, which is what the draft release check accepts.
if ! grep -q '^## \[Unreleased\]' CHANGELOG.md; then
  fail "CHANGELOG.md has no Unreleased section"
fi

# 8. The API document groups the same eight areas as the hub specification.
for group in 'Dates, identity and comparison' 'Conversion and serialization' 'Arithmetic' \
  'Formatting' 'Parsing' 'Ranges' 'Fiscal year' 'Patro'; do
  if ! grep -qF "$group" docs/API.md; then
    fail "docs/API.md has no $group section"
  fi
done

if [ "$issues" -gt 0 ]; then
  echo "check-docs: $issues issue(s)"
  exit 1
fi
echo "check-docs: docs pass"
