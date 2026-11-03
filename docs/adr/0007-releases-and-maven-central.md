# ADR-0007: Releases and Maven Central

- **Status:** accepted
- **Date:** 2026-11-03

## Context

The family releases every SDK on one `MAJOR.MINOR` line from one tag per port (hub ADR-0007). Maven
Central has no OIDC trusted publishing: a deployment needs a Sonatype Central Portal user token and
a GPG key, and the artifacts must carry sources, Javadoc and a signature. The Kotlin port already
solved this with the Vanniktech publishing plugin and a `production` environment; copying that shape
keeps the owner's steps and the verification the same.

## Decision

1. **Coordinates and version.** `io.github.sanamhub:nepdate-java` and
   `io.github.sanamhub:nepdate-java-patro`, both `1.0.0-rc.1`, the family line's first release. The
   version lives in `gradle.properties`; the tag is `v` plus that version.
2. **The Vanniktech plugin with the Central Portal.** `publishToMavenCentral(automaticRelease =
   true, validateDeployment = VALIDATED)` registers the upload and waits for the Portal's check.
   `signAllPublications()` runs only when `signingInMemoryKey` is present, so a local
   `publishToMavenLocal` works without the key and the release job checks every `.asc` before it
   uploads.
3. **The release job has the `production` environment.** The tag rule matches `v*`; only that
   environment holds `ORG_GRADLE_PROJECT_mavenCentralUsername`, `...Password`,
   `...signingInMemoryKey` and `...signingInMemoryKeyPassword`. The job also verifies each signature
   against the release key fingerprint before the upload, and publishes only when the ref is a `v*`
   tag.
4. **Every pull request runs the release dry run.** `publishToMavenLocal` builds, signs nothing and
   runs without secrets, and a list check asserts exactly the expected files. The preflight checks
   the tag against `main`, the version against `gradle.properties` and the changelog heading.
5. **The changelog heading is required at release time.** `scripts/release-check.sh <tag>` requires
   `## [<version>] - YYYY-MM-DD`; the CI and dry-run mode (`--draft`) accepts the `Unreleased`
   heading until the bump pull request, as the Dart and PHP ports do.
6. **`verify-published` runs after a release and on a schedule.** It resolves the artifacts from
   Maven Central in a throwaway Gradle and Maven project on Linux, macOS and Windows, converts a
   date and reads a patro event, and opens one issue if a scheduled run fails.

## Consequences

- The owner's pre-tag steps are: the `production` environment, its `v*` tag rule, the four secrets,
  the Central Portal namespace (already verified for the Kotlin port), and the `published-check`
  label.
- A broken POM, a missing Javadoc jar or an unsigned file fails before the Portal, not after.
- The release workflow can be dry-run from the Actions page with any tag name.

## Alternatives considered

- **`maven-publish` and a hand-written Central bundle:** more steps and no signature check at upload.
- **GitHub Packages:** not the registry Java users expect and not in the family's package data.
- **Publishing from a local machine:** no environment approval and no reproducible log.
