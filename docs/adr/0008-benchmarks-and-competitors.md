# ADR-0008: Benchmarks and the Kotlin comparison

- **Status:** accepted
- **Date:** 2026-11-03

## Context

Hub `spec/BENCHMARK.md` requires the same eight operations on the same 64 dates for every port, a
comparison with the ecosystem's most-used mature Bikram Sambat libraries selected by section 7, and,
for this port specifically, a first comparison against the published Kotlin artifact called from
Java. Java and Kotlin run on the same JVM, so the comparison can be exact: one JMH run, one
classpath, the same inputs.

## Decision

1. **JMH 1.37 in the `benchmarks/` project**, with the annotation processor and no wrapper plugin.
   One benchmark class calls this port and one calls `io.github.sanamhub:nepdate:0.1.0-rc.1` and its
   patro artifact. `BenchmarkInputs` computes the 64 dates once, so both classes use the same arrays
   and the same patro dates.
2. **The operations are B1 to B8 verbatim,** `@OperationsPerInvocation(64)` so a score is one
   operation, `AverageTime` in nanoseconds, five warm-up and ten measured one-second iterations, one
   fork. `-prof gc` is not used because the JDK 17 GC profiler adds noise; allocation is reported
   from a separate counting run when a row needs it.
3. **The run is pinned to logical CPU 2** (affinity mask `0x4`), the reference machine's performance
   core, and the CPU load is sampled for 30 seconds before and after. A run on a loaded machine is
   reported as preliminary and never judged.
4. **The competitor set is the Maven Central one.** `io.github.shivathapaa:nepali-date-picker-core`
   3.3.0 is the only candidate that passes section 7 (one deps.dev dependent, first release
   2024-09-14, a release in 2026, MPL-2.0). The harness measures B1 and B2 for it and checks its
   answers on the 64 dates, because a fast wrong answer is listed and never ranked first.
5. **The Kotlin comparison is a finding, not a competition.** If the Kotlin artifact is slower on a
   row by more than noise, the report names the likely cause (null-check intrinsics, boxing,
   `Companion` or `@JvmStatic` dispatch, inline classes) and files it for the Kotlin maintainers.
   This port never changes the Kotlin repository.
6. **The results live in `docs/benchmarks.md`** with the machine, toolchains, command, raw files and
   the hub's C# column, and the older figures are kept in `docs/raw/` so a rerun can be compared.

## Consequences

- One command reproduces both columns: the `jmh` task or the documented `java -cp` line.
- The benchmark module is not published and its dependencies do not reach a POM.
- The numbers are comparable with the other ports because the inputs, warm-up and measurement
  settings are the family's.

## Alternatives considered

- **kotlinx-benchmark or a JMH Gradle plugin:** another plugin to pin and another way to build the
  classpath; the annotation processor is enough.
- **Comparing against the Kotlin sources built in this repository:** the requirement is the
  published artifact, which is what a user gets.
- **A second machine for Java:** the family's numbers must share one machine to be comparable.
