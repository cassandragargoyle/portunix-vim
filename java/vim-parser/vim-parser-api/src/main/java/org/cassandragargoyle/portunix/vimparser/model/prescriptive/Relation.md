---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Relation

A link from a provision to another provision of the layer.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-prescriptive*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class Relation {
        +String provisionId
        +RelationType relation
    }
    Relation --> "0..1" RelationType : relation
    class RelationType {
        <<enumeration>>
        REFINES
        DEPENDS_ON
        EXCEPTION_TO
        CONFLICTS_WITH
    }
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `provisionId` | `provision_id` | `String` | when not null |
| `relation` | `relation` | `RelationType` | when not null |

## Enum Values

Enums are written as lower snake case and read without regard to case.

| Enum | JSON values |
| ---- | ----------- |
| `RelationType` | `refines`, `depends_on`, `exception_to`, `conflicts_with` |

## Position in the Document

```mermaid
flowchart LR
    Provision["Provision"] --> Relation["Relation"]
```
