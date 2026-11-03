#!/usr/bin/env bash
# Installs both artifacts into mavenLocal and consumes them from a throwaway project, the way a
# user resolves the published coordinates. Run from the repository root.
set -euo pipefail

cd "$(dirname "$0")/../.."

version="$(bash scripts/release-check.sh --print)"
./gradlew --console=plain publishToMavenLocal
./gradlew --console=plain -p tools/consumer-check run -PnepdateVersion="$version"
