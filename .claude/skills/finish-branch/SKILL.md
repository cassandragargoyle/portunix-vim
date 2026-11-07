---
name: finish-branch
description: 'Finish work on a portunix-vim feature branch — acceptance protocol check, README check, issue archival into docs/issues/done/, pull request into main, branch clean-up and closing the GitHub issue. Use when the user wants to finish, merge or close a feature branch. Examples: "finish the branch", "/finish-branch", "merge the feature branch into main", "dokonči branch", "domerguj feature".'
argument-hint: [branch name]
allowed-tools: Read, Glob, Grep, Bash, AskUserQuestion, Edit
user-invocable: true
---

# Finish a feature branch

Argument: **$ARGUMENTS**

## Context (language and role)

- Communicate with me in **Czech**, but **do not translate or modify** issue titles, code,
  commit messages, or technical terms — keep technical content in its original language
- The workflow follows `docs/contributing/ISSUE-DEVELOPMENT-METHODOLOGY.md` (Phase 4:
  Integration), `docs/contributing/ISSUE-MANAGEMENT.md` and
  `docs/contributing/GITHUB-WORKFLOW.md`
- GitHub `cassandragargoyle/portunix-vim` is both the git `origin` and the issue tracker; the
  standard path into `main` is a pull request

**IMPORTANT: NEVER add a "Generated with Claude Code" line, a `Co-Authored-By:` trailer or any
other AI attribution to code files, git commits or pull request descriptions.**

## STEP 1: Establish the context

1. Use the branch from the argument, otherwise the current branch (`git branch --show-current`).
   It should follow `feature/NNN-<short-name>` or `fix/NNN-<short-name>` (`NNN` = GitHub issue
   number padded to three digits, `N` = the plain number for `gh`). A branch without an issue
   number (e.g. `docs/...`) has no issue to archive — say so and skip STEP 4.
2. The target branch is `main` unless I say otherwise
3. Verify the branch exists, contains commits against `main`
   (`git log --oneline main..<branch>`) and the working tree is clean. If it is not clean, tell
   me and ask what to do — never stash, reset or discard changes without my confirmation.

## STEP 2: Acceptance protocol check

The methodology does not allow a merge without the tester's acceptance protocol.

1. Find `docs/testing/acceptance-NNN.md` on the branch
2. Verify it contains `**STATUS**: PASS` and `**Approval for merge**: YES`
3. Verify it covers the last commit of the branch: the commit in its header (`**Commit**`) is the
   branch HEAD, or it is followed only by commits adding the protocol (`test(NNN): …`).
   Otherwise the tests did not see newer changes and a re-test is needed.

If the protocol **does not exist, is not PASS, or does not cover the latest changes**:

- Tell me a merge is not allowed by the methodology without tester approval
- Ask: "Chceš pokračovat přesto? (hotfix / výjimka / větev bez issue?)"
- If I do not confirm an exception → **STOP**

## STEP 3: README check

Verify the READMEs match the state after the change. Look at what the branch touched:

```bash
git diff --name-only main...<branch>
```

1. **Module README** (`java/vim-parser/README.md` and `README.cs.md`, and the README of any
   touched project under `python/` or `go/`):
   - new or changed CLI options are in the CLI options table and in the usage examples
   - the module structure lists every module in `java/vim-parser/pom.xml`
   - notes about dependencies (e.g. `table-detector.version`) and limitations are not outdated
2. **Root README** (`README.md` and `README.cs.md`): the description of the tools and the repository
   layout still say what the repository contains today
3. English and Czech versions say the same thing

If something does not match, fix it and commit it to the feature branch:

```text
docs(NNN): update READMEs for <short summary>
```

If the READMEs match, skip the step and say so in the summary.

## STEP 4: Issue update

Ask me: "Je tímto issue kompletně dokončeno?"

### If YES

Do the archival on the feature branch, so it lands in `main` together with the change:

1. Edit the issue file `docs/issues/NNN-name.md`:
   - Change `**Status**: 📋 Open` to `**Status**: ✅ Implemented`
   - Add the completion date
2. Archive it with history (supporting files of the issue go along; the acceptance protocol stays
   in `docs/testing/`):

   ```bash
   git mv docs/issues/NNN-name.md docs/issues/done/NNN-name.md
   ```

3. The file is now one level deeper: fix relative links pointing outside `docs/issues/`
   (`](../` → `](../../`), including the link to the acceptance protocol
   (`../testing/acceptance-NNN.md` → `../../testing/acceptance-NNN.md`), and check that every link
   resolves. Also fix links to the issue from other files (`grep -rn "issues/NNN-" .`).
4. Commit:

   ```text
   docs(NNN): archive — implementation complete
   ```

### If NO

Skip the status update. The issue stays open for further work and the PR uses `refs #N` instead
of `Closes #N`.

## STEP 5: Pull request and merge

Push the branch (`git push -u origin <branch>`) and ask me for the preferred procedure.

### Option A: Pull request (default)

Prepare the PR description and show it to me:

- **Summary**: What this PR does
- **Motivation**: Why — `Closes #N` (or `refs #N` if the issue stays open)
- **Changes**: List of changes
- **Testing**: Build and test commands run, link to `docs/testing/acceptance-NNN.md`

```bash
gh pr create --repo cassandragargoyle/portunix-vim --base main \
  --title "<type>(NNN): <short summary>" --body-file <description file>
```

Write the description file to the scratchpad, not into the repository. CI
(`.github/workflows/vim-parser.yml`) must pass before the merge.

**STOP** — Ask me before merging. Then merge and delete the branch:

```bash
gh pr merge <PR> --repo cassandragargoyle/portunix-vim --merge --delete-branch
git switch main
git pull --ff-only
git branch -d <branch>
```

### Option B: Direct merge (exception only, e.g. a hotfix I explicitly ask for)

```bash
git switch main
git pull --ff-only
git merge --no-ff <branch>
git push origin main
git branch -d <branch>
git push origin --delete <branch>
```

**STOP** — Wait for my confirmation before merging and before pushing `main`.

## STEP 6: Close the GitHub issue

If the issue was archived in STEP 4:

1. Check it was closed by the merge (`Closes #N`); otherwise close it:

   ```bash
   gh issue close N --repo cassandragargoyle/portunix-vim --comment "Implemented in #<PR>."
   ```

2. Point the link in the GitHub issue body to the `docs/issues/done/` path
   (`gh issue edit N --repo cassandragargoyle/portunix-vim --body-file <file>`)

## STEP 7: Summary

Show a summary:

- Acceptance protocol: PASS / exception (and why)
- READMEs updated: yes / not needed
- PR / merge into branch: `<name>` (PR link)
- Feature branch deleted: local yes/no, remote yes/no
- Issue status updated and archived: yes / no / skipped (and why)
- Next steps (if any)
