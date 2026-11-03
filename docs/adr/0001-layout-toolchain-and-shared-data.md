# ADR-0001: Repository layout, toolchain and the shared data

- **Status:** accepted
- **Date:** 2026-11-03

## Context

The Java port is the eleventh SDK of the NepDate family. It must follow the hub's specification and
vectors exactly, release core and patro together from one tag (hub ADR-0004), and give Java users a
library that does not pull the Kotlin standard library onto their classpath. The ten existing ports
show the layout that passes review: a vendored `shared/` copy, a generator for the tables, one CI
command, and a test suite that reads every vector.

## Decision

1. **One Gradle build, two published projects.** `nepdate/` is the core and `nepdate-patro/` the
   patro; both publish to Maven Central. `codegen/` writes the tables and is never published.
   `benchmarks/` holds the JMH harness. The Gradle project names are `nepdate-java` and
   `nepdate-java-patro`, because Gradle substitutes a module dependency whose `group:name` matches a
   project in the build, and the benchmark harness must resolve the published Kotlin artifact.
2. **Gradle with the Kotlin DSL and the wrapper**, 9.6.0, like the Kotlin port, so the Central
   publishing and signing setup is the same shape. Every version is pinned in
   `gradle/libs.versions.toml` and at least 14 days old when chosen.
3. **Java 17 floor.** The sources use records, `java.time`, `switch` expressions and the module
   system; the bytecode target is 17. CI compiles and tests on 17, 21 and 25 through a toolchain
   property, so a newer JDK is exercised without moving the floor. The Kotlin port keeps covering
   Kotlin and Android, including Android versions without `java.time`.
4. **No runtime dependency.** JSpecify is `compileOnly` and adds nothing at run time. The code
   generator uses no JSON library: `shared/data` is read by a small parser kept in `codegen/`, so
   the repository has no dependency that could reach a published POM.
5. **Shared data is vendored.** `shared/` holds the hub's `data/` and `spec/` at the commit in
   `shared/VERSION`, copied by `tools/shared_sync.py` and checked by the hub's reusable
   `shared-check` workflow. It is never edited by hand.
6. **Codegen is Java too.** `./gradlew :codegen:run` writes the tables and
   `./gradlew :codegen:checkTables` compares them byte for byte. A data update therefore changes the
   generated files and nothing else, and a stale table fails CI.

## Consequences

- A user adds one or two coordinates with no transitive dependency.
- A data update is a `shared-sync` pull request plus a regenerated table, reviewed as a diff.
- The build needs a JDK only; the wrapper downloads Gradle itself.
- Two JDKs are needed in CI for the matrix, which the toolchain resolver provides.

## Alternatives considered

- **One module with an optional patro package:** the core would carry the patro strings, which hub
  ADR-0004 rejects, and the two artifacts could not release independently within the family line.
- **Maven instead of Gradle:** the publishing plugin and signing setup the family uses is Gradle
  based; a second build system would be a second thing to keep correct.
- **A separate repository for the patro:** a second sync, CI and release pipeline for the same hub
  data, which hub ADR-0004 decision 3 rejects.
