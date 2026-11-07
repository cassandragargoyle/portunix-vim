# Issues Documentation

## Overview

This directory keeps the **long-form write-up** for each issue as `NNN-name.md`, where `NNN` is
the **GitHub issue number**. Active issues sit directly in this directory, archived ones in
[`done/`](done/).

There is no overview table here on purpose. The **canonical issue list lives on GitHub** — the
single source of truth for status, assignee, and labels:

- **Open issues** → <https://github.com/cassandragargoyle/portunix-vim/issues>
- **Closed issues** → <https://github.com/cassandragargoyle/portunix-vim/issues?q=is%3Aissue+is%3Aclosed>

## Numbering

Issues are **GitHub-first**: the number is assigned by GitHub when the issue is created, and the
local file adopts that exact number. GitHub issues and pull requests share one counter, so numbers
may skip where a PR consumed one.

## Creating a New Issue

1. Create the GitHub issue first — GitHub assigns the number `N`:

   ```bash
   gh issue create --repo cassandragargoyle/portunix-vim \
     --title "Short descriptive title" \
     --body "Basic description. Full details: docs/issues/N-name.md"
   ```

1. Write the detail file `docs/issues/N-name.md` with the full context, acceptance criteria and
   implementation notes
1. Reference the number in branches and commits: `feat(N): ...`, `fixes #N`

## Archiving a Completed Issue

When an issue reaches a terminal status (✅ Implemented, ❌ Closed, ➡️ Moved):

1. Update the status in the write-up
1. Move the file: `git mv docs/issues/N-name.md docs/issues/done/N-name.md`
1. Close the GitHub issue: `gh issue close N`

## History

`vim-parser` was moved here from
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) with its full git history.

- Issue numbers in commit messages **before the move** (e.g. `feat(159): ...`) refer to
  portunix-plugins issues
- Completed vim-parser issues stay in portunix-plugins: write-ups in
  [`docs/issues/done/`](https://github.com/cassandragargoyle/portunix-plugins/tree/main/docs/issues/done)
  and the GitHub
  [`plugin-vim-parser` label](https://github.com/cassandragargoyle/portunix-plugins/issues?q=label%3Aplugin-vim-parser)
- Open issues were recreated here, each with an **Origin** line:

| portunix-vim | portunix-plugins | Title                                           |
| ------------ | ---------------- | ----------------------------------------------- |
| #1           | #32              | VIM Parser - Lemmatizer Plugin                  |
| #2           | #36              | VIM Parser - MorphoDiTa Lemmatizer Backend Plugin |
