# Security policy

## Supported versions

Only the latest release receives fixes.

## Reporting a vulnerability

Report privately through
[GitHub security advisories](https://github.com/sanamhub/nepdate-java/security/advisories/new).
Do not open a public issue.

Include the input that triggers it, the Java version, the platform and the artifact version. Expect
a first reply within a week.

A wrong calendar result, where nepdate-java and the shared data give the same wrong answer, belongs
to the hub: [sanamhub/nepdate security policy](https://github.com/sanamhub/nepdate/security/policy).

## Dependencies and scanning

The two published artifacts have no runtime dependency: the core needs `java.base` and `java.time`,
the patro needs the core. JSpecify is a compile-only annotation library and is not required at run
time. There is no native code, no network access and no reflection beyond standard Java
serialization, which reads the canonical `YYYY-MM-DD` text through the public parser.

CodeQL supports Java, so `.github/workflows/codeql.yml` scans the library sources and the GitHub
Actions workflows. The test vectors, static analysis (`./gradlew spotbugsMain`), the generator
check, the line-coverage gate and the scheduled PIT run are the rest of the check. Scorecard and
dependency review run on the repository, and the release workflow verifies every signature before
an artifact is uploaded.
