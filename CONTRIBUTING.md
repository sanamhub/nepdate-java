# Contributing

Thanks for helping. This repository is one port of the NepDate family; the calendar data, the
behaviour and the test vectors live in the hub
[`sanamhub/nepdate`](https://github.com/sanamhub/nepdate), and every port must produce the same
answers.

## Before you start

- A behaviour change belongs in the hub as a pull request that updates the specification or the
  vectors. This repository then syncs `shared/` and follows. Never edit `shared/` by hand.
- A date, tithi, holiday or event that is wrong in every port is a hub issue, not a change here.
- Security reports go through [SECURITY.md](SECURITY.md), not a public issue.

## Setup

You need a JDK 17 or newer. The Gradle wrapper downloads Gradle 9.6.0; a newer JDK can be selected
with `-PnepdateToolchain=21` or `-PnepdateToolchain=25`, and the toolchain resolver downloads it if
needed.

```sh
./gradlew checkAll
./scripts/ci.sh
```

`checkAll` runs formatting, static analysis, both test suites, the coverage gate, the generator
check and the core isolation check. `scripts/ci.sh` is the same list CI runs, plus the documentation
and release checks. `./scripts/check-docs.sh` checks the prose rules separately.

## Tests

- Every test that asserts a shared vector reads it from `shared/spec/vectors/`; do not copy a value
  into a test.
- A new public member needs a test that covers it, Javadoc that names the C# member it corresponds
  to, a row in `docs/API.md`, and an example when it starts a new group.
- A bug fix needs a test that fails before the fix. Say in the pull request which test that is.
- The coverage gate is 90% of lines per published module and is not lowered. If a line cannot be
  covered, say why in the pull request instead of excluding it.

## Code

- Google Java Format, two-space indentation, through `./gradlew spotlessApply`.
- No runtime dependency in either artifact. A new build or test dependency must be pinned and at
  least 14 days old; Dependabot proposes the updates with a 14-day cooldown.
- Public types and members are immutable and thread-safe. A method that can fail on an argument
  throws `NepDateException`; the non-throwing form returns `Optional`.
- Comments and documentation say why, name the C# member or the hub specification section, and use
  plain prose without em dashes.

## Commits and pull requests

- Conventional Commits, with a body that says why the change is needed.
- One logical change per pull request. A change to `shared/` created by the sync job is its own
  pull request.
- The pull request template lists the gates; all of them must be green. CI runs on Linux, macOS and
  Windows over Java 17, 21 and 25.
- Do not add co-author or tool-attribution trailers.

## Licence

By contributing you agree that your contribution is licensed under the MIT licence of this
repository, and that the C# NepDate attribution in [NOTICE](NOTICE) stays accurate.
