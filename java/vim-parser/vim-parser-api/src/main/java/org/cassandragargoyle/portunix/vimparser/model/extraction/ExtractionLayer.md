---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# ExtractionLayer

Top-level extraction layer for VIM v2.0 — template-driven results produced by reco, OCR engines, or
other tools.

The JSON keys are snake_case and match the contract schemas
(`vim-document.schema.json`, `vim-extraction*.schema.json`). The Java field names stay camelCase.

## Structure

```mermaid
classDiagram
    class ExtractionLayer {
        +TemplateRef templateRef
        +List~ExtractionSource~ sources
        +ExtractionDocument document
    }
    ExtractionLayer --> "0..1" TemplateRef : template_ref
    ExtractionLayer --> "*" ExtractionSource : sources
    ExtractionLayer --> "0..1" ExtractionDocument : document
```

## JSON Mapping

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `templateRef` | `template_ref` | `TemplateRef` | when not null |
| `sources` | `sources` | `List<ExtractionSource>` | always |
| `document` | `document` | `ExtractionDocument` | when not null |

## Position in the Document

```mermaid
flowchart LR
    VimDocument_Layers["VimDocument.Layers"] --> ExtractionLayer["ExtractionLayer"]
    ExtractionLayer["ExtractionLayer"] --> TemplateRef["TemplateRef"]
    ExtractionLayer["ExtractionLayer"] --> ExtractionSource["ExtractionSource"]
    ExtractionLayer["ExtractionLayer"] --> ExtractionDocument["ExtractionDocument"]
```
