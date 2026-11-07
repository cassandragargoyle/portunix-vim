---
title: Issue Management
description: Workflow for creating, tracking, and archiving issues in portunix-vim — GitHub-first, where GitHub assigns the number and each issue keeps a long-form mirror in docs/issues/NNN-*.md (archived to done/ on completion). No overview tables.
category: contributing-guide
ai_load: scoped
status: active
language: en
last_updated: 2026-11-07
related:
  - ISSUE-DEVELOPMENT-METHODOLOGY.md
---

# Issue Management Guidelines

## Purpose

This document defines the process for creating and managing issues in CassandraGargoyle projects that have GitHub repositories. It covers two primary
workflows for issue creation and ensures proper synchronization between internal team discussions and public GitHub tracking.

## Issue Creation Models

The **primary model for Portunix VIM is Model A: GitHub-First** (below).
Models B and C are retained as historical context only.

> **GitHub is the single source of truth for the issue list.** There are **no
> overview tables** in `docs/issues/README.md` or `done/README.md` — a
> shared Markdown table edited by everyone caused constant merge conflicts and was
> removed. Status, assignee, and labels live on GitHub; the repo keeps only the
> long-form write-up per issue.

### Model A: GitHub-First with Local Detail Mirror (Recommended for Portunix VIM)

**When to use**: The default for every new issue in this project — bug, feature,
or enhancement.

**Overview**: The issue is **created on GitHub first**, so **GitHub assigns the
number `N`**. The GitHub issue carries only the *basics* (title + a short
description + a link to the detail file). The full write-up lives in the repo at
`docs/issues/N-name.md`, where the filename number **equals the GitHub
issue number**. No tables are maintained.

#### Workflow

1. **Create the GitHub issue** (GitHub assigns `N`):

   ```bash
   gh issue create --repo cassandragargoyle/portunix-vim \
     --title "Short descriptive title" \
     --body "One-line summary. Full details: docs/issues/N-name.md"
   ```

2. **Write the detail file** `docs/issues/N-name.md` (problem, acceptance
   criteria, testing, related issues). Use the GitHub number `N` in the filename
   and in the `# Issue #N:` heading.

3. **Update the GitHub body** with a working link to the detail file once `N` is
   known (`gh issue edit N --body …`).

4. **Reference `N`** in branches/commits: `feat(N): …`, `fixes #N`.

5. **Archive on completion**: `gh issue close N` and
   `git mv docs/issues/N-name.md docs/issues/done/`.

In portunix-plugins the `create-issue` skill (`/create-issue`) automates this flow; portunix-vim
does not have it yet, so the steps are done manually.

> **Numbering note.** GitHub issues and pull requests share one counter, so
> numbers may skip where a PR consumed one. Never invent a number — always take
> the one GitHub returns.

#### Issues migrated from portunix-plugins

