# Acceptance Protocol - Issue #3

**Issue**: CI: provide table-detector artifacts without access to private portunix-plugins
**Branch**: feature/003-ci-table-detector-artifacts
**Commit**: `7c6c039` (single repository id `github`), on top of `b216483`
**Tester**: Zdenek (with Claude Code, Tester role)
**Date**: 2026-11-09
**Testing OS**: clean `ubuntu:24.04` container via `scripts/container-build.py` (main `0cca6b4`);
host Linux 7.0 amd64, Java 21.0.10, Maven 3.9.12

## Test Summary

- Total test scenarios: 8
- Passed: 7
- Failed: 0
- Skipped: 1 (T6 — CI run on GitHub, branch not pushed yet)

## Test Results

### Functional Tests

- **T1** ✅ PASS: **Given** a clean container with an empty `~/.m2` and a `settings.xml` with the single server `github`, **when** the CI Maven command runs on `7c6c039`, **then** `table-detector-api`/`-pdf` `1.0.3` are resolved from GitHub Packages and the build passes
- **T2** ✅ PASS: **Given** a build of the branch, **when** the `vim-parser-table` plugin ZIP is inspected, **then** it bundles `lib/table-detector-api-1.0.3.jar` and `lib/table-detector-pdf-1.0.3.jar`
- **T3** ✅ PASS: **Given** no credentials, **when** a `table-detector` artifact is downloaded from GitHub Packages, **then** the registry answers `401`, which confirms the documented need for a token
- **T4** ✅ PASS: **Given** the branch, **when** the workflow, POMs and docs are searched, **then** no checkout of portunix-plugins, no `plugins/table-detector` path, no `1.0.3-SNAPSHOT` and no `github-portunix-*` ids remain (only history notes mention portunix-plugins)
- **T5** ✅ PASS: **Given** the workflow, **when** the `setup-java` step and the POM are compared, **then** `server-id: github` equals the only GitHub Packages repository id in `java/vim-parser/pom.xml`, with `packages: read` permission
- **T6** ⏭️ SKIP: **Given** the pushed branch, **when** the `vim-parser` workflow runs (`pull_request` or `workflow_dispatch`), **then** it is green with `GITHUB_TOKEN` only
- **T7** ✅ PASS: **Given** the docs, **when** build prerequisites are read, **then** `README.md`, `README.cs.md`, `AGENTS.md` and `GITHUB-WORKFLOW.md` describe the repository id `github` with URL `https://maven.pkg.github.com/cassandragargoyle/*` and the `read:packages` PAT
- **T8** ✅ PASS: **Given** the changed Markdown files, **when** `markdownlint-cli2` runs, **then** it reports no findings for them

### Evidence

- T1: `uv run scripts/container-build.py --ref 7c6c039` → `BUILD SUCCESS`, exit code 0 in 74 s;
  363 tests, 0 failures, 0 errors, 7 skipped (MorphoDiTa model tests); TK stemmer tests excluded
  as in CI; container removed afterwards
- T2: `unzip -l table-plugin-0.1.8-SNAPSHOT.zip` lists both `1.0.3` JARs
- T3: `curl` without credentials → HTTP `401`; with a token → HTTP `302` (redirect to storage)
- T5: POM repository ids `central`, `github`; workflow `server-id: github`,
  `server-username: GITHUB_ACTOR`, `server-password: GITHUB_TOKEN`

### Regression Tests

- [x] All vim-parser modules build and their unit and integration tests pass in a clean
  environment (T1)
- [x] Plugin packaging unchanged apart from the table-detector version (T2)
- [ ] Full `make verify` on the host: TK stemmer tests fail with `UnsatisfiedLinkError` (native TK
  library not installed); unrelated to this change and excluded in CI the same way
- [ ] Windows: not tested (change touches only Maven configuration and the Linux CI workflow)

## Findings

1. Existing developer setups with a `github` server in `~/.m2/settings.xml` keep working; the
   earlier `github-portunix-*` server ids are no longer needed
2. Container toolchain (not part of this change): `portunix install java` provided `javac` 21.0.8
   (Temurin), while `portunix install maven` pulled the distribution JRE, so `java` on `PATH` is
   21.0.12; the build is consistent only because the script derives `JAVA_HOME` from `javac`

## Final Decision

**STATUS**: CONDITIONAL

Required before approval: push the branch and confirm T6 (green `vim-parser` workflow on the pull
request or via `workflow_dispatch`).

**Approval for merge**: NO (pending T6)
**Date**: 2026-11-09
**Tester signature**: Zdenek
