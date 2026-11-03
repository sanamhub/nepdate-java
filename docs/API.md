# API

Every public type and member of `nepdate-java` and `nepdate-java-patro`, grouped like the hub's
`spec/API.md`. Dates are Bikram Sambat unless the name says Gregorian. Every clock function takes a
zone or a clock, and an invalid date cannot be constructed: the one-result forms throw
`NepDateException`, and the `tryParse` forms return `Optional`.

```java
import io.github.sanamhub.nepdate.java.*;
import io.github.sanamhub.nepdate.java.patro.*;
```

The core package is `io.github.sanamhub.nepdate.java`, the patro package is
`io.github.sanamhub.nepdate.java.patro`, and the JPMS modules have the same names. The core
requires only `java.base`; `org.jspecify` is a compile-only annotation library.

## 1. Dates, identity and comparison

`NepaliDate` is immutable and always a real day from 1901-01-01 to 2199-12-30. It implements
`Comparable<NepaliDate>` and `Serializable`.

| Member | Signature | Notes |
|---|---|---|
| `NepaliDate.of` | `static NepaliDate of(int year, int month, int day)` | Validates year, then month, then day. |
| `NepaliDate.of` | `static NepaliDate of(int year, Month month, int day)` | The month as a constant. |
| `NepaliDate.MIN` | `static final NepaliDate MIN` | 1901-01-01. |
| `NepaliDate.MAX` | `static final NepaliDate MAX` | 2199-12-30. |
| `NepaliDate.KATHMANDU` | `static final ZoneId KATHMANDU` | `Asia/Kathmandu`, a convenience constant and never a default. |
| `year` | `int year()` | 1901 to 2199. |
| `monthNumber` | `int monthNumber()` | 1 to 12. |
| `month` | `Month month()` | The month constant. |
| `day` | `int day()` | 1 to the month length. |
| `weekday` | `Weekday weekday()` | Sunday is index 0. |
| `dayOfYear` | `int dayOfYear()` | 1-based, counted in the BS year. |
| `monthLength` | `int monthLength()` | 29 to 32. |
| `yearLength` | `int yearLength()` | 364 to 367. |
| `firstDayOfMonth` | `NepaliDate firstDayOfMonth()` | Day 1 of this month. |
| `lastDayOfMonth` | `NepaliDate lastDayOfMonth()` | The month's last day. |
| `fiscalYear` | `FiscalYear fiscalYear()` | The containing fiscal year. |
| `quarter` | `Quarter quarter()` | The containing fiscal quarter. |
| `compareTo` | `int compareTo(NepaliDate other)` | Negative, zero or positive by day. |
| `equals`, `hashCode` | `boolean equals(Object other)`, `int hashCode()` | Value equality by day. |

```java
import io.github.sanamhub.nepdate.java.*;

NepaliDate date = NepaliDate.of(2081, 4, 15);
System.out.println(date);                    // => 2081/04/15
System.out.println(date.month());            // => SHRAWAN
System.out.println(date.weekday());          // => TUESDAY
System.out.println(date.dayOfYear());        // => 109
System.out.println(date.yearLength());       // => 366
System.out.println(date.compareTo(NepaliDate.of(2081, 4, 16))); // => -1
```

## 2. Conversion and serialization

| Member | Signature | Notes |
|---|---|---|
| `ofGregorian` | `static NepaliDate ofGregorian(int year, int month, int day)` | Invalid AD dates are `INVALID_GREGORIAN`, real dates outside the span are `OUT_OF_RANGE`. |
| `of` | `static NepaliDate of(LocalDate date)` | The same day in the BS calendar. |
| `ofEpochDay` | `static NepaliDate ofEpochDay(long epochDay)` | Days since 1970-01-01. |
| `ofYmd` | `static NepaliDate ofYmd(int value)` | Reads `YYYYMMDD`; no input wraps. |
| `toLocalDate` | `LocalDate toLocalDate()` | The Gregorian date. |
| `toEpochDay` | `long toEpochDay()` | -45920 to 63291. |
| `toYmd` | `int toYmd()` | 19010101 to 21991230. |
| `gregorianYear`, `gregorianMonth`, `gregorianDay` | `int` | The Gregorian fields without allocating a `LocalDate`. |
| `toIsoString` | `String toIsoString()` | `YYYY-MM-DD`, the canonical text form. |
| `toNepaliDates` | `static List<NepaliDate> toNepaliDates(Iterable<? extends LocalDate>)` | Ordered batch conversion. |
| `toEnglishDates` | `static List<LocalDate> toEnglishDates(Iterable<NepaliDate>)` | Ordered batch conversion. |
| `parseToEnglishDates` | `static List<LocalDate> parseToEnglishDates(Iterable<String>)` | Parses each string strictly. |
| serialization | `implements Serializable` | The stream holds the canonical text and reads it back with `parse`. |

