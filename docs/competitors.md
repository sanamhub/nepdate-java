# Other Java libraries for the Bikram Sambat calendar

Checked 2026-11-03 against Maven Central, deps.dev, JitPack and the repositories, using the selection
rules of [`shared/spec/BENCHMARK.md`](../shared/spec/BENCHMARK.md) section 7. Usage is the deps.dev
dependent count for the selected version, read on 2026-11-03. The maturity bar is: a first release
at least 12 months before 2026-11-03, a release or commit in the last 36 months, a licence, not
archived or deprecated, a build on the current toolchain, and at least one dependent.

The Java and Kotlin ecosystems share Maven Central, so the candidates are the same ones the Kotlin
port researched on 2026-11-02; the entries below were re-checked for this port.

## Candidates

| Library | Coordinates | Version | Dependents | First release | Last release | Licence | Verdict |
|---|---|---|---:|---|---|---|---|
| `shivathapaa/Nepali-Date-Picker` | `io.github.shivathapaa:nepali-date-picker-core-jvm` | 3.3.0 | 1 | 2024-09-14 | 2026-09-22 | MPL-2.0 | selected |
| `dhiraj-var/adbs` | JitPack `com.github.dhiraj-var.adbs:adbs-core` | v4.0.0 | not on Maven Central | 2026 | 2026-09 | MIT | fails the 12-month bar and has no deps.dev count |
| `AagitoEx/nepdate` | `io.github.aagitoex:nepdate` | 0.0.1-beta01 | 0 | 2022-07-04 | 2022-09-05 | MIT | fails the dependent bar and is an abandoned beta |
| `theGBguy/KomposeNepaliDatePicker` | `io.github.thegbguy:nepali-date-picker` | 1.0.2 | 0 | 2024-07-01 | 2024-10-26 | MIT | fails the dependent bar |
| `keyrunHORNET/date_picker_converter` | JitPack only | 3.0.0 | not on Maven Central | 2016 | 2021-10 | Apache-2.0 | fails the 36-month bar |
| `io.github.rongaru:nepali-calender` | `io.github.rongaru:nepali-calender` | 1.0.1 | 0 | 2024-04-05 | 2024-04-05 | MIT | fails the dependent bar |
| `sujit-sharma/bikram-sambat` | not published | 2.0.0 | none | 2026-09 | 2026-10 | MIT | fails the 12-month bar and is not on Maven Central |

`io.github.aagitoex:compose-nepcal` is a Compose date picker with no conversion API, so it is not a
candidate. Neither `java.time.chrono`, ThreeTen-Extra nor ICU4J has a Bikram Sambat calendar.

## Selected library

`io.github.shivathapaa:nepali-date-picker-core-jvm` 3.3.0 is the only candidate that passes. The
harness calls `NepaliDateConverter.INSTANCE.convertNepaliToEnglish(y, m, d)` for B1 and
`convertEnglishToNepali(y, m, d)` for B2, and reads `getYear()`, `getMonth()` and `getDayOfMonth()`
from the returned `CustomCalendar`. The published POM pulls the Compose runtime that the picker
widgets use; the conversion classes do not need it and it is not on Maven Central, so the benchmark
excludes `androidx.compose.runtime`, `org.jetbrains.compose.runtime`, `...ui` and `...foundation`.

## Correctness

`AgreementCheck` in the benchmark module compares the 64 dates in both directions. shivathapaa
differs from `shared/data` on BS 2086-07-23, 2088-01-04 and 2089-06-10, in both directions: it
agrees on 61 of 64. A fast wrong answer is listed in the benchmark table, never ranked first.

## Sources

- https://repo1.maven.org/maven2/io/github/shivathapaa/nepali-date-picker-core/maven-metadata.xml
- https://api.deps.dev/v3alpha/systems/MAVEN/packages/io.github.shivathapaa%3Anepali-date-picker-core/versions/3.3.0:dependents
- https://github.com/shivathapaa/Nepali-Date-Picker
- https://github.com/dhiraj-var/adbs
- https://github.com/AagitoEx/nepdate
- https://github.com/theGBguy/KomposeNepaliDatePicker
- https://github.com/keyrunHORNET/date_picker_converter
- https://github.com/sujit-sharma/bikram-sambat
