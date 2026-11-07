# Issue #3: CI: Provide table-detector Artifacts Without Access to Private portunix-plugins

## Summary

The vim-parser CI workflow cannot build because it needs `table-detector-api` and
`table-detector-pdf` from the private repository `cassandragargoyle/portunix-plugins`, which the
workflow token cannot read. Provide these artifacts to CI in a way that works for a public
repository.

**Status**: 📋 Open
**Priority**: High
**Component**: `.github/workflows/vim-parser.yml`, `java/vim-parser/vim-parser-table`
**Created**: 2026-11-07
**GitHub**: #3
**Related**: portunix-plugins #231 (move of vim-parser to portunix-vim)

## Problem Description

`vim-parser-table` depends on `table-detector-api` and `table-detector-pdf`
(version `table-detector.version` in `java/vim-parser/pom.xml`, currently `1.0.3-SNAPSHOT`).
They are not published to any Maven repository; they exist only as sources in
`portunix-plugins/plugins/table-detector`.

The workflow `.github/workflows/vim-parser.yml` therefore:

1. Checks out `cassandragargoyle/portunix-plugins` (`actions/checkout` with `repository:`)
1. Runs `mvn install -DskipTests` for `plugins/table-detector`
1. Runs `mvn verify` in `java/vim-parser`

Step 1 fails with `Not Found - https://docs.github.com/rest/repos/repos#get-a-repository`: the
automatic `GITHUB_TOKEN` is scoped to `portunix-vim` only and `portunix-plugins` is private.
The build never starts, so no pull request gets a build or test signal.

Local builds work only because `table-detector` is already installed in `~/.m2`.

## Options

| Option                                  | Effort | Pros                                         | Cons                                                                 |
| --------------------------------------- | ------ | -------------------------------------------- | -------------------------------------------------------------------- |
| 1. Publish to GitHub Packages           | Medium | Clean Maven dependency, no secrets in vim CI | Publish workflow in portunix-plugins, release discipline for versions |
| 2. PAT secret for the checkout          | Low    | Minimal change                               | Token tied to a personal account, expires, public repo needs private |
| 3. Move table-detector to portunix-vim  | High   | Self-contained                               | `text-extractor` and `fulltext` in portunix-plugins depend on it     |
| 4. Make portunix-plugins public         | Low    | Workflow works unchanged                     | Organizational decision outside this repository                     |

**Recommended**: option 1, unless portunix-plugins becomes public (option 4).

### Option 1 Outline

1. In portunix-plugins, add a workflow that runs `mvn deploy` for `plugins/table-detector` to
   `https://maven.pkg.github.com/cassandragargoyle/portunix-plugins` (or the shared
   `cassandragargoyle/api` package registry already used for `org-cassandragargoyle-api`), with
   `distributionManagement` in the table-detector parent POM
1. Publish a release version (not `-SNAPSHOT`) and set `table-detector.version` to it
1. Make the package readable for portunix-vim (package visibility or repository access settings)
1. Add the package repository to `java/vim-parser/pom.xml` (`<repositories>`, id `github`, so the
   existing `setup-java` `server-id: github` credentials apply)
1. Remove the "Checkout portunix-plugins" and "Install table-detector" steps from the workflow
1. Update the build prerequisites in `README.md`, `README.cs.md` and `AGENTS.md`

## Acceptance Criteria

1. The `vim-parser` workflow passes on `main` and on pull requests without access to private
   repositories through personal credentials (unless option 2 is explicitly chosen)
1. `mvn clean verify` in `java/vim-parser` resolves `table-detector-*` from a clean `~/.m2`
1. Build prerequisites in the READMEs and `AGENTS.md` match the chosen solution

## Testing

- Re-run the `vim-parser` workflow (`workflow_dispatch`) and check it is green
- Locally: `mvn -Dmaven.repo.local=$(mktemp -d) clean verify` in `java/vim-parser`

## Rollback

Revert the workflow and POM changes; local builds keep working with a locally installed
`table-detector`.
