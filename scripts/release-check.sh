#!/usr/bin/env bash
# Release preflight (ADR-0007): the version in gradle.properties is a SemVer version with an
# optional -beta.N or -rc.N suffix; the tag, when given, is "v" plus that version; and the changelog
# carries the version's own dated section.
#
#   scripts/release-check.sh v1.0.0-rc.1          the release: the dated heading is required
#   scripts/release-check.sh --draft [TAG]        CI and the dry run: accepts Unreleased
#   scripts/release-check.sh --print              print the version after validating it
#
# --draft accepts "## [Unreleased]" with a non-empty body, because the bump pull request has not
# added the dated heading yet. It does not accept an empty Unreleased section.
set -euo pipefail

mode=strict
if [ "${1:-}" = "--print" ]; then
  mode=print
  shift
elif [ "${1:-}" = "--draft" ]; then
  mode=draft
  shift
fi

root="."
tag=""
if [ "$mode" = "print" ]; then
  root="${1:-.}"
else
  tag="${1:-}"
  root="${2:-.}"
fi

version="$(grep '^version=' "$root/gradle.properties" | cut -d= -f2 | tr -d '[:space:]')"
ident='(0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)'
pattern="^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-$ident(\.$ident)*)?$"

if ! printf '%s' "$version" | grep -Eq "$pattern"; then
  echo "release-check: gradle.properties version '$version' is not X.Y.Z or X.Y.Z-PRERELEASE"
  exit 1
fi
if ! printf '%s' "$version" | grep -Eq '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-(beta|rc)\.[1-9][0-9]*)?$'; then
  echo "release-check: $version uses a pre-release suffix other than -beta.N or -rc.N"
  exit 1
fi
if [ -n "$tag" ] && [ "$tag" != "v$version" ]; then
  echo "release-check: tag '$tag' does not match gradle.properties version $version (want v$version)"
  exit 1
fi

# --print is the CI version reader: it validates the manifest and, when given, the tag, but it must
# work before the changelog heading exists.
if [ "$mode" = "print" ]; then
  printf '%s\n' "$version"
  exit 0
fi

dated_heading() {
  # index() compares literally, so the dots in the version are not regex wildcards.
  awk -v v="$version" '
    index($0, "## [" v "] - ") == 1 &&
      substr($0, length("## [" v "] - ") + 1) ~ /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$/ { found = 1 }
    END { exit found ? 0 : 1 }
  ' "$root/CHANGELOG.md"
}

unreleased_heading() {
  awk '
    /^## \[Unreleased\]/ { found = 1; next }
    found && /^## \[/ { exit }
    found && /[^[:space:]]/ { body = 1 }
    END { exit (found && body) ? 0 : 1 }
  ' "$root/CHANGELOG.md"
}

if [ "$mode" = "draft" ]; then
  if ! dated_heading && ! unreleased_heading; then
    echo "release-check: CHANGELOG.md has neither '## [$version] - YYYY-MM-DD' nor a non-empty '## [Unreleased]'"
    exit 1
  fi
else
  if ! dated_heading; then
    echo "release-check: CHANGELOG.md has no '## [$version] - YYYY-MM-DD' section"
    exit 1
  fi
fi

echo "release-check: $version ok"
