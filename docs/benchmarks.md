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
| CPU load before | 5.1 percent average over 30 s, 7.7 percent peak |
| CPU load after | 4.4 percent average over 12 s |

The machine was otherwise idle, so these numbers are not marked preliminary.

## Toolchains and versions

| Item | Value |
|---|---|
| nepdae-java | 1.0.0-rc.1 (working tree at the measured commit) |
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
  -rf json -rff docs\raw\2026-11-03\jmh.json
```

The allocation column comes from a second run of the Java benchmarks in the same session:

```
java -Xmx1g -cp "<the same classpath>" org.openjdk.jmh.Main JavaBenchmark ^
  -prof gc -wi 3 -i 5 -rf json -rff docs\raw\2026-11-03\jmh-gc.json
```

Time and bytes are per single operation (`OperationsPerInvocation = 64`). For B8 one operation is a
whole month grid of 29 to 32 `Patro.info` calls. The error is JMH's 99.9 percent confidence
half-width. The raw files are [`raw/2026-11-03/jmh.json`](raw/2026-11-03/jmh.json),
[`raw/2026-11-03/jmh.log`](raw/2026-11-03/jmh.log), [`raw/2026-11-03/jmh-gc.json`](raw/2026-11-03/jmh-gc.json)
and [`raw/2026-11-03/jmh-gc.log`](raw/2026-11-03/jmh-gc.log).

## Results

| ID | Operation | nepdae-java ns/op | error | B/op | Kotlin ns/op | error | shivathapaa ns/op | agrees | C# .NET 10 ns/op |
|---|---:|---:|---:|---:|---:|---:|---:|---|---:|
| B1 | BS to AD | 13.88 | 0.40 | 0 | 15.08 | 0.73 | 649.45 | no (61 of 64) | 13.50 |
| B2 | AD to BS | 8.50 | 0.84 | 0 | 6.34 | 0.26 | 590.91 | no (61 of 64) | 20.10 |
| B3 | add 1000 days | 6.26 | 0.07 | 0 | 4.35 | 0.22 | n/a | | 19.05 |
| B4 | add 13 months, Clamp | 11.93 | 0.19 | 0 | 6.45 | 0.31 | n/a | | 20.19 |
| B5 | long format, Nepali, weekday | 96.55 | 1.30 | 415 | 89.26 | 3.72 | n/a | | 150.41 |
| B6 | strict parse `YYYY/MM/DD` | 30.94 | 0.64 | 0 | 22.81 | 0.16 | n/a | | 22.42 |
| B7 | patro info | 2.07 | 0.02 | 0 | 0.99 | 0.003 | none | | 48.21 |
| B8 | patro month grid | 81.84 | 0.26 | 0 | 47.80 | 0.26 | none | | 3,112.76 |

`? 10??` is what JMH's GC profiler prints when an allocation is statistically indistinguishable
from zero; those rows are reported as 0 B/op. B5 allocates the formatted string, as every port does.

`agrees` is the result of `AgreementCheck` over the same 64 dates: shivathapaa differs on BS
2086-07-23, 2088-01-04 and 2089-06-10 in both directions, so it agrees on 61 of 64. A fast wrong
answer is listed, never ranked first.

## Against C#

BENCHMARK section 6 has no Java row. The family's rule for the JVM (Kotlin, and Go and Swift) is
B1, B2 and B7 within 1.5 times C# NepDate with no allocation beyond the date object. Java is 1.03
times C# on B1, 0.42 times on B2 and 0.04 times on B7, and the date itself is the only allocation in
those rows. B6 is 1.38 times C#, inside the same rule if it is applied to every row. Passes.

## Against the Kotlin port

| Row | Java / Kotlin | Reading |
|---|---:|---|
| B1 | 0.92 | Java is slightly faster. The difference, 1.2 ns, is inside the two confidence intervals (0.40 and 0.73), so it is noise, not a finding. |
| B2 | 1.34 | Kotlin's table-based AD to BS path is faster than Java's Hinnant arithmetic here. |
| B3 | 1.44 | Kotlin's day step is a serial addition with a packed date; Java decodes and revalidates through `plusDays`. |
| B4 | 1.85 | Java's month step evaluates the clamp and spill checks and recomputes the target month from `MONTH_START`; Kotlin reads the same table with fewer branches. |
| B5 | 1.08 | Both allocate the formatted string; the difference is the `StringBuilder` and digit path. |
| B6 | 1.36 | Kotlin's scanner reads a `CharSequence` the same way; Java's validation runs through `NepaliDate.of`, which builds no exception but is a separate call. |
| B7 | 2.10 | Both return a cached per-day `CalendarInfo`; Java's lookup subtracts serials through `daysSince` where Kotlin adds an offset. |
| B8 | 1.71 | Java's month iteration builds a `NepaliDateRange` and an iterator; Kotlin iterates a pre-sized list. |

No row shows Kotlin slower than the Java port by more than noise, so there is no Kotlin issue to
file. The gaps that are real are on the Java side and are recorded here as the next optimization
pass, not as a reason to change the Kotlin repository.

## Notes

- The Kotlin comparison uses the published 0.1.0-rc.1 artifact. Its `toYmd()` accessor, present in
  the Kotlin repository's current source, is not in that release, so the Kotlin benchmark consumes
  the date through `getYear()`, `getMonthValue()` and `getDayOfMonth()`, which exist in both.
- B7 and B8 were measured again after the Java patro switched from building a `CalendarInfo` per
  call to one cached instance per covered day. The table above is from the final run.
- The three benchmark classes share one `BenchmarkInputs`, so the 64 dates are identical for every
  column.
