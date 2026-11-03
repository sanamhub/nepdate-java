# Release runbook

The family releases every SDK on one `MAJOR.MINOR` line from one `v*` tag per port
([hub ADR-0007](https://github.com/sanamhub/nepdate/blob/main/docs/adr/0007-one-version-line-for-every-sdk.md)).
This port releases `io.github.sanamhub:nepdate-java` and `io.github.sanamhub:nepdate-java-patro`
together, with the same version. The version lives in `gradle.properties` and nowhere else.

## One-time owner steps, in order

1. **Repository settings.** Protect `main`: a pull request, one approval, the CI checks green, no
   force-push, no deletion. Enable Dependabot alerts and automated security fixes, the dependency
   graph and private vulnerability reporting.
2. **Tag protection.** A tag rule for `v*`, so only the owner can move a release tag.
3. **The `production` environment.** Create it in Settings, Environments, with a required reviewer
   (the owner) and a tag rule matching `v*` only.
4. **The secrets in `production`.** Four values, copied from the Kotlin port, which already publishes
   under the same namespace:
   - `ORG_GRADLE_PROJECT_mavenCentralUsername`
   - `ORG_GRADLE_PROJECT_mavenCentralPassword`
   - `ORG_GRADLE_PROJECT_signingInMemoryKey` (the ASCII-armoured private key)
   - `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`

   The release key's fingerprint is in `.github/workflows/release.yml`; a different key in the secret
   fails the signature check before the upload.
5. **Maven Central.** The `io.github.sanamhub` namespace is already verified for the Kotlin port.
   Confirm that both new artifact IDs can be published under it; there is no separate pending
   publisher for Maven Central, unlike the registries with trusted publishing.
6. **The label.** Create the `published-check` label used by `verify-published.yml`.
7. **The hub branch.** Merge the hub branch that adds Java to `spec/versions.json`,
   `spec/packages.json`, `spec/API.md` and the site. `shared-check` is the one red job until then and
   no tag is cut before it is green.
8. **Sync after the hub merge.** The reusable `shared-check` reads the hub at the commit in
   `shared/VERSION`, and its version check uses that commit's `spec/versions.json`. Merge the hub
   branch, run the `shared-sync` workflow (or `py tools/shared_sync.py .`), and merge its pull
   request, so `shared/VERSION` names the hub commit that has the `java` entry and the 1.0 line.
   Until that happens the job reports `java 1.0.0-rc.1: major.minor 1.0 is not the hub line 0.2`.

## Every release

1. Land the changes on `main` with their changelog lines under `## [Unreleased]`.
2. Confirm `./scripts/ci.sh` and the hub's `shared-check` are green on that commit.
3. Bump `version=` in `gradle.properties` if the line moved, and add the version's own dated section
   to `CHANGELOG.md`:

   ```markdown
   ## [1.0.0-rc.1] - 2026-11-03
   ```

   `scripts/release-check.sh v1.0.0-rc.1` must pass; it checks the manifest, the tag and that
   heading. The heading is the one the GitHub Release notes are cut from.
4. Open the bump pull request, wait for CI, and merge it.
5. Tag the merged commit and push the tag:

   ```sh
   git checkout main
   git pull
   git tag -a v1.0.0-rc.1 -m "v1.0.0-rc.1"
   git push origin v1.0.0-rc.1
   ```

6. The release workflow's `preflight` checks the tag against `main`, the manifest and the changelog;
   `ci` runs `scripts/ci.sh`; `publish` waits for the `production` approval, signs both artifacts,
   verifies every `.asc` against the release key, uploads to the Central Portal and creates the
   GitHub Release. `verify-published` then resolves both artifacts from Maven Central on Linux, macOS
   and Windows.
7. Approve the `production` environment only after reading the preflight and CI logs.
8. Confirm the artifacts on
   [central.sonatype.com](https://central.sonatype.com/artifact/io.github.sanamhub/nepdate-java) and
   that `verify-published` is green. Update the hub's `spec/versions.json` entry if the release
   changed a version the site shows.

## If something fails

- **A tag on the wrong commit.** Delete it (`git push origin :v1.0.0-rc.1`, `git tag -d
  v1.0.0-rc.1`), fix `main`, tag again. Never move a tag that has been used for an upload.
- **A rejected deployment.** Read the Portal's message; a missing signature, sources or Javadoc jar
  is caught by the release job before the upload, so a rejection means the Portal's checks, not the
  local ones.
- **A published artifact that does not resolve.** The `verify-published` job fails and opens one
  issue labelled `published-check`. Do not delete a version; publish a patch.
- **A `shared/` drift.** Re-sync with the hub's `shared-sync` workflow and merge the pull request; do
  not edit `shared/` by hand.

## The dry run

`release.yml` also accepts `workflow_dispatch` with a tag name. That runs the preflight in draft mode
and the full CI, and stops before signing and uploading, so the pipeline can be exercised before the
tag exists.
