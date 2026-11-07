# Architecture Decision Records (ADR)

## Overview

This directory contains Architecture Decision Records for the Portunix VIM project.

An Architecture Decision Record (ADR) captures an important architectural decision made along with
its context and consequences.

## ADR List

| ADR                                                  | Title                                                    | Status   | Date       |
| ---------------------------------------------------- | -------------------------------------------------------- | -------- | ---------- |
| [ADR-001](001-vim-parser-core-architecture.md)       | vim-parser Core Architecture                             | Accepted | 2026-01-30 |
| [ADR-002](002-vim-parser-modular-architecture.md)    | Modular Architecture for VIM Parser                      | Proposed | 2026-01-31 |
| [ADR-003](003-vim-parser-pf4j-portunix-plugin.md)    | VIM Parser - PF4J Plugin System and Portunix Integration | Proposed | 2026-01-31 |

ADR-001 to ADR-003 were moved from portunix-plugins (originally ADR-011 to ADR-013); each keeps an
**Origin** line pointing to the original.

## Status Legend

- **Proposed** - Decision is under consideration
- **Accepted** - Decision has been accepted and should be followed
- **Deprecated** - Decision is no longer valid
- **Superseded** - Decision has been replaced by a newer ADR

## ADR Template

```markdown
# ADR-XXX: Title

**Status**: Proposed | Accepted | Deprecated | Superseded
**Date**: YYYY-MM-DD
**Deciders**: Names
**Related**: Issue #XXX, ADR-XXX

## Context

[Describe the context and problem statement]

## Decision

[Describe the decision and its rationale]

## Consequences

### Positive

[List positive consequences]

### Negative

[List negative consequences]

### Risks

[List risks and mitigations]
```

## Creating a New ADR

1. Copy the template above
1. Use the next sequential number (currently: 004)
1. Name the file `NNN-short-kebab-title.md`
1. Add the ADR to the list table above
