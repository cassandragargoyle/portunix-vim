---
name: implement-issue
description: 'Guide for implementing a portunix-vim issue — from reading the write-up in docs/issues/, through claiming it on GitHub, confirming understanding, creating a branch, implementing the code (with architecture docs next to the code), running the build and tests, up to committing and pushing the branch for testing (then /finish-branch). Use when the user wants to implement an issue, work on an issue from docs/issues/, or process an issue according to the project methodology. Examples: "implement issue 3", "/implement-issue 001", "implementuj issue #2", "pusť se do issue s lemmatizerem".'
argument-hint: [issue number or name]
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, AskUserQuestion
user-invocable: true
---

# Implement an issue

Argument: **$ARGUMENTS**

## Context (language and role)

- Communicate with me in **Czech**, but **do not translate or modify** issue titles,
  descriptions, code, commit messages, or technical terms — keep technical content in its
  original language
- Code, code comments and program texts are in English (see `AGENTS.md`)
- The workflow follows `docs/contributing/ISSUE-DEVELOPMENT-METHODOLOGY.md` (Phase 2:
  Implementation, Phase 4: Integration) and `docs/contributing/ISSUE-MANAGEMENT.md`

**IMPORTANT: NEVER add a "Generated with Claude Code" line, a `Co-Authored-By:` trailer or any
other AI attribution to code files, git commits or pull request descriptions.**

## STEP 0: Role check

If `CLAUDE.local.md` exists and sets a role other than **Developer**:

1. Inform the user that implementation requires the Developer role
2. Ask the user to switch to the Developer role
3. **STOP** — Do not continue until the Developer role is active

If no role is set, continue in the Developer role.

## STEP 1: Get the issue

If the argument contains an issue number or name, use it. Otherwise ask me for the issue number
or name and wait for my answer.

Numbering is GitHub-first: GitHub `cassandragargoyle/portunix-vim` assigns the number `N`, and the
write-up is `docs/issues/NNN-name.md` with `NNN` = `N` padded to three digits (`#3` and `003` are
the same issue). Use `N` for all `gh` calls below.

## STEP 2: Claim the issue (status + assignee check)

Before doing any work, verify the issue is free to work on. **GitHub is the single source of
truth for status and assignee.**

Run the checks in order:

1. **Locate the file.** If the file lives in `docs/issues/done/`, the issue is already **done** →
   **STOP**. Tell me the issue is archived as done and refuse to start work on it. If there is no
   write-up at all, tell me and offer to create it first (see `ISSUE-MANAGEMENT.md`).

2. **Get the current GitHub user:**

   ```bash
   gh api user --jq .login
   ```

   If `gh` is not installed or not authenticated, **STOP** and tell me: the claim check cannot run
   without `gh`; ask me to install/authenticate `gh` or to explicitly confirm I want to skip the
   claim check.

3. **Read the issue state and assignees:**

   ```bash
   gh issue view N --repo cassandragargoyle/portunix-vim --json number,title,state,assignees
   ```

4. **Status check.** If `state` is `CLOSED` → the issue is **done** → **STOP**. Tell me the issue
   is already closed on GitHub and refuse to start work on it.

5. **Assignee check:**
   - If it is assigned to **someone other than the current user** → **STOP**. Report who it is
     assigned to and refuse to continue. Tell me to ask that person to **release the issue**
     (unassign themselves) before I can pick it up.
   - If it is assigned to **the current user already** → it is mine, continue.
   - If it is **unassigned** → claim it for me:

     ```bash
     gh issue edit N --repo cassandragargoyle/portunix-vim --add-assignee @me
     ```

     Confirm the assignment succeeded before continuing.

Only once the issue is not done and is assigned to the current user, continue to STEP 3.

## STEP 3: Confirm understanding

1. Read the issue file from `docs/issues/` and the files it references
2. Restate the issue in your own words
3. Explain the required code changes (modules, classes, tests, docs)
4. Ask me for confirmation

**STOP** — Wait for my explicit confirmation before continuing.

## STEP 4: Create a branch

Create a git branch from an up-to-date `main`: `feature/NNN-<short-description>` or
`fix/NNN-<short-description>` for bug fixes.

- Use lowercase letters and hyphens for the description
- Example: `feature/001-vim-parser-lemmatizer`

Show the git command before running it. If the working tree is not clean, tell me and ask what to
do — never stash, reset or discard changes without my confirmation.

## STEP 5: Implementation

- Make all code changes in a single continuous phase
- Do NOT ask questions unless it is genuinely ambiguous
- Modify only the relevant files
- Follow the project's coding style (`docs/contributing/CODE-STYLE-JAVA.md` for Java), prefer
  minimal diffs
- Add tests for new functionality (`docs/contributing/JAVA-TESTING.md` for Java)
- Use `portunix container` commands for any container work, never `docker`/`podman` directly

### Architecture documentation next to the code

This is a multi-language repository (Java, Python, Go). The rule is the same in every language:
each source file has an architecture document next to it with the SAME base name and a `.md`
extension.

- Examples per language:
  - **Java**: `java/vim-parser/vim-parser-core/src/main/java/org/cassandragargoyle/portunix/vimparser/Main.java`
    -> `.../vimparser/Main.md`
  - **Python**: `python/<project>/src/<package>/module.py` -> `.../module.md`
  - **Go**: `go/<tool>/internal/foo/foo.go` -> `go/<tool>/internal/foo/foo.md`
- The document MUST start with a frontmatter block warning that only the code is the source of
  truth and that this file is meant for humans:

  ```markdown
  ---
  warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
  ---
  ```

- The document MUST contain Mermaid diagrams describing the structure and flow (e.g. class or
  package diagram and a sequence or flow diagram of the main path)
- Follow `docs/contributing/MARKDOWN-STYLE.md`
- Whenever you change a source file, update its matching `.md` document in the SAME commit so it
  stays in sync with the code
- If the source file has no matching `.md` document yet, create one as part of the change
- Do NOT create architecture docs for test files, generated code, or third-party/vendored sources
  (e.g. `.venv/`, `target/`, `node_modules/`)

### Pre-testing validation

Run the build and tests for every language you touched and report the result faithfully:

- **Java**: `make verify` in the repository root (unit + integration tests, same as CI)
- **Python**: `uv run pytest` in the project under `python/`
- **Go**: `go test ./...` and `go build -o .` in the project under `go/`

At the end, provide a summary:

- Modified files
- Added methods/classes
- Removed code
- Build and test results
- Key decisions

Ask me what I want:

- a) Accept as is
- b) Adjust specific parts
- c) Redo completely

**STOP** — Wait for my approval before continuing.

## STEP 6: Commit

Prepare the commit message:

```text
<type>(NNN): <short summary>

- key change 1
- key change 2

refs #N
```

Allowed types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

**STOP** — Ask me to confirm the commit message before committing.

## STEP 7: Push for testing

Per the methodology, the Developer **MUST NOT** merge into `main` before tester approval.

1. Push the branch so the tester can take it over:

   ```bash
   git push -u origin feature/NNN-<short-description>
   ```

2. Tell me the branch is ready for the Tester role, who creates the acceptance protocol
   `docs/testing/acceptance-NNN.md`

**STOP** — Implementation is done here; the merge follows only after tester approval.

---

## Next steps

Once the tester's acceptance protocol shows PASS, use `/finish-branch` to:

- Check the acceptance protocol and the READMEs
- Update the issue status to ✅ Implemented and archive it into `docs/issues/done/`
- Open the pull request into `main`, merge it and delete the feature branch
- Close the GitHub issue
