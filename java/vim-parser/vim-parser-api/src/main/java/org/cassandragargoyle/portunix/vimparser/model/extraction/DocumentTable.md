---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# DocumentTable

Merged table at the document level, assembled from one or more ExtractionTable fragments.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class DocumentTable {
        +String tableId
        +String label
        +List~ColumnDef~ columns
        +List~DocumentTableRow~ rows
        +List~String~ sourceFragments
        +Integer totalRows
        +Double confidence
    }
    DocumentTable --> "*" ColumnDef : columns
    DocumentTable --> "*" DocumentTableRow : rows
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `tableId` | `table_id` | `String` | when not null |
| `label` | `label` | `String` | when not null |
| `columns` | `columns` | `List<ColumnDef>` | always |
| `rows` | `rows` | `List<DocumentTableRow>` | always |
| `sourceFragments` | `source_fragments` | `List<String>` | when not empty |
| `totalRows` | `total_rows` | `Integer` | when not null |
| `confidence` | `confidence` | `Double` | when not null |

## Position in the Document

```mermaid
flowchart LR
    ExtractionDocument["ExtractionDocument"] --> DocumentTable["DocumentTable"]
    DocumentTable["DocumentTable"] --> ColumnDef["ColumnDef"]
    DocumentTable["DocumentTable"] --> DocumentTableRow["DocumentTableRow"]
```
