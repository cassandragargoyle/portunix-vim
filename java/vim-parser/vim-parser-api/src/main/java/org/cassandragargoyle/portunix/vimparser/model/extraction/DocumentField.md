---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# DocumentField

Final merged field value at the document level, with references back to contributing sources.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class DocumentField {
        +String fieldId
        +String label
        +Object value
        +String dataType
        +Double confidence
        +CollectMode collectMode
        +List~SourceRef~ sourceRefs
        +List~Revision~ revisions
    }
    DocumentField --> "0..1" CollectMode : collect_mode
    DocumentField --> "*" SourceRef : source_refs
    DocumentField --> "*" Revision : revisions
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `fieldId` | `field_id` | `String` | when not null |
| `label` | `label` | `String` | when not null |
| `value` | `value` | `Object` | always, `null` included |
| `dataType` | `data_type` | `String` | when not null |
| `confidence` | `confidence` | `Double` | when not null |
| `collectMode` | `collect_mode` | `CollectMode` | when not null |
| `sourceRefs` | `source_refs` | `List<SourceRef>` | when not empty |
| `revisions` | `revisions` | `List<Revision>` | when not empty |

## Position in the Document

```mermaid
flowchart LR
    ExtractionDocument["ExtractionDocument"] --> DocumentField["DocumentField"]
    GroupInstance["GroupInstance"] --> DocumentField["DocumentField"]
    DocumentField["DocumentField"] --> CollectMode["CollectMode"]
    DocumentField["DocumentField"] --> SourceRef["SourceRef"]
    DocumentField["DocumentField"] --> Revision["Revision"]
```
