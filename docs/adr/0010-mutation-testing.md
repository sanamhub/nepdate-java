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
3. **Only tests that would change every mutant are excluded** from the PIT run: the exhaustive walk
   over 109212 serials and the jqwik properties would repeat for every mutant. The deterministic
   vector tests still cover those paths.
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
