# Changelog

All notable changes are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/). Every NepDate SDK shares one `MAJOR.MINOR` line, so this
repository joins at `1.0.0-rc.1` with the other ports
([hub ADR-0007](https://github.com/sanamhub/nepdate/blob/main/docs/adr/0007-one-version-line-for-every-sdk.md)).

## [1.0.0-rc.1] - 2026-11-04

The API is stable from 1.0 (hub ADR-0009). This is the first release of the Java port.

### Added

- The `io.github.sanamhub:nepdate-java` artifact: Bikram Sambat dates from 1901-01-01 to
  2199-12-30 with conversion to and from `java.time.LocalDate`, the `YYYYMMDD` integer form,
  arithmetic with clamp and spill, the calendar breakdown between two dates, fiscal years and
  quarters, English and Nepali formatting with patterns, strict and lenient parsing, inclusive
  ranges with their set operations, ordered batch conversion, and Java serialization of the
  canonical text.
- The `io.github.sanamhub:nepdate-java-patro` artifact: tithi, public holidays and events for
  2077-01-01 to 2084-08-20.
- Both artifacts are pure Java with no runtime dependency, target Java 17 and ship real JPMS
  modules, so they sit beside the Kotlin port without a split package.
- Calendar and patro tables generated from the shared data by `:codegen`, a `--check` mode that
  fails on drift, and a check of the patro counts against the hub's summary.
- Tests for every shared vector file, for every row of the C# test catalogue that applies, and for
  the properties that hold for all 109212 supported days.
- One CI command, `scripts/ci.sh`, that runs formatting, static analysis, the generator check, both
  test suites, a 90% line-coverage gate, the Javadoc gate, the core isolation check, the
  documentation examples and the release check.
- Javadoc for every public member, eleven architecture decisions, a release runbook, and a README and
  API reference whose Java samples compile, run and are checked against their printed output in CI.
- A JMH benchmark of B1 to B8 against the published Kotlin artifact, the Maven Central competitor
  and the C# baseline, with the evidence in `docs/competitors.md`.
- CI on Linux, macOS and Windows over Java 17, 21 and 25, the hub's shared-check, CodeQL, Scorecard,
  dependency review, a scheduled mutation run, and the release and verify-published workflows.

