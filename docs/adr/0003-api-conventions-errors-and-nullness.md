# ADR-0003: API conventions, errors and nullness

- **Status:** accepted
- **Date:** 2026-11-03

## Context

The hub's `spec/API.md` leaves the shape of failures to each language, with the rule that the port
must be consistent with itself. The ten existing ports show two families: exceptions everywhere, or
a result type everywhere. Java has an established convention for argument errors (`LocalDate.of`
throws `DateTimeException`) and for optional results (`Optional`), and the Kotlin port, the closest
neighbour, throws `NepDateException extends IllegalArgumentException`.

## Decision

1. **Validation and parsing failures throw `NepDateException`,** an unchecked
   `IllegalArgumentException` carrying a `NepDateError` kind and the month length for an invalid
   day. Checked exceptions are not used: a caller cannot recover from a date that does not exist,
   and forcing a `try` around every factory would make the common path worse. `tryParse` and
   `tryParseLenient` are the non-throwing forms and return `Optional<NepaliDate>`.
2. **The kind is the contract, the message is not.** `error()` is one of the nine kinds of
   `spec/PARSING.md` section 4 and `spec/ALGORITHM.md` section 3; messages are for people and may
   change. The first failing check wins, in the order year, month, day, or invalid Gregorian date
   then range.
3. **`Optional` only where absence is a real outcome:** `tryParse`, `tryParseLenient`, a tithi
   outside its range, and an intersection with no overlap. A date, a range, a list or a string is
   never null, and the package is `@NullMarked` (JSpecify) so a checker can rely on it.
4. **Java names, not translated names.** Static factories are `of`, `ofGregorian`, `ofYmd`,
   `parse`; the C# `Year`, `Month`, `Day` and `MonthName` members become `year()`, `monthNumber()`,
   `day()` and `month()`; `AddMonths` and `AddDays` become `plusMonths` and `plusDays` as
   `java.time` spells them; the C# `TimeSpan Subtract` becomes `daysUntil` (other minus this) and
   the C# operator direction is `daysSince` (this minus other). `docs/API.md` names the C# member
   for every row.
5. **A range refuses `start > end`** with `NepDateException(OUT_OF_RANGE)`, because this port has no
   empty range (hub D-01). `intersect` returns an empty `Optional`; `except` returns zero, one or
   two ranges.
6. **Clock functions take a `ZoneId` or a `Clock`.** There is no `today()` without an argument.
   `KATHMANDU` is a constant a caller may pass, not a default.
7. **The pattern and the long form are small value types.** `DateOrder`, `Separator`, `Language`
   and the `LongDateOptions` record replace the C# bool parameters, so a call reads as what it
   writes. `tryFormat(char[])` is the Java form of `ISpanFormattable.TryFormat`: false and an
   unchanged buffer when the text does not fit.

## Consequences

- A caller writes `catch (NepDateException e)` and switches on `e.error()`.
- A Kotlin user sees the same names and behaviour in both ports, and both can sit on one classpath.
- Every new member needs a kind, a Javadoc sentence, an API table row and a test.

## Alternatives considered

- **A `Result` type everywhere:** Java has no sealed result idiom in common use, and every call would
  need unpacking; `Optional` plus exceptions is the platform's own split.
- **Checked `NepDateException`:** forces a `throws` clause through every conversion and adds no
  recovery path.
- **Returning `null`:** rejected by the nullness decision and by hub D-01.
