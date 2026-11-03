# ADR-0005: The patro is a second artifact

- **Status:** accepted
- **Date:** 2026-11-03

## Context

Hub ADR-0004 requires every port to ship the core and the patro as two units, with the patro
depending on the core and never the other way round, released together from one tag. The patro is
about 100 KB of names and tables against a core of about 45 KB, it changes every year, and most
users only convert dates.

## Decision

1. **Two artifacts, one repository, one tag:** `io.github.sanamhub:nepdate-java` and
   `io.github.sanamhub:nepdate-java-patro`, both at the same version, released from the same `v*`
   tag. The patro POM declares the core at the same version.
2. **Two JPMS modules and two packages:** `io.github.sanamhub.nepdate.java` and
   `io.github.sanamhub.nepdate.java.patro`. The patro module `requires transitive` the core because
   its API exposes `NepaliDate`. The names end in `.java` so the Kotlin port can sit beside this one
   without a split package.
3. **The query shape is `Patro.info(date)` returning `CalendarInfo`,** plus `coverage()` and
   `publicHolidays()`. `info` never fails and returns empty values outside the data, matching the
   other ports; `tithi()` is an `Optional` because a day without a tithi is a real outcome.
4. **The tables are compiled into the patro jar only.** `checkCoreSplit` proves the core contains no
   patro string and no reference to the patro package, and that the patro contains the marker.
5. **The core builds and tests on its own.** A patro test is the only test that reads the patro; the
   core test task has no dependency on the patro project.

## Consequences

- An application that converts dates downloads about 45 KB and no almanac strings.
- A patro data update bumps both artifacts, as hub ADR-0004 decision 3 requires.
- A new patro member belongs in `docs/API.md` group 8 and in the patro tests.

## Alternatives considered

- **One artifact:** simpler, but every user carries the almanac, and the two parts could not differ
  in cadence.
- **A separate repository:** a second sync and release pipeline for the same data.
- **A `patro` subpackage of the core:** would make `requires` optional at the JPMS level, which a
  classpath user cannot honour, and would still ship the data.
