## What and why

Describe the change and the reason for it. Link the hub specification section or the issue.

## Checklist

- [ ] Tests cover the change, and a bug fix has a test that failed before it.
- [ ] A new public member has Javadoc naming the C# member it corresponds to, a row in
      `docs/API.md`, and a runnable example when it starts a group.
- [ ] `./scripts/ci.sh` and `./gradlew checkAll` pass locally.
- [ ] `shared/` is unchanged, or the change comes from the shared-sync pull request.
- [ ] `CHANGELOG.md` has a line under `Unreleased`.
- [ ] The commit messages are Conventional Commits and carry no co-author or tool attribution.
