---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# TableRowRef

Reference from a merged table row back to its originating row in a source table fragment.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class TableRowRef {
        +String sourceId
        +String tableId
        +Integer rowIndex
    }
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `sourceId` | `source_id` | `String` | when not null |
| `tableId` | `table_id` | `String` | when not null |
| `rowIndex` | `row_index` | `Integer` | when not null |

## Position in the Document

```mermaid
flowchart LR
    DocumentTableRow["DocumentTableRow"] --> TableRowRef["TableRowRef"]
```
