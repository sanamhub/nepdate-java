# ADR-0009: CI without the hub remote

- **Status:** accepted
- **Date:** 2026-11-03

## Context

`shared-check` downloads the hub commit named in `shared/VERSION` and compares `shared/` with it,
and checks this port's version against the hub's `spec/versions.json`. While the port is being built,
the hub branch that adds the Java entry and moves the line to `1.0` is not merged, so that job fails.
The PHP port hit the same situation and documented it rather than weakening the check.

## Decision

1. **CI is the local command.** `.github/workflows/ci.yml` runs `scripts/ci.sh`, and a green local
   run means a green CI run. The script's list is the only place the gates are ordered.
2. **The matrix is real.** Linux, macOS and Windows, each over Java 17, 21 and 25. The toolchain is
   the same on all nine cells; nothing is skipped on an operating system.
3. **`shared-check` calls the hub's reusable workflow pinned to a commit.** It compares `shared/`
   with the hub commit in `shared/VERSION` and checks this port's version against that commit's
   `spec/versions.json`. A port with no entry there may declare a later line (hub ADR-0008), so the
   job is green before the 1.0 line moves. The hub branch merges at the 1.0 release; after that
   merge a shared-sync pull request moves `shared/VERSION` to the release-state commit, and only
   then does the version check compare against the 1.0 line. The runbook puts both steps in the
   release window and says no tag is cut before the job is green.
4. **The workflows are pinned by commit SHA** with the version in a comment, top-level
   `permissions: contents: read`, raised per job only where needed. No `pull_request_target`, no
   secrets in a job that runs a pull request's code.
5. **A Windows job with bash blocks sets `defaults: run: shell: bash`,** because four ports lost a
   release to that omission.

## Consequences

- A contributor can reproduce every gate with `./scripts/ci.sh`.
- The first pull request is green except for `shared-check`, as the runbook states.
- The hub branch must land before `v1.0.0-rc.1`.

## Alternatives considered

- **Calling `shared-check` without the sdk and version inputs:** would hide the version drift the
  check exists to catch, and is a weakening.
- **Pinning the hub workflow to a branch:** not reproducible.
