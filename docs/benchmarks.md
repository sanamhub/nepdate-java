# Benchmarks

Time and allocation of the Java port on operations B1 to B8 of
[`shared/spec/BENCHMARK.md`](../shared/spec/BENCHMARK.md), measured with JMH 1.37 through the
[`:benchmarks`](../benchmarks/) module. The Kotlin column is the published
`io.github.sanamhub:nepdate:0.1.0-rc.1` and `nepdate-patro:0.1.0-rc.1`, called from Java in the same
JVM and the same run. The competitor is
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
| Idle gate before the main run | 4.1 percent average, 8.8 peak (30 one-second samples) |
| Load during the main run | 9.6 percent average, 15.3 peak over 284 one-second samples; 3.6 and 10.0 on the CPUs other than CPU 2 |
| Idle gate before the allocation run | 4.7 percent average, 8.1 peak |
| Load during the allocation run | 9.4 percent average, 13.5 peak over 69 one-second samples |

The machine was idle: no other build, test suite or benchmark ran during either run, and the Gradle
daemon was stopped after the classpath was built. The load during a run includes the benchmark's own
pinned core and the sampler. The samples of the main run are in
[`raw/2026-11-16/cpu-samples.txt`](raw/2026-11-16/cpu-samples.txt).

## Toolchains and versions

| Item | Value |
|---|---|
| nepdate-java | 1.0.0-rc.1 |
| JDK | Temurin 17.0.21+9, `-Xmx1g` |
| JMH | 1.37, compiler blackholes |
| Kotlin port | 0.1.0-rc.1, Kotlin stdlib 2.4.20 |
| shivathapaa nepali-date-picker-core | 3.3.0 (Compose runtime excluded; see competitors) |
| Date of run | 2026-11-16 |

## Command

The classpath is written by the build so the process can be pinned to one core:

```
gradlew.bat :benchmarks:printClasspath
java -Xmx1g -cp "<benchmarks/build/benchmark-classpath.txt>" org.openjdk.jmh.Main ^
  -rf json -rff docs\raw\2026-11-16\jmh.json
```

The allocation column comes from a second run of the Java benchmarks in the same session:

```
java -Xmx1g -cp "<the same classpath>" org.openjdk.jmh.Main JavaBenchmark ^
  -prof gc -wi 3 -i 5 -rf json -rff docs\raw\2026-11-16\jmh-gc.json
```

Both are started with `start /affinity 4 /wait /b`, which pins the process to logical CPU 2. Time
and bytes are per single operation (`OperationsPerInvocation = 64`). For B8 one operation is a
whole month grid of 29 to 32 `Patro.info` calls. The error is JMH's 99.9 percent confidence
half-width. The raw files are in [`raw/2026-11-16/`](raw/2026-11-16/): `jmh.json`, `jmh.log`,
`jmh-gc.json`, `jmh-gc.log` and the load samples.

## Results

Previous is the run of 2026-11-03 in [`raw/2026-11-03-final/`](raw/2026-11-03-final/). Now is the run
above, with the Kotlin port measured in the same session.

The comparison with C# NepDate, the original library, and the target check from `spec/BENCHMARK.md` section 6 are in the hub: [performance page](https://nepdate.com/docs/project/performance/).

| ID | Operation | Java previous | Java now | error | B/op | Kotlin | error | shivathapaa |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| B1 | BS to AD | 14.03 | 12.23 | 0.03 | 0 | 13.94 | 0.05 | 641.89 |
| B2 | AD to BS | 6.70 | 5.78 | 0.05 | 0 | 5.91 | 0.02 | 656.91 |
| B3 | add 1000 days | 5.23 | 4.55 | 0.01 | 0 | 4.06 | 0.01 | n/a |
| B4 | add 13 months, Clamp | 5.32 | 4.56 | 0.01 | 0 | 6.02 | 0.02 | n/a |
| B5 | long format, Nepali, weekday | 91.99 | 30.18 | 0.21 | 223 | 83.61 | 0.69 | n/a |
| B6 | strict parse `YYYY/MM/DD` | 27.41 | 23.46 | 0.14 | 0 | 21.38 | 0.11 | n/a |
| B7 | patro info | 1.29 | 1.10 | 0.01 | 0 | 0.95 | 0.00 | none |
| B8 | patro month grid | 55.81 | 48.94 | 0.15 | 0 | 46.54 | 0.95 | none |

B/op is 0 where JMH's GC profiler reports no allocation. B5 allocates the formatted string, as every
port does: 223 B/op now, 303 B/op in the previous run.

The shivathapaa figures have an error of 4.18 ns on B1 and 3.75 ns on B2 in this run (370 and 92 ns
in the previous one). It agrees with `shared/data` on 61 of 64 dates in both directions, differing at
BS 2086-07-23, 2088-01-04 and 2089-06-10, so it is listed, never ranked first.

## Against the Kotlin port

| Row | Java / Kotlin | Reading |
|---|---:|---|
| B1 | 0.88 | Java is faster. |
| B2 | 0.98 | Equal within the intervals. The `JAN1`/`CUM` table replaced the Hinnant division path. |
| B3 | 1.12 | Kotlin is 0.49 ns faster. |
| B4 | 0.76 | The month step computes the serial from the target month index instead of rebuilding year and month and calling `monthLength`. |
| B5 | 0.36 | The cached enum arrays removed the `values()` clone from `Month.of` and `Weekday.ofIndex`; Kotlin's figure is the published 0.1.0-rc.1 artifact. |
| B6 | 1.10 | Kotlin is 2.08 ns faster. |
| B7 | 1.16 | Kotlin is 0.15 ns faster. |
| B8 | 1.05 | Kotlin is 2.4 ns faster per month, about 0.08 ns per day. |

Java is at or below Kotlin on B1, B2, B4 and B5. Kotlin is ahead on B3, B6, B7 and B8 by 0.15 to
2.4 ns; none of these is a design difference, and they are recorded for the next optimization pass.

## What changed in the optimization pass of 2026-11-03

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
- The previous column is the 2026-11-03 session and is there for the direction of the change; the
  Kotlin column is the same-session comparison that the rule asks for.
