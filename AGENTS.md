# Portunix VIM Development Instructions

Instructions for AI coding agents (Claude Code, Codex, Copilot, ...) and contributors.
`CLAUDE.md` only imports this file. Contribution guidelines live in `docs/contributing/`.

## Project Information

- **Repository**: <https://github.com/cassandragargoyle/portunix-vim/> (GitHub — single source of
  truth for development and publication)
- **Project Name**: portunix-vim
- **Purpose**: VIM (Layered Information Model) tooling for the Portunix ecosystem
- **Primary Languages**: Java (`java/`), Python (`python/`), Go (`go/`)
- **Platforms**: Windows, Linux (cross-platform)

## Repository Layout

- `java/vim-parser/` — VIM parser Portunix plugin (multi-module Maven project, PF4J modules)
- `python/` — Python VIM tools (uv-managed projects)
- `go/` — Go VIM CLI and helper tools
- `docs/adr/` — Architecture Decision Records
- `docs/issues/` — long-form issue write-ups
- `docs/contributing/` — contribution guides
- `docs/book/` — PDF books from Markdown chapters (EN and CS); `docs/book/vim/` is the canonical,
  most complete description of VIM in the ecosystem (see [docs/book/README.md](docs/book/README.md),
  skill `book-chapter`, `make book`)

## Coding Guidelines & Development Instructions

### Strict Policies

- Code, code comments, and user messages must be in English
- If implementation of a request could lead to loss of implemented code (e.g., when manipulating
  git), highlight this prominently and always require confirmation
- Do not put "Generated with [Claude Code](https://claude.ai/code)" or any AI attribution into
  code or other files
- **Never add a `Co-Authored-By:` trailer** (Claude or any other AI agent) or similar attribution
  lines to git commit messages or pull request descriptions

### General Policies

- Follow existing conventions and styles in the project
- Always explore existing code first before creating new code
- Prefer editing existing files over creating new ones
- All texts in programs (messages, labels, constants) are in English
- Comments are in English and are not sentences (no period at the end)
- **Windows scripts**: Prefer PowerShell (.ps1) over batch files (.bat)

### Markdown

- Before writing or generating any `.md` file, read and follow
  [docs/contributing/MARKDOWN-STYLE.md](docs/contributing/MARKDOWN-STYLE.md)

### Portunix Integration

- **Always use** `portunix system info` for OS detection when Portunix is available
- **Always use** `portunix container` commands for container operations; never call `docker` or
  `podman` directly in code, scripts, or tests

### CLI Conventions

- **GNU/POSIX style**: Options BEFORE positional arguments
- Use short (`-o`) and long (`--output`) option variants
- Boolean flags don't require values: `--verbose`, `--dry-run`
- All CLI commands must support `--help` and `--version` flags
- Java CLIs use Picocli

### Java

Build and test from the repository root (wraps `mvn -f java/vim-parser/pom.xml clean verify`):

```bash
make verify
```

`vim-parser-table` consumes `table-detector-api` and `table-detector-pdf` from
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) as versioned artifacts
(`table-detector.version` in `java/vim-parser/pom.xml`). Install them locally with
`mvn -f <portunix-plugins>/plugins/table-detector/pom.xml install -DskipTests`.

Distribution package for Portunix: `make dist` in the repository root (output in `dist/`).
Run `make help` for all targets.

### Python

- **Use `uv`** for all Python work (`pyproject.toml` + checked-in `uv.lock`)
- `uv sync` installs dependencies, `uv run <cmd>` runs tools and scripts
- Add dependencies with `uv add <pkg>` (`uv add --dev <pkg>` for dev tools)
- Do not activate `.venv` manually or call `pip install`

### Go

Compile with `go build -o .`

### Issue Tracking & Documentation

- **GitHub is the single source of truth** for the issue list (status, assignee, labels)
- Long-form write-ups live in `docs/issues/NNN-name.md`, where `NNN` = GitHub issue number
- Archived (done) write-ups live in `docs/issues/done/`
- Before creating new features, check existing issues on GitHub (`gh issue list`) and in
  `docs/issues/`
- Workflow: create the GitHub issue first, then `docs/issues/NNN-name.md`; on a terminal status
  (✅ Implemented, ❌ Closed, ➡️ Moved) update the status, `git mv` the file to `docs/issues/done/`
  and close the GitHub issue

### Architecture Decision Records (ADR)

- Before creating a new ADR, always read `docs/adr/README.md` first
- Follow the template and naming conventions defined in that file
- Update the ADR list table after creating a new ADR

### GitHub Workflow

Feature branch → Pull Request → merge into `main`:

```bash
git switch -c feature/my-change
git commit -m "feat(NNN): short description"
git push -u origin feature/my-change
```

**Remote:** `origin -> github.com/cassandragargoyle/portunix-vim`

**Never commit secrets.** The following stay local-only (gitignored): `NOTES.md`,
`.claude/settings.local.json`, `CLAUDE.local.md`, `.env*`.

See [docs/contributing/GITHUB-WORKFLOW.md](docs/contributing/GITHUB-WORKFLOW.md).
