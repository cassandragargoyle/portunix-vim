---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Validation

Result of validating an extracted field.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class Validation {
        +ValidationStatus status
        +List~String~ rulesApplied
        +List~String~ messages
    }
    Validation --> "0..1" ValidationStatus : status
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `status` | `status` | `ValidationStatus` | when not null |
| `rulesApplied` | `rules_applied` | `List<String>` | when not empty |
| `messages` | `messages` | `List<String>` | when not empty |

## Position in the Document

```mermaid
flowchart LR
    ExtractionField["ExtractionField"] --> Validation["Validation"]
    Validation["Validation"] --> ValidationStatus["ValidationStatus"]
```
