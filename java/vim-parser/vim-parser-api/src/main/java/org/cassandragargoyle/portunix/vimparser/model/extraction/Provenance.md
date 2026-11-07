---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Provenance

Context describing how a value was produced (engine, parameters, timing).

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class Provenance {
        +ExtractionMethod extractionMethod
        +String engine
        +String engineVersion
        +Map~String,Object~ params
        +String timestamp
        +Long durationMs
        +String pipelineStep
    }
    Provenance --> "0..1" ExtractionMethod : extraction_method
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `extractionMethod` | `extraction_method` | `ExtractionMethod` | when not null |
| `engine` | `engine` | `String` | when not null |
| `engineVersion` | `engine_version` | `String` | when not null |
| `params` | `params` | `Map<String, Object>` | when not empty |
| `timestamp` | `timestamp` | `String` | when not null |
| `durationMs` | `duration_ms` | `Long` | when not null |
| `pipelineStep` | `pipeline_step` | `String` | when not null |

## Position in the Document

```mermaid
flowchart LR
    ExtractionSource["ExtractionSource"] --> Provenance["Provenance"]
    Provenance["Provenance"] --> ExtractionMethod["ExtractionMethod"]
```