```java
import io.github.sanamhub.nepdate.java.*;
import java.time.LocalDate;
import java.util.List;

NepaliDate date = NepaliDate.ofGregorian(2024, 7, 30);
System.out.println(date);                       // => 2081/04/15
System.out.println(date.toIsoString());         // => 2081-04-15
System.out.println(date.toYmd());               // => 20810415
System.out.println(NepaliDate.ofYmd(20810415)); // => 2081/04/15
System.out.println(date.gregorianYear());       // => 2024
List<NepaliDate> all = NepaliDate.toNepaliDates(List.of(LocalDate.of(2024, 7, 30)));
System.out.println(all);                        // => [2081/04/15]
```

## 3. Arithmetic

| Member | Signature | Notes |
|---|---|---|
| `plusDays` | `NepaliDate plusDays(long days)` | Negative values subtract. |
| `minusDays` | `NepaliDate minusDays(long days)` | The same, with the sign flipped. |
| `plusMonths` | `NepaliDate plusMonths(long months)` | Clamps to the target month's last day. |
| `plusMonths` | `NepaliDate plusMonths(long months, MonthOverflow overflow)` | `CLAMP` or `SPILL`. |
| `plusYears` | `NepaliDate plusYears(long years)` | `plusMonths(years * 12)`. |
| `plusYears` | `NepaliDate plusYears(long years, MonthOverflow overflow)` | With the same overflow choice. |
| `daysUntil` | `long daysUntil(NepaliDate other)` | Positive when `other` is later: `other - this`. |
| `daysSince` | `long daysSince(NepaliDate other)` | Positive when this date is later: `this - other`. |
| `until` | `Period until(NepaliDate other)` | Years, months and days, anchored at the earlier date; negative components when `other` is earlier. |
| `MonthOverflow` | `CLAMP`, `SPILL` | What month arithmetic does when the day does not exist. |

```java
import io.github.sanamhub.nepdate.java.*;

NepaliDate date = NepaliDate.of(2081, 4, 32);
System.out.println(date.plusMonths(2));                      // => 2081/06/30
System.out.println(date.plusMonths(2, MonthOverflow.SPILL)); // => 2081/07/02
System.out.println(date.plusDays(1000));                     // => 2084/01/30
System.out.println(date.plusYears(1));                       // => 2082/04/31
System.out.println(date.daysUntil(NepaliDate.of(2081, 5, 31))); // => 31
System.out.println(date.daysSince(NepaliDate.of(2081, 5, 31))); // => -31
System.out.println(date.until(NepaliDate.of(2081, 6, 1)));   // => P1M1D
```

## 4. Formatting

