---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# ExtractionTable

Table fragment extracted from one source, possibly part of a multi-source group.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class ExtractionTable {
        +String tableId
        +String groupId
        +Integer pageSequence
        +BoundingBox bbox
        +Double confidence
        +List~ColumnDef~ columns
        +List~TableRow~ rows
    }
    ExtractionTable --> "0..1" BoundingBox : bbox
    ExtractionTable --> "*" ColumnDef : columns
    ExtractionTable --> "*" TableRow : rows
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `tableId` | `table_id` | `String` | when not null |
| `groupId` | `group_id` | `String` | when not null |
| `pageSequence` | `page_sequence` | `Integer` | when not null |
| `bbox` | `bbox` | `BoundingBox` | when not null |
| `confidence` | `confidence` | `Double` | when not null |
| `columns` | `columns` | `List<ColumnDef>` | always |
| `rows` | `rows` | `List<TableRow>` | always |

## Position in the Document

```mermaid
flowchart LR
    ExtractionSource["ExtractionSource"] --> ExtractionTable["ExtractionTable"]
    ExtractionTable["ExtractionTable"] --> BoundingBox["BoundingBox"]
    ExtractionTable["ExtractionTable"] --> ColumnDef["ColumnDef"]
    ExtractionTable["ExtractionTable"] --> TableRow["TableRow"]
```
