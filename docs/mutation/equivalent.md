# Mutation testing: the targets and the surviving mutants

PIT runs on the core only (ADR-0010). The thresholds are `nepdateMutationThreshold` and
`nepdateMaxSurviving` in `gradle.properties`. The gate sits one point below the measured score and
at the measured missed count, with no padding.

## What is excluded, and why

| Target or test | Why |
|---|---|
| `CalendarTable` | Generated data from `shared/data`. `:codegen:checkTables` compares it byte for byte and `ExhaustiveTest` checks every serial against it, so a mutation would be caught without PIT. |
| `ExhaustiveTest` | Walks all 109212 serials; PIT would repeat that walk for every mutant. |
| `PropertiesTest` | jqwik runs a thousand tries per property; PIT would repeat that for every mutant. |
| `DatesVectorTest`, `AddDaysVectorTest`, `AddMonthsVectorTest`, `MonthBoundariesVectorTest`, `FormatPatternVectorTest` | Each reads a vector row per dynamic invocation, 1018 to 10,180 of them. PIT selects every covering invocation for each mutant, so a single unit became tens of thousands of test runs. They run on every push in the normal gate; `MutationCoverageTest` asserts the boundaries they reach in passing. |

All other classes and the remaining deterministic tests are mutated and run.

## The last full run

| Item | Value |
|---|---|
| Command | `./scripts/mutation.sh` |
| PIT | 1.30.1 |
| Java | Temurin 17.0.20.1 on Windows 11 |
| Date | 2026-11-03 |
| Mutations | 823 |
| Killed | 770 |
| Timed out | 26 |
| Memory errors | 7 |
| Survived | 20 |
| No coverage | 0 |
| Score | 803 detected of 823, 97 percent |
| Test strength | 98 percent |
| Line coverage of the mutated classes | 761 of 769, 99 percent |
| Tests run | 6,804, 8.27 per mutation |

The 26 timeouts are all in the `LenientParser` scanners (`isNumericRun`, `nextNumber`, `nextToken`,
`token`, `scanLenient`): a mutated loop bound stops the token position advancing, so the minion runs
until PIT's timeout. That is the correct outcome for a loop bound and counts as detected. The 7
memory errors are in `NepaliDateRange`'s split and quarter loops and in `Text.appendCustom`, where a
mutated bound makes a range or a string grow until the minion runs out of memory; PIT counts them as
detected, and `scripts/mutation.sh` prints both counts so a rise cannot hide.

## Equivalent mutants

These 20 survive because no input can make them change the returned value or the exception. They are
listed with the reason, not hidden by lowering a threshold.

| Source | Method | Line | Mutator | Why it cannot change behaviour |
|---|---|---|---:|---|
| `NepaliDate.java` | `plusMonths` | 559 | boundary | The bound is exactly `±MONTHS`. The next check, on `shifted`, rejects every date at that bound, so making the comparison inclusive changes only which of the two checks throws, not the error kind or the message. |
| `NepaliDate.java` | `plusMonths` | 583 | boundary | On the spill path the result is at most three days into the following month (32 minus a 29-day month), and `MAX_SERIAL` is the last day of the last month. The serial can never equal `MAX_SERIAL`, so `>` and `>=` never differ. |
| `NepaliDate.java` | `plusYears` | 614 | boundary | `±YEARS` is always rejected by `plusMonths` for every date, so the inclusive bound cannot be reached. |
| `NepaliDate.java` | `sizeHint` | 330 | return 0 | The value is only the initial `ArrayList` capacity in the batch conversions. A smaller capacity changes no result and no exception. |
| `NepaliDate.java` | `until` | 658 | boundary | `<` and `<=` differ only when both dates are the same day. In that case the swapped operands are the same value, the month and day counts are zero either way, and the returned `Period` is `P0D`. |
| `LenientParser.java` | `foldCase` | 326 | boundary | The boundary is the letter `Z`. No month name, era marker or separator the parser accepts contains `Z`, so both branches produce the same folded token. |
| `LenientParser.java` | `nextToken` | 157 | boundary | `from > 0` and `>=` differ at the first call. At `from == 0` the number probe finds the same leading number the token loop would find, or returns `-1` and the loop proceeds; the token is the same. |
| `LenientParser.java` | `nextToken` | 157 | math | The look-behind only decides whether to try a number at the current position. Changing the offset reads a separator or the same digit, and either the probe returns `-1` and the loop reaches the token, or it returns the same number. |
| `LenientParser.java` | `nextToken` | 159, 171 | boundary | The only scanned number that is zero is the year `0`, and that input is rejected as `AMBIGUOUS` before the number's sign matters; both branches end at the same error. |
| `LenientParser.java` | `parse` | 268 | boundary | `scanLenient` never returns the packed value `0`: a zero date fails validation and becomes a negative error code, so the `<= 0` boundary is unreachable. |
| `LenientParser.java` | `scanLenient` | 194 | boundary | The strict scanner turns a zero date into a negative error, so `strict == 0` never reaches the comparison. |
| `LenientParser.java` | `scanLenient` | 210 | boundary | A token encodes two character positions, start and end, so its packed value is never `0`; the loop's `found < 0` boundary cannot be hit at zero. |
| `LenientParser.java` | `tryParse` | 283 | boundary | Same zero-value argument as `parse`: `scanLenient` does not return `0`. |
| `Parsing.java` | `tryParse` | 168 | boundary | Same zero-value argument: `scanStrictValid` returns a negative error for a zero date, never `0`. |
| `Text.java` | `appendCustom` | 230 | boundary | `indexOf('\'', index + 1)` searches from at least position 1, so its result can be `-1` or at least 1, never `0`; `close < 0` and `close <= 0` are the same test. |
| `Text.java` | `appendDigit` | 57 | return null | Every caller appends through the `StringBuilder` and ignores the return value. Returning `null` instead of the builder changes nothing. |
| `Text.java` | `appendShort` | 94 | negated conditional | The flag selects a four-digit year width or no padding. Every supported year has four digits, so both widths print the same year. |