| Member | Signature | Notes |
|---|---|---|
| `toString` | `String toString()` | `YYYY/MM/DD`, ASCII digits. |
| `toShortString` | `String toShortString()` | The same as `toString`. |
| `toShortString` | `String toShortString(DateOrder order, Separator separator, boolean pad)` | English. |
| `toShortString` | `String toShortString(DateOrder order, Separator separator, boolean pad, Language language)` | English or Nepali digits. |
| `toLongString` | `String toLongString()` | `[Weekday, ]Month day[, year]` with the defaults. |
| `toLongString` | `String toLongString(LongDateOptions options)` | English. |
| `toLongString` | `String toLongString(LongDateOptions options, Language language)` | English or Nepali names and digits. |
| `format` | `String format(String pattern)` | The .NET-style pattern language. |
| `format` | `String format(String pattern, Language language)` | With a language. |
| `appendTo` | `StringBuilder appendTo(StringBuilder out)`, `appendTo(StringBuilder, String pattern)`, `appendTo(StringBuilder, String pattern, Language)` | Allocation-free variants. |
| `tryFormat` | `boolean tryFormat(char[] buffer)`, `tryFormat(char[], String pattern)`, `tryFormat(char[], String pattern, Language)` | False and the buffer unchanged when it does not fit. |
| `LongDateOptions` | `record LongDateOptions(boolean weekday, boolean year, boolean pad)`, `DEFAULT`, `withWeekday`, `withYear`, `withPad` | The long-form switches. |
| `DateOrder` | `YMD`, `YDM`, `MYD`, `MDY`, `DYM`, `DMY` | The field order. |
| `Separator` | `SLASH`, `BACKSLASH`, `DOT`, `UNDERSCORE`, `DASH`, `SPACE`, `character()` | The character between the fields. |
| `Language` | `ENGLISH`, `NEPALI` | Names and digits. |
| `Month` | `BAISHAKH` to `CHAITRA`, `number()`, `english()`, `shortEnglish()`, `nepali()`, `name(Language)`, `shortName(Language)`, `of(int)` | Ashad is `Asa` and Ashoj is `Aso`. |
| `Weekday` | `SUNDAY` to `SATURDAY`, `index()`, `english()`, `shortEnglish()`, `nepali()`, `shortNepali()`, `name(Language)`, `shortName(Language)`, `toDayOfWeek()`, `ofIndex(int)` | Sunday is index 0. |

Pattern tokens: `yyyy` and longer runs write the four-digit year; `y`, `yy` and `yyy` write the last
two digits; `MMMM` the full month name; `MMM` its three-letter abbreviation; `MM` and `M` the number;
`dd` the padded day and any other run of `d` the day; `\x` and `'text'` are literals. The standard
patterns are `""`, `G`, `g` and `d` for the default display, `D` for the long form and `s` for
`YYYY-MM-DD`.

```java
import io.github.sanamhub.nepdate.java.*;

NepaliDate date = NepaliDate.of(2081, 4, 15);
System.out.println(date.toShortString(DateOrder.DMY, Separator.DASH, false)); // => 15-4-2081
System.out.println(date.toLongString(LongDateOptions.DEFAULT.withWeekday(true))); // => Tuesday, Shrawan 15, 2081
System.out.println(date.format("dd.MM.yy"));                    // => 15.04.81
System.out.println(date.format("MMMM d, yyyy", Language.NEPALI)); // => साउन १५, २०८१
System.out.println(Month.SHRAWAN.number());                     // => 4
System.out.println(Weekday.TUESDAY.toDayOfWeek());              // => TUESDAY
char[] buffer = new char[10];
System.out.println(date.tryFormat(buffer));                     // => true
System.out.println(new String(buffer));                         // => 2081/04/15
```

## 5. Parsing

| Member | Signature | Notes |
|---|---|---|
| `parse` | `static NepaliDate parse(CharSequence text)` | Strict; throws `NepDateException`. |
| `tryParse` | `static Optional<NepaliDate> tryParse(CharSequence text)` | Non-throwing. |
| `parseLenient` | `static NepaliDate parseLenient(CharSequence text)` | Strict first, then month names, era markers and both orders. |
| `tryParseLenient` | `static Optional<NepaliDate> tryParseLenient(CharSequence text)` | Non-throwing. |
| `NepDateException` | `error()`, `monthLength()`, `getMessage()` | Extends `IllegalArgumentException`. |
| `NepDateError` | `OUT_OF_RANGE`, `INVALID_MONTH`, `INVALID_DAY`, `INVALID_GREGORIAN`, `INVALID_CHARACTER`, `WRONG_GROUP_COUNT`, `NUMBER_TOO_LONG`, `AMBIGUOUS`, `UNRECOGNIZED` | The error kinds. |

