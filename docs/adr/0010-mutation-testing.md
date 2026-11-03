# ADR-0010: Mutation testing with PIT

- **Status:** accepted
- **Date:** 2026-11-03

## Context

Coverage says a line ran; mutation testing says a test would notice if the line were wrong. The
family's ports run a mutation gate where a tool exists. PIT is the Java tool; it is slow enough that
it cannot run on every push.

## Decision

1. **PIT mutates the core only, in a scheduled workflow and a local script.** The patro is table
   lookups over generated data; its behaviour is covered by the count tests and by `:codegen:test`,
   and mutating it would mostly mutate the generated tables.
2. **The thresholds are in `gradle.properties`:** `nepdateMutationThreshold` (percentage) and
   `nepdateMaxSurviving` (count). They are set one point or a few mutants below the measured score,
   so a regression fails without rounding hiding it. `scripts/test-mutation.sh` proves both
   thresholds fail when they should.
3. **Tests whose cost is disproportionate are excluded from the PIT run, not from CI.** The
   exhaustive walk over 109212 serials and the jqwik properties would repeat the whole search for
   every mutant. The five vector classes that read one row per dynamic invocation (1018 to 10,180
   invocations each) are excluded too: PIT selects every covering invocation for a mutant, so one
   unit became tens of thousands of test runs. They still run on every push and on all three
   operating systems; the remaining tests, chiefly `ApiTest`, `ArithmeticEdgeTest`,
   `CoverageEdgesTest`, `ParseVectorTest` and the range tests, cover the mutated lines, and a line
   that becomes NO_COVERAGE in the PIT report is visible in the score rather than hidden.
4. **Generated tables are excluded as targets,** because they are data and `:codegen:checkTables`
   owns them.
5. **A surviving mutant that cannot be killed is listed in `docs/mutation/equivalent.md`** with the
   reason, and never hidden by lowering the threshold silently.

## Consequences

- A change to a branch in the core is checked for test strength within a day.
- The threshold is a repository value, visible in `gradle.properties` and in the report.
- The mutation run is not a per-push gate; `scripts/ci.sh` says so.

## Alternatives considered

- **PIT on every push:** about four minutes per module on the reference machine, too slow for the
  matrix.
- **A lower threshold:** the review standard forbids lowering a gate to make it pass.
