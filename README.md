<p align="center">
  <img src="https://raw.githubusercontent.com/sanamhub/nepdate-java/main/docs/assets/nepdate-java-128.png" width="128" alt="NepDate for Java logo">
</p>

<h1 align="center">nepdate-java</h1>

<p align="center">
  <a href="https://github.com/sanamhub/nepdate-java/actions/workflows/ci.yml"><img src="https://github.com/sanamhub/nepdate-java/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://central.sonatype.com/artifact/io.github.sanamhub/nepdate-java"><img src="https://img.shields.io/maven-central/v/io.github.sanamhub/nepdate-java?include_prereleases&label=nepdate-java&logo=apachemaven" alt="nepdate-java on Maven Central"></a>
  <a href="https://central.sonatype.com/artifact/io.github.sanamhub/nepdate-java-patro"><img src="https://img.shields.io/maven-central/v/io.github.sanamhub/nepdate-java-patro?include_prereleases&label=nepdate-java-patro&logo=apachemaven" alt="nepdate-java-patro on Maven Central"></a>
  <img src="https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white" alt="Java 17 or newer">
  <a href="https://github.com/sanamhub/nepdate-java/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue" alt="License: MIT"></a>
</p>

Bikram Sambat (Nepali calendar) dates for Java: conversion to and from Gregorian, arithmetic with
an explicit overflow mode, formatting and parsing in English and Nepali, ranges and fiscal years.
Part of the NepDate family: the shared data and specification live in the hub
[`sanamhub/nepdate`](https://github.com/sanamhub/nepdate); the C# original is
[NepDate](https://github.com/RajuPrasai/NepDate) by Raju Prasai.

> **Status:** 1.0.0-rc.1 is the first release of the Java port and is not on Maven Central yet. The
> API is listed in [docs/API.md](https://github.com/sanamhub/nepdate-java/blob/main/docs/API.md)
> and may still change before 1.0.0. Patro data (tithi, holidays, events) is informational, not an
> official calendar.

Two artifacts, released together with the same version and one tag:

- [`io.github.sanamhub:nepdate-java`](https://central.sonatype.com/artifact/io.github.sanamhub/nepdate-java):
  dates, conversion, arithmetic, formatting, parsing, ranges and fiscal years. No dependencies at
  all and no almanac data.
- [`io.github.sanamhub:nepdate-java-patro`](https://central.sonatype.com/artifact/io.github.sanamhub/nepdate-java-patro):
  the patro (पात्रो, the Nepali almanac): tithi, public holidays and events from BS 2077-01-01 to
  2084-08-20. It depends on the core.

Pure Java with no runtime dependency, Java 17 or newer, and a real JPMS module. The Kotlin port
covers Kotlin and Android; this port is for a classpath without the Kotlin standard library. Every
clock function takes a `ZoneId` or a `Clock`, so nothing reads a hidden local zone. The supported
range is BS 1901-01-01 to 2199-12-30 (AD 1844-04-11 to 2143-04-15), with the same results as C#
NepDate and the other ports, checked against the shared vectors.

## Install

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.sanamhub:nepdate-java:1.0.0-rc.1")
implementation("io.github.sanamhub:nepdate-java-patro:1.0.0-rc.1") // tithi, holidays, events
```

Maven:

```xml
<dependency>
  <groupId>io.github.sanamhub</groupId>
  <artifactId>nepdate-java</artifactId>
  <version>1.0.0-rc.1</version>
</dependency>
```

The artifacts are JPMS modules `io.github.sanamhub.nepdate.java` and
`io.github.sanamhub.nepdate.java.patro`. Both packages fit on one classpath beside the Kotlin port:
the Java port's packages end in `.java`, so there is no split package.

## Quick start

```java
import io.github.sanamhub.nepdate.java.NepaliDate;

NepaliDate date = NepaliDate.of(2081, 4, 15);
System.out.println(date);                       // => 2081/04/15
System.out.println(date.toLocalDate());         // => 2024-07-30
System.out.println(date.toLongString());        // => Shrawan 15, 2081
System.out.println(NepaliDate.parse("2081/04/15").equals(date)); // => true
```

## Dates

A `NepaliDate` is immutable, thread-safe and always a real day. An invalid date cannot be
constructed, so there is no default or zero date and no method returns `null`.

```java
import io.github.sanamhub.nepdate.java.NepaliDate;

NepaliDate date = NepaliDate.parse("2081-04-15");
System.out.println(date.year());          // => 2081
System.out.println(date.monthNumber());   // => 4
System.out.println(date.month());         // => SHRAWAN
System.out.println(date.day());           // => 15
System.out.println(date.weekday());       // => TUESDAY
System.out.println(date.dayOfYear());     // => 109
System.out.println(date.monthLength());   // => 32
System.out.println(date.yearLength());    // => 366
System.out.println(date.lastDayOfMonth()); // => 2081/04/32
System.out.println(NepaliDate.MIN);       // => 1901/01/01
System.out.println(NepaliDate.MAX);       // => 2199/12/30
```

## Conversion and serialization

Gregorian dates are `java.time.LocalDate`. The canonical text form is `YYYY-MM-DD`; `toString()` is
the C# display form `YYYY/MM/DD`.

```java
import io.github.sanamhub.nepdate.java.NepaliDate;
import java.time.LocalDate;
import java.util.List;

NepaliDate date = NepaliDate.ofGregorian(2024, 7, 30);
System.out.println(date);                 // => 2081/04/15
System.out.println(date.toLocalDate());   // => 2024-07-30
System.out.println(date.toIsoString());   // => 2081-04-15
System.out.println(date.toYmd());         // => 20810415
System.out.println(NepaliDate.ofYmd(20810415)); // => 2081/04/15
System.out.println(NepaliDate.of(LocalDate.of(2024, 7, 30))); // => 2081/04/15

List<NepaliDate> dates = NepaliDate.toNepaliDates(List.of(LocalDate.of(2024, 7, 30), LocalDate.of(2024, 7, 31)));
System.out.println(dates);                // => [2081/04/15, 2081/04/16]
System.out.println(NepaliDate.toEnglishDates(dates)); // => [2024-07-30, 2024-07-31]
System.out.println(NepaliDate.parseToEnglishDates(List.of("2081-04-15"))); // => [2024-07-30]
```

`NepaliDate` is `Serializable`, and the stream holds the canonical text:

```java
import io.github.sanamhub.nepdate.java.NepaliDate;
import java.io.*;

ByteArrayOutputStream bytes = new ByteArrayOutputStream();
try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
  out.writeObject(NepaliDate.of(2081, 4, 15));
}
try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
  System.out.println(in.readObject());    // => 2081/04/15
}
```

## Arithmetic

Day arithmetic is exact. Month and year arithmetic takes an integer and an explicit overflow mode:
`MonthOverflow.CLAMP` uses the target month's last day, `MonthOverflow.SPILL` carries the extra days
into the next month. Negative values go the other way.

```java
import io.github.sanamhub.nepdate.java.MonthOverflow;
import io.github.sanamhub.nepdate.java.NepaliDate;
import java.time.Period;

NepaliDate date = NepaliDate.of(2081, 4, 32);
System.out.println(date.plusMonths(2));                        // => 2081/06/30
System.out.println(date.plusMonths(2, MonthOverflow.SPILL));   // => 2081/07/02
System.out.println(date.plusDays(1000));                       // => 2084/01/30
System.out.println(date.plusYears(1));                         // => 2082/04/31
System.out.println(NepaliDate.of(2081, 1, 31).daysUntil(NepaliDate.of(2081, 2, 30))); // => 30
System.out.println(NepaliDate.of(2081, 2, 30).daysSince(NepaliDate.of(2081, 1, 31))); // => 30
Period period = NepaliDate.of(2080, 1, 1).until(NepaliDate.of(2081, 4, 15));
System.out.println(period);                                    // => P1Y3M14D
```

## Formatting

Short forms take an order and a separator; long forms take options; the pattern language follows
.NET. Nepali output uses the same methods with `Language.NEPALI`.

```java
import io.github.sanamhub.nepdate.java.*;
import java.util.Locale;

NepaliDate date = NepaliDate.of(2081, 4, 15);
System.out.println(date.toShortString(DateOrder.DMY, Separator.DASH, false)); // => 15-4-2081
System.out.println(date.toLongString(LongDateOptions.DEFAULT.withWeekday(true))); // => Tuesday, Shrawan 15, 2081
System.out.println(date.format("dd.MM.yy"));                    // => 15.04.81
System.out.println(date.format("MMMM d, yyyy", Language.NEPALI)); // => साउन १५, २०८१
System.out.println(date.toShortString(DateOrder.YMD, Separator.SLASH, true, Language.NEPALI)); // => २०८१/०४/१५
```

`tryFormat` writes into a fixed `char[]` and returns false when the output does not fit:

```java
import io.github.sanamhub.nepdate.java.NepaliDate;

NepaliDate date = NepaliDate.of(2081, 4, 15);
char[] buffer = new char[10];
System.out.println(date.tryFormat(buffer));   // => true
System.out.println(new String(buffer));       // => 2081/04/15
System.out.println(date.tryFormat(new char[9])); // => false
```

## Parsing

The strict parser takes exactly year, month and day, in ASCII or Devanagari digits, with the
separators `- / . _ \ space । |`. The lenient parser also accepts month names, era markers and day
first, and rejects rather than guesses. Both have a non-throwing form.

```java
import io.github.sanamhub.nepdate.java.NepaliDate;
import java.util.Optional;

System.out.println(NepaliDate.parse("२०८०/०५/१५"));            // => 2080/05/15
System.out.println(NepaliDate.parseLenient("15 Shrawan 2080")); // => 2080/04/15
System.out.println(NepaliDate.parseLenient("2080/04/15 BS"));   // => 2080/04/15
Optional<NepaliDate> parsed = NepaliDate.tryParse("2080/05/40");
System.out.println(parsed.isPresent());                        // => false
System.out.println(NepaliDate.tryParseLenient("nope").isPresent()); // => false
```

A failure is a `NepDateException` with an `error()` kind, never a guessed date.

## Ranges

A range is inclusive and never empty: the factory refuses a start after the end, and an
intersection with no overlap is an empty `Optional`.

```java
import io.github.sanamhub.nepdate.java.*;
import java.util.Optional;

NepaliDateRange range = NepaliDateRange.of(NepaliDate.of(2081, 4, 10), NepaliDate.of(2081, 4, 20));
System.out.println(range);                       // => 2081/04/10 - 2081/04/20
System.out.println(range.length());              // => 11
System.out.println(range.contains(NepaliDate.of(2081, 4, 15))); // => true
System.out.println(range.splitByMonth().size()); // => 1
System.out.println(range.workingDays(false).count()); // => 9
Optional<NepaliDateRange> overlap = range.intersect(NepaliDateRange.of(NepaliDate.of(2081, 4, 15), NepaliDate.of(2081, 4, 25)));
System.out.println(overlap.orElseThrow());       // => 2081/04/15 - 2081/04/20
```

## Fiscal year

A fiscal year starts on 1 Shrawan and ends on the last day of Ashad of the next BS year. Quarters
are Q1 Shrawan to Ashoj, Q2 Kartik to Poush, Q3 Magh to Chaitra and Q4 Baishakh to Ashad.

```java
import io.github.sanamhub.nepdate.java.*;

NepaliDate date = NepaliDate.of(2081, 4, 15);
System.out.println(date.fiscalYear());              // => 2081/82
System.out.println(date.fiscalYear().start());      // => 2081/04/01
System.out.println(date.fiscalYear().end());        // => 2082/03/32
System.out.println(date.quarter());                 // => Q1
System.out.println(date.fiscalYear().quarterRange(Quarter.Q1)); // => 2081/04/01 - 2081/06/30
System.out.println(FiscalYear.of(2080).label());    // => 2080/81
```

## Today

`today` takes a zone or a clock; `isToday`, `isYesterday` and `isTomorrow` do the same. Nothing
reads the machine's hidden local zone.

```java
import io.github.sanamhub.nepdate.java.NepaliDate;
import java.time.Clock;
import java.time.Instant;

Clock clock = Clock.fixed(Instant.parse("2024-07-30T18:30:00Z"), NepaliDate.KATHMANDU);
NepaliDate today = NepaliDate.today(clock);
System.out.println(today);                    // => 2081/04/16
System.out.println(today.isToday(clock));     // => true
System.out.println(today.minusDays(1).isYesterday(clock)); // => true
System.out.println(NepaliDate.today(NepaliDate.KATHMANDU).isToday(NepaliDate.KATHMANDU)); // => true
```

## Patro: tithi, holidays and events

The patro is the second artifact. `info` never throws: a day outside the data returns empty values.
Events keep the file's display order.

```java
import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.patro.*;

CalendarInfo day = Patro.info(NepaliDate.of(2081, 1, 1));
System.out.println(day.tithiEn());            // => Panchami
System.out.println(day.isPublicHoliday());    // => true
System.out.println(day.eventsEn());           // => [New Year, Mesh Sankranti, Bisket Jatra]
System.out.println(Patro.coverage().start()); // => 2077/01/01
System.out.println(Patro.publicHolidays().size()); // => 294
```

## Supported versions and platforms

Java 17 is the floor; CI also builds and tests on Java 21 and Java 25. The jars target Java 17
bytecode. There is no runtime dependency and no reflection beyond standard Java serialization. The
module requires only `java.base`, and `java.time` for the Gregorian bridge.

The Kotlin port remains for Kotlin and Android. This port needs Java 17, so it does not run on
Android versions without `java.time`; the Kotlin port covers those.

## Data and correctness

Calendar data and the specification come from the hub
[`sanamhub/nepdate`](https://github.com/sanamhub/nepdate), which imported them from C# NepDate. The
tables are generated from `shared/data` by `:codegen`, and `:codegen:checkTables` fails when a
generated table is stale. Tests load every shared vector: the golden date, arithmetic, formatting,
parsing, range and serialization files, plus the month boundaries, year lengths and patro counts.
The CI run also checks every code sample in this file and in `docs/API.md`. Tithi, holiday and event
data is informational, not an official calendar.

## Documentation

| What | Where |
|---|---|
| Every public type and member | [docs/API.md](https://github.com/sanamhub/nepdate-java/blob/main/docs/API.md) |
| Architecture decisions | [docs/adr/](https://github.com/sanamhub/nepdate-java/blob/main/docs/adr/README.md) |
| Benchmarks | [docs/benchmarks.md](https://github.com/sanamhub/nepdate-java/blob/main/docs/benchmarks.md) |
| Release runbook | [docs/runbooks/release.md](https://github.com/sanamhub/nepdate-java/blob/main/docs/runbooks/release.md) |
| What changed | [CHANGELOG.md](https://github.com/sanamhub/nepdate-java/blob/main/CHANGELOG.md) |

## Benchmarks

JMH B1 to B8 for this port and for the published Kotlin artifact, in the same JVM, same machine and
same run, are in [docs/benchmarks.md](https://github.com/sanamhub/nepdate-java/blob/main/docs/benchmarks.md).

## Contributing

See [CONTRIBUTING.md](https://github.com/sanamhub/nepdate-java/blob/main/CONTRIBUTING.md). Run
`./gradlew checkAll` and `./scripts/ci.sh` before a pull request; CI runs the same gates on Linux,
macOS and Windows.

## Credit

The calendar data and algorithm come from C# [NepDate](https://github.com/RajuPrasai/NepDate) by
Raju Prasai, under the MIT licence. The NepDate family and this port were written by
[Sanam Pakuwal](https://github.com/sanamhub).

## License

MIT. See [LICENSE](https://github.com/sanamhub/nepdate-java/blob/main/LICENSE) and
[NOTICE](https://github.com/sanamhub/nepdate-java/blob/main/NOTICE).
