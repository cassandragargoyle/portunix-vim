---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Alternative

Alternative interpretation of an extracted value (e.g., OCR candidate).

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class Alternative {
        +Object value
        +Double confidence
    }
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `value` | `value` | `Object` | always, `null` included |
| `confidence` | `confidence` | `Double` | when not null |

## Position in the Document

```mermaid
flowchart LR
    ExtractionField["ExtractionField"] --> Alternative["Alternative"]
```