Strict input is exactly year, month and day, in ASCII or Devanagari digits, with the separators
`- / . _ \ space । |`. A group has at most four digits. The lenient parser accepts the closed list of
month spellings in `spec/PARSING.md`, drops `BS`, `B.S.`, `VS`, `वि.सं.`, `गते` and `मिति`, and
rejects a two-digit year as `AMBIGUOUS` instead of guessing.

```java
import io.github.sanamhub.nepdate.java.*;

System.out.println(NepaliDate.parse("२०८०/०५/१५"));            // => 2080/05/15
System.out.println(NepaliDate.parseLenient("15 Shrawan 2080")); // => 2080/04/15
System.out.println(NepaliDate.parseLenient("2080/04/15 BS"));   // => 2080/04/15
System.out.println(NepaliDate.tryParse("2080/05/40").isEmpty()); // => true
try {
  NepaliDate.parse("2080/13/01");
} catch (NepDateException error) {
  System.out.println(error.error());                            // => INVALID_MONTH
}
```

## 6. Ranges

A range is inclusive and never empty. It holds only its bounds, iterates every day, and the day
helpers never throw at the end of the data.

| Member | Signature | Notes |
|---|---|---|
| `of` | `static NepaliDateRange of(NepaliDate start, NepaliDate endInclusive)` | Refuses `start > end`. |
| `singleDay` | `static NepaliDateRange singleDay(NepaliDate date)` | One day. |
| `fromDayCount` | `static NepaliDateRange fromDayCount(NepaliDate start, int days)` | `days` days from `start`. |
| `forMonth` | `static NepaliDateRange forMonth(int year, int month)` | One BS month. |
| `forYear` | `static NepaliDateRange forYear(int year)` | One BS year. |
| `forFiscalYear` | `static NepaliDateRange forFiscalYear(int fiscalYear)` | One fiscal year. |
| `currentMonth`, `currentFiscalYear`, `currentYear` | `static ...(ZoneId zone)` and `static ...(Clock clock)` | Today in a zone. |
| `start`, `end`, `length` | `NepaliDate start()`, `NepaliDate end()`, `int length()` | The bounds and the day count. |
| `contains` | `boolean contains(NepaliDate date)`, `boolean contains(NepaliDateRange other)` | A day or a whole range. |
| `overlaps` | `boolean overlaps(NepaliDateRange other)` | At least one shared day. |
| `isAdjacentTo` | `boolean isAdjacentTo(NepaliDateRange other)` | Touching without overlapping. |
| `intersect` | `Optional<NepaliDateRange> intersect(NepaliDateRange other)` | Empty when disjoint. |
| `union` | `NepaliDateRange union(NepaliDateRange other)` | The smallest covering range. |
| `except` | `List<NepaliDateRange> except(NepaliDateRange other)` | Zero, one or two ranges. |
| `splitByMonth` | `List<NepaliDateRange> splitByMonth()` | One piece per month touched. |
| `splitByFiscalQuarter` | `List<NepaliDateRange> splitByFiscalQuarter()` | One piece per fiscal quarter touched. |
| `datesWithInterval` | `Stream<NepaliDate> datesWithInterval(int interval)` | Every `interval` days; `interval` at least 1. |
| `workingDays` | `Stream<NepaliDate> workingDays(boolean excludeSunday)` | Every day except Saturday, and Sunday when asked. |
| `weekendDays` | `Stream<NepaliDate> weekendDays(boolean includeSunday)` | Every Saturday, and Sunday when asked. |
| `stream`, `iterator` | `Stream<NepaliDate> stream()`, `Iterator<NepaliDate> iterator()` | Every day in order. |
| `toString` | `String toString()` | `start - end` in the default display. |
| `toShortString` | `String toShortString(DateOrder order, Separator separator)` and the `Language` form | Both bounds in one format. |
| `equals`, `hashCode` | Value equality of the bounds | |

