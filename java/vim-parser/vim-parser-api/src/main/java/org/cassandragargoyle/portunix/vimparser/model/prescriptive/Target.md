---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Target

Target of a reference. An absent `edition` means any edition.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-prescriptive*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class Target {
        +String identifier
        +String edition
        +String title
        +String clause
        +String uri
    }
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `identifier` | `identifier` | `String` | when not null |
| `edition` | `edition` | `String` | when not null |
| `title` | `title` | `String` | when not null |
| `clause` | `clause` | `String` | when not null |
| `uri` | `uri` | `String` | when not null |

## Position in the Document

```mermaid
flowchart LR
    Reference["Reference"] --> Target["Target"]
```