`vim-parser` was moved here from
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins), where it used the
GitHub-first model with the plugins' own numbering. Open vim-parser issues were recreated here
with new numbers (`#1` ← `portunix-plugins#32`, `#2` ← `portunix-plugins#36`), each with an
**Origin** line. Completed ones stay in portunix-plugins. References to issues there are written
as `portunix-plugins#NN` — see [docs/issues/README.md](../issues/README.md#history).

### Model B: GitHub-First Issue Creation  

**When to use**: For bugs, feature requests, or issues that can be discussed publicly from the start and don't require internal planning.

**Process**:

1. **Create GitHub Issue**

   - Go to the project's GitHub repository
   - Click "Issues" → "New issue"
   - Choose appropriate template (Bug Report, Feature Request, etc.)
   - Fill in all required fields:
     - Clear, descriptive title
     - Detailed description
     - Steps to reproduce (for bugs)
     - Expected vs actual behavior
     - Environment details
     - Labels (bug, enhancement, question, etc.)
     - Assign to milestone if applicable

2. **Reference in Team Communication**
   - Share GitHub issue link in team channels
   - Use issue number in commits: `Fix authentication bug (closes #42)`
   - Reference in pull requests: `Fixes #42`

3. **Track Progress**
   - Update issue status as work progresses
   - Add comments for significant updates
   - Close when resolved with summary

**Example GitHub Issue Creation**:

```text
Title: PowerShell installation fails on Fedora 40

Description:
## Bug Report

**Environment:**
- OS: Fedora 40
- Portunix version: 1.2.3
- Installation method: portunix install powershell --variant fedora

**Steps to reproduce:**
1. Run `portunix install powershell --variant fedora`
2. Wait for installation to complete

**Expected behavior:**
PowerShell should install successfully and be available via `pwsh` command

**Actual behavior:**
Installation fails with error: "Package powershell-fedora not found"

**Additional context:**
This worked on Fedora 39 but fails on Fedora 40. May be related to repository changes.

Labels: bug, powershell, fedora
Milestone: v1.3.0
```

### Model C: Team-First Issue Creation (Legacy)

**When to use**: For sensitive issues, internal planning, or when initial discussion is needed before public visibility.

**Note**: For the Portunix VIM project, prefer Model A (GitHub-First) which provides better structure and traceability.

**Process**:

1. **Internal Team Discussion**
   - Discuss the issue in team channels (Slack, Discord, etc.)
   - Gather initial requirements and scope
   - Determine if issue should be public or remain internal
   - Assign team member initials for tracking

2. **Create GitHub Issue**
   - Once ready for implementation, create public GitHub issue
   - Reference internal discussion: "Based on team discussion from [date]"
   - Include refined requirements and scope
   - Add appropriate labels and assignments

3. **Synchronize Tracking**
   - Link GitHub issue number to internal tracking
   - Name the local file after the GitHub issue number: `docs/issues/042-name.md`
   - Update both systems as work progresses

**Example Team-First Workflow**:

```text
Internal Discussion:
Team Member 1: "We need better error handling for container failures"
Team Member 2: "Yes, and we should add retry logic"
Team Lead: "Let's create GitHub issue #43 for this enhancement"

GitHub Issue Creation:
Title: Improve container failure handling with retry logic

Description:
## Enhancement Request

Based on team analysis, we need to improve how Portunix handles container failures.

**Current behavior:**
- Container failures cause immediate termination
- No retry mechanism for transient failures
- Limited error information provided to user

**Proposed enhancement:**
- Add configurable retry logic (3 attempts by default)
- Implement exponential backoff between retries
- Provide detailed error messages with troubleshooting hints
- Log failure details for debugging

**Acceptance criteria:**
- [ ] Retry mechanism with configurable attempts
- [ ] Exponential backoff implementation
- [ ] Enhanced error messages
- [ ] Unit tests for retry logic
- [ ] Integration tests with container failures

Labels: enhancement, containers, error-handling
Milestone: v1.4.0
Assignee: developer-username
```

## Portunix-Specific Issue Management

### File Structure

```text
docs/
├── issues/
│   ├── README.md              # Process notes only — NO overview table
│   ├── NNN-feature.md         # Active issue write-up (NNN = GitHub issue number)
│   └── done/                  # Archived (✅ Implemented / ❌ Closed / ➡️ Moved) issues
│       ├── README.md          # Process notes only — NO overview table
│       └── NNN-*.md
```

### Quick Reference Commands

```bash
# 1. Create the GitHub issue first — GitHub assigns the number N
gh issue create --repo cassandragargoyle/portunix-vim \
  --title "Short descriptive title" \
  --body "One-line summary. Full details: docs/issues/N-name.md"

# 2. Write the detail file (N = number GitHub just assigned)
#    docs/issues/N-name.md

# 3. Add a working link back into the GitHub issue body
gh issue edit N --body "One-line summary.

📄 https://github.com/cassandragargoyle/portunix-vim/blob/main/docs/issues/N-name.md"
```

No README index or mapping file to update — GitHub is the list of record.

## Issue Numbering and References

### GitHub Issue Numbers

- Use GitHub's automatic numbering (#1, #2, #3, etc.)
- Reference in commits: `git commit -m "Fix container timeout (refs #42)"`
- Close with commits: `git commit -m "Add retry logic (closes #42)"`

### Local Detail Files

- Each issue's long-form write-up is `docs/issues/NNN-name.md`, where
  `NNN` **equals the GitHub issue number** (active) — archived to
  `docs/issues/done/NNN-name.md` on completion.
- All issues from `#1` onward are aligned 1:1 with GitHub. `#1` and `#2` were recreated from
  portunix-plugins and keep the original number in their **Origin** line.

## Issue Templates

### Bug Report Template

```markdown
## Bug Report

**Environment:**
- OS: [e.g., Ubuntu 22.04, Windows 11]
- Project version: [e.g., v1.2.3]
- Installation method: [e.g., package manager, source]

**Steps to reproduce:**
1. Step one
2. Step two
3. Step three

**Expected behavior:**
Clear description of what should happen

**Actual behavior:**
Clear description of what actually happens

**Additional context:**
Any other relevant information, logs, screenshots
```

### Feature Request Template

```markdown
## Feature Request

**Problem description:**
Clear description of the problem this feature would solve

**Proposed solution:**
Detailed description of the proposed feature

**Alternative solutions:**
Any alternative approaches considered

**Additional context:**
Any other relevant information or examples
```

## Labels and Classification

### Standard Labels

- **Type**: `bug`, `enhancement`, `question`, `documentation`
- **Priority**: `low`, `medium`, `high`, `critical`
- **Module**: `module-<name>` — one per vim-parser module, matching the directory
  `java/vim-parser/vim-parser-<name>` (e.g. `module-table`, `module-toc`, `module-lemmatizer`).
  This is how issues are **filtered per module**.
- **Language** (outside vim-parser): `python`, `go`
- **Component** (non-module): `installation`, `testing`, `build-infra`, `core`

> **Not created yet.** The repository currently has only the GitHub default labels
> (`bug`, `enhancement`, `documentation`, ...). The priority, module, language, component,
> status and platform labels above still have to be created.
- **Status**: `needs-triage`, `in-progress`, `blocked`, `ready-for-review`
- **Platform**: `windows`, `linux`, `macos`, `cross-platform`

#### Filtering issues by module

Every vim-parser module has a `module-<name>` label matching its directory
`java/vim-parser/vim-parser-<name>`. Assign it when creating an issue that concerns a module, then
filter with the GitHub UI label dropdown or search:

```text
is:issue is:open label:module-table
```

Infrastructure issues (build tooling, CI, packaging) carry a non-module component label instead.
Create a new `module-<name>` label whenever a new module is added under `java/vim-parser/`:

```bash
gh label create module-<name> --repo cassandragargoyle/portunix-vim \
  --color 1d76db --description "Issues for the vim-parser-<name> module"
```

### Label Usage Guidelines

1. **Always assign type label** (bug, enhancement, etc.)
2. **Add component labels** for easier filtering
3. **Use priority labels** for important issues
4. **Add platform labels** when platform-specific

## Milestones and Planning

### Milestone Creation

- Create milestones for major releases: `v1.3.0`, `v1.4.0`
- Use milestones for sprint planning: `Sprint 2024-Q1`
- Include target dates and release notes

### Issue Assignment

- Assign issues to specific milestones during planning
- Move issues between milestones as priorities change
- Close milestone when all issues are resolved

## Communication Guidelines

### Issue Comments

- **Be constructive** and professional
- **Provide context** for decisions and changes
- **Tag relevant team members** with @mentions
- **Update status** when significant progress is made

### Cross-References

- Reference related issues: "Related to #42"
- Link pull requests: "PR #123 addresses this issue"
- Reference commits: "Fixed in commit abc1234"

## Integration with Development Workflow

### Commit Messages

```bash
# Reference issue
git commit -m "Add logging for container operations (refs #42)"

# Close issue
git commit -m "Fix authentication timeout (closes #42)"

# Multiple issues
git commit -m "Refactor error handling (refs #42, #43, closes #44)"
```

### Pull Request Integration

```markdown
## Pull Request

**Related Issues:** Fixes #42, refs #43

**Description:**
Brief description of changes made

**Testing:**
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

**Checklist:**
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated if needed
```

## Best Practices

### Issue Creation

1. **Use descriptive titles** that summarize the issue
2. **Provide complete context** in the description
3. **Add appropriate labels** and assignments immediately
4. **Include reproduction steps** for bugs
5. **Define acceptance criteria** for features

### Issue Management

1. **Triage new issues** within 24-48 hours
2. **Update issue status** regularly
3. **Close resolved issues** promptly
4. **Archive or label** old/stale issues
5. **Review and update** issue templates periodically

### Team Coordination

1. **Discuss complex issues** in team channels first
2. **Document decisions** in issue comments
3. **Coordinate assignments** to avoid duplication
4. **Review progress** in team meetings
5. **Celebrate completions** and acknowledge contributors

## Tools and Automation

### GitHub Features

- **Issue templates** for consistent reporting
- **Labels and milestones** for organization
- **Projects** for kanban-style tracking
- **Actions** for automated workflows

### Integration Options

- **Slack/Discord bots** for notifications
- **Project management tools** (Jira, Trello, etc.)
- **CI/CD integration** for automatic issue updates
- **Time tracking** tools if needed

---

**Note**: These guidelines should be adapted based on specific project requirements and team preferences. Regular review and updates
ensure the process remains effective and relevant.

*Created: 2025-08-23*
*Last updated: 2026-11-07 - Adapted repository, migrated issues and module labels to the Portunix VIM project*
