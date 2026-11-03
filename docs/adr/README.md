# Architecture decisions

| ADR | Decision |
|---|---|
| [0001](0001-layout-toolchain-and-shared-data.md) | Repository layout, Gradle and Java 17, the vendored `shared/` and the generator |
| [0002](0002-value-type-and-generated-tables.md) | The date is a serial and a packed int; ranges hold two serials; the tables are generated |
| [0003](0003-api-conventions-errors-and-nullness.md) | Exceptions and `Optional`, error kinds, Java names, nullness and the clock rule |
| [0004](0004-tests-and-quality-gates.md) | JUnit and jqwik, SpotBugs, JaCoCo 90%, Javadoc, executable documentation |
| [0005](0005-patro-as-a-second-artifact.md) | The patro is a second artifact and a second JPMS module |
| [0006](0006-serialization.md) | Serialization is the canonical text through a serialization proxy |
| [0007](0007-releases-and-maven-central.md) | Maven Central through the Central Portal, the `production` environment and signing |
| [0008](0008-benchmarks-and-competitors.md) | JMH B1 to B8, the Kotlin comparison and the Maven Central competitor |
| [0009](0009-ci-without-the-hub-remote.md) | The nine-cell CI matrix, `shared-check` and the red job until the hub branch merges |
| [0010](0010-mutation-testing.md) | PIT on the core, thresholds in `gradle.properties` |
| [0011](0011-jpms-names-and-packages.md) | Artifact and module names, the `.java` package suffix |
