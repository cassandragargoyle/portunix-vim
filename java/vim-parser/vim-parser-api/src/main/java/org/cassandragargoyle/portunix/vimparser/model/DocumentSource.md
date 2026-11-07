---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# DocumentSource

Describes a single input source contributing to a VIM v2.0 document.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class DocumentSource {
        +String file
        +Integer page
        +String type
        +String format
        +Dimensions dimensions
        +Long sizeBytes
        +String sha256
    }
    class Dimensions {
        +Integer width
        +Integer height
    }
    DocumentSource --> "0..1" Dimensions : dimensions
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `file` | `file` | `String` | when not null |
| `page` | `page` | `Integer` | when not null |
| `type` | `type` | `String` | when not null |
| `format` | `format` | `String` | when not null |
| `dimensions` | `dimensions` | `Dimensions` | when not null |
| `sizeBytes` | `size_bytes` | `Long` | when not null |
| `sha256` | `sha256` | `String` | when not null |

## Position in the Document

```mermaid
flowchart LR
    VimDocument_DocumentInfo["VimDocument.DocumentInfo"] --> DocumentSource["DocumentSource"]
    DocumentSource["DocumentSource"] --> Dimensions["DocumentSource.Dimensions"]
```
