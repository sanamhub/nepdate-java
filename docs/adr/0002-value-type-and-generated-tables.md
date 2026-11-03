# ADR-0002: The value type, the serial day and the generated tables

- **Status:** accepted
- **Date:** 2026-11-03

## Context

A date is used in tight loops (a calendar grid builds hundreds of dates) and must be cheap to copy,
compare and use as a map key. It must also be impossible to hold an invalid date, because hub
`spec/PARITY.md` D-01 makes invalid states unrepresentable. The specification's serial-day model
(ALGORITHM section 4) gives every supported day one integer.

## Decision

1. **`NepaliDate` is a final class over two ints:** the serial (0 to 109211) and the BS date packed
   as `year << 10 | month << 6 | day`. The constructor is private; the static factories validate
   once. Accessors are shifts, so no field is recomputed per call.
2. **The identity is the day.** `equals`, `hashCode` and `compareTo` use the serial, so a date is a
   value, a `TreeMap` key and a `HashSet` element without extra work. `Month`, `Weekday`, `Language`,
   `DateOrder`, `Separator`, `MonthOverflow` and `Quarter` are enums; their numbers are fields, not
   declaration positions.
3. **Gregorian conversion uses the platform and the algorithm.** `java.time.LocalDate` is the
   interop type and the source of epoch days when one is given. The AD fields and the AD to BS
   direction use Hinnant's `civil_from_days` and `days_from_civil`, the same public-domain
   algorithms as `spec/ALGORITHM.md` section 5, so a conversion allocates nothing.
4. **The BS to date direction is a table plus an estimate.** The month starts come from one
   character per month in `CalendarTable`, decoded once at class initialisation. The serial to BS
   estimate is at most one month low and one compare fixes it; `ExhaustiveTest` walks all 109212
   serials, so a wrong constant fails a test, not a user.
5. **The tables are generated, never hand-copied.** `CalendarTable` and `PatroTable` are written by
   `:codegen` from `shared/data`; the patro generator also checks every count against
   `shared/spec/vectors/patro-summary.json` and fails on a disagreement. The generated patro table
   is split into small builder methods because one initializer with 8860 numbers exceeds the JVM's
   64 KB method limit; the split is mechanical and the file is excluded from formatting.
6. **Ranges hold two serials.** `NepaliDateRange` stores the first and last day and nothing else, so
   a range of years costs the same as a range of one day. It implements `Iterable` rather than
   `List`, because a range's value is its two bounds and the `List` equality contract would demand
   element-by-element comparison of up to 109212 days.

## Consequences

- A date is 24 bytes plus a header, allocation-free arithmetic on the serial, and a hash code that
  is the serial.
- `NepaliDate` is serializable through the canonical text (ADR-0006), not through its fields.
- A data change alters one generated file, reviewed as data.
- The exhaustive test is the proof of the estimate and runs once per test task.

## Alternatives considered

- **Three int fields and a computed serial:** more memory and an O(1) computation per comparison.
- **`long` serials:** the range needs 17 bits, so an `int` is enough.
- **Loading the data as JSON at run time:** a runtime dependency, a parse cost and a failure mode at
  first use; every port compiles the tables instead.
- **Implementing `List`:** convenient but its equality and sublist contracts do not fit a range that
  represents its bounds, and element access would be a linear scan for large ranges.
