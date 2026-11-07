---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# FieldGroup

Repeating field group, expanded to one or more instances.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class FieldGroup {
        +String groupId
        +String label
        +RepeatMode repeatMode
        +List~GroupInstance~ instances
    }
    FieldGroup --> "0..1" RepeatMode : repeat_mode
    FieldGroup --> "*" GroupInstance : instances
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `groupId` | `group_id` | `String` | when not null |
| `label` | `label` | `String` | when not null |
| `repeatMode` | `repeat_mode` | `RepeatMode` | when not null |
| `instances` | `instances` | `List<GroupInstance>` | always |

## Position in the Document

```mermaid
flowchart LR
    ExtractionDocument["ExtractionDocument"] --> FieldGroup["FieldGroup"]
    FieldGroup["FieldGroup"] --> RepeatMode["RepeatMode"]
    FieldGroup["FieldGroup"] --> GroupInstance["GroupInstance"]
```
