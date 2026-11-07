---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# SourceRef

Reference from a merged field/row back to its origin in sources[].

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class SourceRef {
        +String sourceId
        +String fieldId
    }
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `sourceId` | `source_id` | `String` | when not null |
| `fieldId` | `field_id` | `String` | when not null |

## Position in the Document

```mermaid
flowchart LR
    DocumentField["DocumentField"] --> SourceRef["SourceRef"]
```
