---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Revision

Single audit-trail entry describing a value change.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class Revision {
        +Integer revision
        +String property
        +Object oldValue
        +Object newValue
        +RevisionActor actor
        +String actorId
        +RevisionReason reason
        +String comment
        +String timestamp
    }
    Revision --> "0..1" RevisionActor : actor
    Revision --> "0..1" RevisionReason : reason
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `revision` | `revision` | `Integer` | when not null |
| `property` | `property` | `String` | when not null |
| `oldValue` | `old_value` | `Object` | always, `null` included |
| `newValue` | `new_value` | `Object` | always, `null` included |
| `actor` | `actor` | `RevisionActor` | when not null |
| `actorId` | `actor_id` | `String` | when not null |
| `reason` | `reason` | `RevisionReason` | when not null |
| `comment` | `comment` | `String` | when not null |
| `timestamp` | `timestamp` | `String` | when not null |

## Position in the Document

```mermaid
flowchart LR
    DocumentField["DocumentField"] --> Revision["Revision"]
    ExtractionField["ExtractionField"] --> Revision["Revision"]
    TableCell["TableCell"] --> Revision["Revision"]
    Revision["Revision"] --> RevisionActor["RevisionActor"]
    Revision["Revision"] --> RevisionReason["RevisionReason"]
```
