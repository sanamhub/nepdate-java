# Benchmarks

Time and allocation of the Java port on operations B1 to B8 of
[`shared/spec/BENCHMARK.md`](../shared/spec/BENCHMARK.md), measured with JMH 1.37 through the
[`:benchmarks`](../benchmarks/) module. The Kotlin column is the published
`io.github.sanamhub:nepdate:0.1.0-rc.1` and `nepdate-patro:0.1.0-rc.1`, called from Java in the same
JVM and the same run. The C# column is copied from the hub's
[`docs/benchmarks/dotnet.md`](https://github.com/sanamhub/nepdate/blob/main/docs/benchmarks/dotnet.md)
(.NET 10 column, NepDate 2.0.7, run 2026-11-02). The competitor is
`io.github.shivathapaa:nepali-date-picker-core-jvm` 3.3.0, the only library that passes
[`docs/competitors.md`](competitors.md).

## Machine

| Item | Value |
|---|---|
| CPU | 12th Gen Intel Core i5-12500H (4 performance + 8 efficiency cores, 16 threads, base 3.1 GHz) |
| RAM | 31.7 GB |
| OS | Windows 11 Home 10.0.26300 |
| Power plan | Ultimate Performance |
| Affinity | logical CPU 2 (mask `0x4`), set on the JMH process |
| CPU load before | 6.9 percent average over 30 s, 11.4 percent peak |
| CPU load after | 13.0 percent average over 20 s, 20.1 percent peak |

The machine was otherwise idle. The gate asks for under 15 percent before and after; the averages
meet it, and the 20 percent peak after the run is a background process, not the benchmark.

## Toolchains and versions

| Item | Value |
|---|---|
| nepdae-java | 1.0.0-rc.1 (working tree after the optimization pass) |
| JDK | Temurin 17.0.20.1, `-Xmx1g` |
| JMH | 1.37, compiler blackholes |
| Kotlin port | 0.1.0-rc.1, Kotlin stdlib 2.4.20 |
| shivathapaa nepali-date-picker-core | 3.3.0 (Compose runtime excluded; see competitors) |
| Date of run | 2026-11-03 |

## Command

The classpath is written by the build so the process can be pinned to one core:

```
gradlew.bat :benchmarks:printClasspath
java -Xmx1g -cp "<benchmarks/build/benchmark-classpath.txt>" org.openjdk.jmh.Main ^
  -rf json -rff docs\raw\2026-11-03-final\jmh.json
```

The allocation column comes from a second run of the Java benchmarks in the same session:

```
java -Xmx1g -cp "<the same classpath>" org.openjdk.jmh.Main JavaBenchmark ^
  -prof gc -wi 3 -i 5 -rf json -rff docs\raw\2026-11-03-final\jmh-gc.json
```

Time and bytes are per single operation (`OperationsPerInvocation = 64`). For B8 one operation is a
whole month grid of 29 to 32 `Patro.info` calls. The error is JMH's 99.9 percent confidence
half-width. The raw files are in [`raw/2026-11-03-final/`](raw/2026-11-03-final/):
`jmh.json`, `jmh.log`, `jmh-gc.json`, `jmh-gc.log`, and the three-fork rerun of B7 and B8 in
`jmh-forks.json` and `jmh-forks.log`.

## Results

Before is the run in [`raw/2026-11-03/`](raw/2026-11-03/), from the first complete pass. After is the
run above, with the Kotlin port measured in the same session.

| ID | Operation | Java before | Java after | error | B/op | Kotlin | error | shivathapaa | C# .NET 10 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| B1 | BS to AD | 13.88 | 14.03 | 0.27 | 0 | 15.98 | 0.19 | 919.34 | 13.50 |
| B2 | AD to BS | 8.50 | 6.70 | 0.14 | 0 | 6.77 | 0.11 | 682.02 | 20.10 |
| B3 | add 1000 days | 6.26 | 5.23 | 0.11 | 0 | 5.15 | 0.61 | n/a | 19.05 |
| B4 | add 13 months, Clamp | 11.93 | 5.32 | 0.12 | 0 | 8.77 | 2.37 | n/a | 20.19 |
| B5 | long format, Nepali, weekday | 96.55 | 91.99 | 2.70 | 303 | 99.63 | 4.29 | n/a | 150.41 |
| B6 | strict parse `YYYY/MM/DD` | 30.94 | 27.41 | 0.35 | 0 | 30.39 | 10.57 | n/a | 22.42 |
| B7 | patro info | 2.07 | 1.29 | 0.02 | 0 | 1.11 | 0.04 | none | 48.21 |
| B8 | patro month grid | 81.84 | 55.81 | 1.37 | 0 | 53.87 | 0.59 | none | 3,112.76 |

`? 10??` is what JMH's GC profiler prints when an allocation is statistically indistinguishable
from zero; those rows are reported as 0 B/op. B5 allocates the formatted string, as every port does;
it fell from 415 to 303 B/op in this pass.

The shivathapaa figures carry a large error (370 ns on B1, 92 ns on B2) because its converter builds
more objects; the three-fork run in the earlier record had 649 and 591. It agrees with `shared/data`
on 61 of 64 dates in both directions, differing at BS 2086-07-23, 2088-01-04 and 2089-06-10, so it
is listed, never ranked first.

## Against C#

BENCHMARK section 6 has no Java row. The family's rule for the JVM (Kotlin, and Go and Swift) is
B1, B2 and B7 within 1.5 times C# NepDate with no allocation beyond the date object. Java is 1.04
times C# on B1, 0.33 on B2 and 0.03 on B7, and the date itself is the only allocation in those rows.
B6 is 1.22 times C#, inside the same rule if it is applied to every row. Passes.

## Against the Kotlin port

| Row | Java / Kotlin | Reading |
|---|---:|---|
| B1 | 0.88 | Java is faster. |
| B2 | 0.99 | Equal within the intervals. The `JAN1`/`CUM` table replaced the Hinnant division path. |
| B3 | 1.02 | Equal within the intervals; both decode the serial once. |
| B4 | 0.61 | The month step now computes the serial from the target month index instead of rebuilding year and month and calling `monthLength`. |
| B5 | 0.92 | The cached enum arrays removed the `values()` clone from `Month.of` and `Weekday.ofIndex`, and the format allocates 112 bytes less. |
| B6 | 0.90 | Parsing a known date now packs the y/m/d instead of decoding the serial back. |
| B7 | 1.16 | 0.18 ns slower. The lookup is one subtraction and one array read; the difference is the list-size access in `CalendarInfo` and the object layout of the cached entries, not a branch in the lookup. B8, which performs 32 lookups per operation, shows 0.06 ns per day, so the single-call benchmark overstates it. |
| B8 | 1.04 | 1.9 ns (3.6 percent) slower per month, 0.06 ns per day. The range iterator allocates one cursor per month and one date per day, as the Kotlin `AbstractList` iterator does; the remaining cost is the iterator call pair, not the lookup. |

Every row is at or below Kotlin except B7 and B8, which are 0.18 ns and 1.9 ns above it; both are
recorded as the next optimization pass rather than as a design difference. The three-fork rerun in
`jmh-forks.json` measures B7 at 1.402 against 1.018 and B8 at 52.5 against 48.7, the same order of
difference.

## What changed in this pass

- BS to AD conversion uses the `JAN1` year-start table and the `CUM` month table for
  `ofGregorian`, which removes two divisions from B2.
- `NepaliDate` has a two-argument constructor that takes the serial and the packed date, so the
  factories that start from a year, month and day no longer decode the serial they just computed
  (B1, B3, B4, B6).
- `plusMonths` adds the month to the month index and reads `MONTH_START`, instead of rebuilding the
  target year and calling `monthLength` (B4).
- `Month.of` and `Weekday.ofIndex` index a cached array rather than calling `values()`, which clones
  (B5).
- `Patro.info` computes the offset from the epoch day with a constant and reads a constant span, and
  the range's plain iterator is a two-field cursor instead of the filtered cursor (B7, B8).
- The benchmark inputs gained prepared patro dates for the Java side, so B7 and B8 no longer build a
  date inside the measured loop, and B3, B4 and B6 read the same three fields as the Kotlin
  benchmark instead of `toYmd()`.

## Notes

- The Kotlin comparison uses the published 0.1.0-rc.1 artifact. Its `toYmd()` accessor, present in
  the Kotlin repository's current source, is not in that release, so the Kotlin benchmark consumes
  the date through `getYear()`, `getMonthValue()` and `getDayOfMonth()`, which exist in both.
- The two ports share one `BenchmarkInputs`, so the 64 dates are identical for every column.
- The date of the before and after runs is the same; they are different sessions, so the before
  column is for the direction of the change, and the Kotlin column in the after row is the
  same-session comparison that the rule asks for.