```java
import io.github.sanamhub.nepdate.java.*;
import java.util.Optional;

NepaliDateRange range = NepaliDateRange.of(NepaliDate.of(2081, 4, 10), NepaliDate.of(2081, 4, 20));
System.out.println(range);                          // => 2081/04/10 - 2081/04/20
System.out.println(range.length());                 // => 11
System.out.println(range.stream().count());         // => 11
System.out.println(range.splitByMonth().size());    // => 1
System.out.println(range.workingDays(false).count()); // => 9
System.out.println(NepaliDateRange.forMonth(2081, 4)); // => 2081/04/01 - 2081/04/32
Optional<NepaliDateRange> none = range.intersect(NepaliDateRange.forMonth(2081, 5));
System.out.println(none.isEmpty());                 // => true
```

## 7. Fiscal year

| Member | Signature | Notes |
|---|---|---|
| `FiscalYear.of` | `static FiscalYear of(int startYear)` | Any start year; a method whose result leaves the data throws. |
| `FiscalYear.containing` | `static FiscalYear containing(NepaliDate date)` | The fiscal year of a date. |
| `startYear` | `int startYear()` | The BS year it starts in. |
| `start` | `NepaliDate start()` | 1 Shrawan. |
| `end` | `NepaliDate end()` | The last day of Ashad of the next year. |
| `range` | `NepaliDateRange range()` | Every day. |
| `quarterRange` | `NepaliDateRange quarterRange(Quarter quarter)` | Every day of one quarter. |
| `label` | `String label()`, `String label(Language language)` | `2082/83`. |
| `Quarter` | `Q1`, `Q2`, `Q3`, `Q4`, `months()`, `ofMonth(int)` | Q1 is Shrawan to Ashoj and Q4 Baishakh to Ashad. |
| comparison | `compareTo`, `equals`, `hashCode`, `toString` | By start year. |

```java
import io.github.sanamhub.nepdate.java.*;

NepaliDate date = NepaliDate.of(2081, 4, 15);
System.out.println(date.fiscalYear());               // => 2081/82
System.out.println(date.fiscalYear().start());       // => 2081/04/01
System.out.println(date.fiscalYear().end());         // => 2082/03/32
System.out.println(date.quarter());                  // => Q1
System.out.println(date.fiscalYear().quarterRange(Quarter.Q1)); // => 2081/04/01 - 2081/06/30
System.out.println(FiscalYear.containing(date).label()); // => 2081/82
```

## 8. Patro: tithi, holidays and events

| Member | Signature | Notes |
|---|---|---|
| `Patro.info` | `static CalendarInfo info(NepaliDate date)` | Never throws; empty outside the coverage. |
| `Patro.coverage` | `static NepaliDateRange coverage()` | The smallest range with any metadata, 2077-01-01 to 2084-08-20. |
| `Patro.publicHolidays` | `static List<NepaliDate> publicHolidays()` | Ascending, every flagged day. |
| `CalendarInfo.tithi` | `Optional<Named> tithi()` | Beyond the 2190-day tithi range it is empty. |
| `CalendarInfo.tithiNp`, `tithiEn` | `String` | Empty when the day has no tithi. |
| `CalendarInfo.isPublicHoliday` | `boolean` | The gazetted flag. |
| `CalendarInfo.events` | `List<Named> events()` | File order, possibly empty. |
| `CalendarInfo.eventsNp`, `eventsEn` | `List<String>` | The same events in one language. |
| `Named` | `record Named(String nepali, String english)`, `name(Language)` | One name in both languages. |

```java
import io.github.sanamhub.nepdate.java.NepaliDate;
import io.github.sanamhub.nepdate.java.patro.*;

CalendarInfo day = Patro.info(NepaliDate.of(2081, 1, 1));
System.out.println(day.tithiEn());               // => Panchami
System.out.println(day.isPublicHoliday());       // => true
System.out.println(day.eventsEn());              // => [New Year, Mesh Sankranti, Bisket Jatra]
CalendarInfo empty = Patro.info(NepaliDate.of(2070, 6, 15));
System.out.println(empty.tithi().isEmpty());     // => true
System.out.println(Patro.coverage());            // => 2077/01/01 - 2084/08/20
System.out.println(Patro.publicHolidays().size()); // => 294
```
