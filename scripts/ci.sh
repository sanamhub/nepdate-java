#!/usr/bin/env bash
# The CI command (ADR-0004): runs the gates in order and stops at the first failure.
# .github/workflows/ci.yml runs this script, so a green local run means a green CI run.
set -euo pipefail

cd "$(dirname "$0")/.."

# PIT is not in this list: ./scripts/mutation.sh takes about four minutes, so mutation.yml runs it
# on a schedule. It is not part of the per-change gate.
version="$(bash scripts/release-check.sh --print)"
echo "ci: version $version"

gates=(
  "exec-bits"
  "BUILD"
  "JAVADOC"
  "CODEGEN"
  "DRY-RUN"
  "CORE-ALONE"
  "CHECK-DOCS"
  "RELEASE-CHECK"
)

passed=0
for gate in "${gates[@]}"; do
  case "$gate" in
    exec-bits)
      # A script committed from Windows loses its executable bit, and CI then fails with exit 126.
      echo "ci: every committed *.sh and gradlew is executable"
      if git ls-files -s -- '*.sh' '*gradlew' | grep -v '^100755'; then
        echo "ci: the files above are not executable in git; run git update-index --chmod=+x <file>"
        exit 1
      fi
      ;;
    BUILD)
      # The module check runs formatting, SpotBugs, both test suites, the coverage gates and the
      # Javadoc task through the projects' check tasks; the benchmarks must compile as well.
      echo "ci: ./gradlew :nepdate-java:check :nepdate-java-patro:check :benchmarks:classes"
      ./gradlew --console=plain :nepdate-java:check :nepdate-java-patro:check :benchmarks:classes
      ;;
    JAVADOC)
      # check already runs javadoc; this call makes the gate explicit for a reader of the log. The
      # task runs with -Xdoclint:all -Werror (ADR-0004 gate 5).
      echo "ci: ./gradlew :nepdate-java:javadoc :nepdate-java-patro:javadoc"
      ./gradlew --console=plain :nepdate-java:javadoc :nepdate-java-patro:javadoc
      ;;
    CODEGEN)
      echo "ci: ./gradlew :codegen:checkTables :codegen:test"
      ./gradlew --console=plain :codegen:checkTables :codegen:test
      ;;
    DRY-RUN)
      # The release dry run: pack, sign nothing, and check the file list. It catches a missing
      # sources or javadoc jar before a tag does.
      echo "ci: ./gradlew publishToMavenLocal, then the expected files"
      ./gradlew --console=plain publishToMavenLocal
      m2="$HOME/.m2/repository/io/github/sanamhub"
      for artifact in nepdate-java nepdate-java-patro; do
        dir="$m2/$artifact/$version"
        if [ ! -d "$dir" ]; then
          echo "ci: $dir was not installed"
          exit 1
        fi
        for suffix in .jar -sources.jar -javadoc.jar .pom; do
          if [ ! -f "$dir/$artifact-$version$suffix" ]; then
            echo "ci: missing $artifact-$version$suffix"
            exit 1
          fi
        done
      done
      ;;
    CORE-ALONE)
      # Building the core must not touch the patro project.
      echo "ci: ./gradlew :nepdate-java:build --dry-run (no :nepdate-java-patro: task)"
      plan="$(./gradlew --console=plain :nepdate-java:build --dry-run)"
      if grep -q ":nepdate-java-patro:" <<<"$plan"; then
        echo "ci: :nepdate-java:build runs a :nepdate-java-patro: task"
        exit 1
      fi
      ;;
    CHECK-DOCS)
      echo "ci: ./scripts/check-docs.sh"
      ./scripts/check-docs.sh
      ;;
    RELEASE-CHECK)
      # The release preflight's own cases, then the repository in draft mode: the changelog has only
      # the Unreleased section until the bump pull request.
      echo "ci: ./scripts/test-release-check.sh && ./scripts/release-check.sh --draft"
      ./scripts/test-release-check.sh
      ./scripts/release-check.sh --draft
      ;;
    *)
      echo "ci: unknown gate $gate"
      exit 1
      ;;
  esac
  passed=$((passed + 1))
done

echo "ci: $passed/${#gates[@]} gates passed"
