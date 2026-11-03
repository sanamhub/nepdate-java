# Mutation testing: the targets and the surviving mutants

PIT runs on the core only (ADR-0010). The thresholds are `nepdateMutationThreshold` and
`nepdateMaxSurviving` in `gradle.properties`; the measured score and the statuses are below.

## What is excluded, and why

| Target or test | Why |
|---|---|
| `CalendarTable` | Generated data from `shared/data`. `:codegen:checkTables` compares it byte for byte and `ExhaustiveTest` checks every serial against it, so a mutation would be caught without PIT. |
| `ExhaustiveTest` | Walks all 109212 serials; PIT would repeat that walk for every mutant. |
| `PropertiesTest` | jqwik runs a thousand tries per property; PIT would repeat that for every mutant. |
| `DatesVectorTest`, `AddDaysVectorTest`, `AddMonthsVectorTest`, `MonthBoundariesVectorTest`, `FormatPatternVectorTest` | Each reads a vector row per dynamic invocation, 1018 to 10,180 of them. PIT selects every covering invocation for each mutant, so a single unit became tens of thousands of test runs. They run on every push in the normal gate, and the lines they reach are also covered by `ApiTest`, `ArithmeticEdgeTest`, `CoverageEdgesTest`, `ParseVectorTest` and the range tests. |

All other classes and the remaining deterministic tests are mutated and run.

## The first full run

| Item | Value |
|---|---|
| Command | `./scripts/mutation.sh` |
| PIT | 1.30.1 |
| Java | Temurin 17.0.21 on Ubuntu 26.04 in WSL |
| Date | 2026-11-03 |
| Mutations | 818 |
| Killed | 716 |
| Timed out | 26 |
| Memory errors | 4 |
| Survived | 59 |
| No coverage | 13 |
| Score | 746 detected of 818, 91 percent |
| Test strength | 93 percent |
| Line coverage of the mutated classes | 735 of 754, 97 percent |
| Tests run | 6,829, 8.35 per mutation |

The 13 no-coverage mutants are lines whose only covering tests are the five excluded vector
classes. They are counted as missed, not hidden: the score would rise if those classes ran, but the
run would not finish. The 4 memory errors are minion JVM memory failures; PIT counts them as
detected, and this gate follows PIT's count while printing the number so a rise is visible.

The Windows desktop cannot run PIT at the moment: the machine sits at its commit limit because many
tool servers are resident, and a minion dies with a native memory allocation error. The measured run
therefore used WSL with the same JDK, the same tests, the same mutants and the same PIT version; the
scheduled GitHub job runs the same script on an Ubuntu runner.

## Equivalent mutants

None is known at the first full run. A survivor that no test can kill is added here with the reason
and the line, rather than lowering a threshold.
