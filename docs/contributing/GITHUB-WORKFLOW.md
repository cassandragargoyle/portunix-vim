# GitHub Workflow for Portunix VIM

## Overview

`portunix-vim` is developed directly on **GitHub** — it is the single source of truth for both
development and publication. The repository is fully public, including issue write-ups
(`docs/issues/`) and project instructions (`AGENTS.md`, imported by `CLAUDE.md`).

## Everyday Development

```bash
git switch -c feature/my-change
git commit -m "feat(NNN): short description"
git push -u origin feature/my-change
```

Open a Pull Request on GitHub, get it reviewed, and merge into `main`.

## Remotes

```text
origin -> github.com/cassandragargoyle/portunix-vim
```

## Community Contributions

External contributors use the standard fork → PR model against
`cassandragargoyle/portunix-vim`.

## Pull Request Checks (CI)

The workflow [`.github/workflows/vim-parser.yml`](../../.github/workflows/vim-parser.yml) runs on
pull requests to `main`, on pushes to `main` and manually (`workflow_dispatch`) when
`java/vim-parser/**` or the workflow changes.

| Step           | What it does                                                                                 |
| -------------- | -------------------------------------------------------------------------------------------- |
| Set up Java    | Temurin 21; writes `settings.xml` with server `github` (`GITHUB_ACTOR` / `GITHUB_TOKEN`)     |
| Build and test | `mvn -B verify` in `java/vim-parser` (unit + integration tests)                              |

TK stemmer tests need the native TK library, which the runner does not have; they are excluded in
CI the same way as in portunix-plugins.

## Notes

- Do not commit secrets — `NOTES.md`, `.claude/settings.local.json`, `CLAUDE.local.md`, and
  `.env*` are gitignored and stay local
- Keep commit messages in English and reference the issue number where applicable
