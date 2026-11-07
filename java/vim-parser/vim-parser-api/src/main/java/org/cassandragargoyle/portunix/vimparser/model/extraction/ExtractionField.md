---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# ExtractionField

Single extracted field value from a source, with confidence, alternatives, validation, and audit
trail.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class ExtractionField {
        +String fieldId
        +String label
        +Object value
        +Object rawValue
        +String dataType
        +Double confidence
        +BoundingBox bbox
        +String zoneId
        +String extractionMethod
        +List~Alternative~ alternatives
        +Validation validation
        +List~Revision~ revisions
    }
    ExtractionField --> "0..1" BoundingBox : bbox
    ExtractionField --> "*" Alternative : alternatives
    ExtractionField --> "0..1" Validation : validation
    ExtractionField --> "*" Revision : revisions
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `fieldId` | `field_id` | `String` | when not null |
| `label` | `label` | `String` | when not null |
| `value` | `value` | `Object` | always, `null` included |
| `rawValue` | `raw_value` | `Object` | when not null |
| `dataType` | `data_type` | `String` | when not null |
| `confidence` | `confidence` | `Double` | when not null |
| `bbox` | `bbox` | `BoundingBox` | when not null |
| `zoneId` | `zone_id` | `String` | when not null |
| `extractionMethod` | `extraction_method` | `String` | when not null |
| `alternatives` | `alternatives` | `List<Alternative>` | when not empty |
| `validation` | `validation` | `Validation` | when not null |
| `revisions` | `revisions` | `List<Revision>` | when not empty |

## Numeric Precision

`value` and `raw_value` are untyped. A default Jackson mapper reads a JSON number with a
fraction as `double`, so more than 17 significant digits are lost and an overflow is written
back as the string `"Infinity"`. A reader that needs exact decimals enables
`DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS` on its mapper.

## Position in the Document

```mermaid
flowchart LR
    ExtractionSource["ExtractionSource"] --> ExtractionField["ExtractionField"]
    ExtractionField["ExtractionField"] --> BoundingBox["BoundingBox"]
    ExtractionField["ExtractionField"] --> Alternative["Alternative"]
    ExtractionField["ExtractionField"] --> Validation["Validation"]
    ExtractionField["ExtractionField"] --> Revision["Revision"]
```
