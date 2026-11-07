---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Verification

How a provision can be checked. `rule` is an open object: no rule language is defined, the
content is an agreement between the producer and the checking tool.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-prescriptive*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class Verification {
        +VerificationMethod method
        +String subject
        +Map~String, Object~ rule
    }
    Verification --> "0..1" VerificationMethod : method
    class VerificationMethod {
        <<enumeration>>
        AUTOMATIC
        TEST
        MANUAL
    }
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `method` | `method` | `VerificationMethod` | when not null |
| `subject` | `subject` | `String` | when not null |
| `rule` | `rule` | `Map<String, Object>` | when not null |

## Enum Values

Enums are written as lower snake case and read without regard to case.

| Enum | JSON values |
| ---- | ----------- |
| `VerificationMethod` | `automatic`, `test`, `manual` |

## Position in the Document

```mermaid
flowchart LR
    Provision["Provision"] --> Verification["Verification"]
```
