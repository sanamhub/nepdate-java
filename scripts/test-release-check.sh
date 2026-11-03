#!/usr/bin/env bash
# The release preflight's own cases: each fixture must pass or fail for the stated reason. The
# script under test is scripts/release-check.sh; nothing here touches the real repository.
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

fixture() {
  local dir="$1" version="$2" changelog="$3"
  mkdir -p "$dir"
  printf 'version=%s\n' "$version" > "$dir/gradle.properties"
  printf '%s\n' "$changelog" > "$dir/CHANGELOG.md"
}

expect_ok() {
  local description="$1"
  shift
  if ! "$@" > "$tmp/out" 2>&1; then
    printf 'expected pass: %s\n%s\n' "$description" "$(cat "$tmp/out")" >&2
    exit 1
  fi
}

expect_failure() {
  local description="$1" message="$2"
  shift 2
  local status=0
  "$@" > "$tmp/out" 2>&1 || status=$?
  if [ "$status" -eq 0 ]; then
    printf 'expected failure: %s\n' "$description" >&2
    exit 1
  fi
  if ! grep -qF "$message" "$tmp/out"; then
    printf 'expected message for %s: %s\ngot:\n%s\n' "$description" "$message" "$(cat "$tmp/out")" >&2
    exit 1
  fi
}

released="$tmp/released"
fixture "$released" 1.0.0-rc.1 $'# Changelog\n\n## [Unreleased]\n\n- Something.\n\n## [1.0.0-rc.1] - 2026-11-03\n\n- First release.'

draft="$tmp/draft"
fixture "$draft" 1.0.0-rc.1 $'# Changelog\n\n## [Unreleased]\n\n- The first release is being prepared.'

empty="$tmp/empty"
fixture "$empty" 1.0.0-rc.1 $'# Changelog\n\n## [Unreleased]\n'

bad_version="$tmp/bad-version"
fixture "$bad_version" 1.0 $'## [Unreleased]\n\n- Something.'

bad_suffix="$tmp/bad-suffix"
fixture "$bad_suffix" 1.0.0-alpha.1 $'## [Unreleased]\n\n- Something.'

# The dry run accepts the Unreleased heading and prints the version.
expect_ok "--draft with Unreleased" bash "$script_dir/release-check.sh" --draft "" "$draft"
expect_ok "--print with Unreleased" bash "$script_dir/release-check.sh" --print "$draft"
if [ "$(bash "$script_dir/release-check.sh" --print "$draft")" != "1.0.0-rc.1" ]; then
  echo "release-check controls: --print printed the wrong version" >&2
  exit 1
fi

# A release needs the dated heading, and the tag must match.
expect_failure "strict mode with only Unreleased" \
  "CHANGELOG.md has no '## [1.0.0-rc.1] - YYYY-MM-DD' section" \
  bash "$script_dir/release-check.sh" v1.0.0-rc.1 "$draft"
expect_ok "strict mode with the dated heading" \
  bash "$script_dir/release-check.sh" v1.0.0-rc.1 "$released"
expect_failure "a tag that does not match the version" \
  "does not match gradle.properties version 1.0.0-rc.1" \
  bash "$script_dir/release-check.sh" v2.0.0 "$released"
expect_failure "an empty Unreleased section" \
  "has neither" \
  bash "$script_dir/release-check.sh" --draft "" "$empty"
expect_failure "a version without a patch number" \
  "is not X.Y.Z" \
  bash "$script_dir/release-check.sh" --print "$bad_version"
expect_failure "a pre-release suffix other than beta or rc" \
  "pre-release suffix" \
  bash "$script_dir/release-check.sh" --print "$bad_suffix"

echo "release-check controls: pass"
