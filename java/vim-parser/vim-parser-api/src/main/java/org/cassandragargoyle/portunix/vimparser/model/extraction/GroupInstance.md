---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# GroupInstance

Single instance of a repeating group (e.g., one line item, one page).

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class GroupInstance {
        +Integer instanceIndex
        +String sourceId
        +List~DocumentField~ fields
    }
    GroupInstance --> "*" DocumentField : fields
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `instanceIndex` | `instance_index` | `Integer` | when not null |
| `sourceId` | `source_id` | `String` | when not null |
| `fields` | `fields` | `List<DocumentField>` | always |

## Position in the Document

```mermaid
flowchart LR
    FieldGroup["FieldGroup"] --> GroupInstance["GroupInstance"]
    GroupInstance["GroupInstance"] --> DocumentField["DocumentField"]
```
