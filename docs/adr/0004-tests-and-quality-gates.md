# ADR-0004: Tests and quality gates

- **Status:** accepted
- **Date:** 2026-11-03

## Context

The family's review standard is that every claim has a test or a log line, every gate has a
negative control, and no gate is lowered to make a build green. The Java ecosystem offers several
choices for each gate, and the ten ports already show which ones survive review.

## Decision

1. **JUnit 5 for the vector tests and jqwik for the properties.** A vector file is read row by row,
   its column names are asserted, and every row is one parameterized invocation, so a failure names
   the row. jqwik covers the invariants over all dates: serial round trip, step and back, clamping,
   split coverage, antisymmetry of the day difference and reconstruction from the calendar
   breakdown.
2. **SpotBugs, not Error Prone.** Error Prone 2.50 is compiled for Java 21 bytecode and cannot run
   inside a Java 17 compiler, so it would silently disappear from the 17 job that defines the floor.
   SpotBugs analyses the compiled classes at `effort=MAX` and `reportLevel=LOW`, runs on every JDK in
   the matrix, and its findings are fixed rather than filtered. The only filter is the two generated
   data classes, which are checked byte for byte by `:codegen:checkTables`.
3. **JaCoCo line coverage at 90% per published project.** The generated tables are excluded because
   they are data, and `docs/mutation/equivalent.md` records that decision with the reason. The gate
   lives in `gradle.properties` (`nepdateCoverageMinimum`) so CI and a local run use the same
   number. The measured coverage is higher and reported in the pull request.
4. **Spotless with google-java-format.** Formatting is a check, not a style discussion. The
   generated tables are excluded because `:codegen:checkTables` owns their exact bytes.
5. **Javadoc is a gate.** `javadoc` runs with `-Xdoclint:all -Werror`, so a public member without a
   comment fails the javadoc jar that Maven Central serves. `docs/API.md` is the narrative copy of
   the same surface.
6. **The documentation is executable.** `DocsExamplesTest` reads every ```java block from the README
   and `docs/API.md`, hoists its imports, wraps it in a `main`, compiles it in memory and runs it. A
   line ending in `// => value` states what that line prints, and the test compares stdout line by
   line. A sample that prints something else, or throws, fails CI.
7. **The generator has a check mode and its own tests.** `:codegen:checkTables` fails when a table is
   stale, and `:codegen:test` covers the JSON reader, the renderers and the drift detector itself.
8. **One command runs the gates.** `scripts/ci.sh` is the list CI runs; the summary line counts the
   gates that passed, and a missing tool or file fails instead of being skipped. The script has
   tests of its own for the release check (`scripts/test-release-check.sh`), including the cases
   that must fail.
9. **The core is isolated from the patro.** `checkCoreSplit` fails if the core jar contains the
   patro's marker string or refers to the patro package, and if the patro jar lacks the marker, so
   the check cannot pass by accident.

## Consequences

- The 17 job runs the same static analysis as 21 and 25; no gate is conditional on the JDK.
- A documentation change that breaks a sample fails the build on all three operating systems.
- The gate numbers are in the repository, not in a CI variable, so a reviewer can reproduce them.

## Alternatives considered

- **Error Prone:** more checks, but unusable on the 17 floor, which is the version the artifact
  targets.
- **Checkstyle or PMD:** overlap with google-java-format and SpotBugs, and a second configuration to
  maintain.
- **A documentation linter instead of running samples:** it cannot catch a wrong printed value,
  which is the failure the README had in earlier ports.
