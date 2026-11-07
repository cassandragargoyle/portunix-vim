---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# TextElement

Single text run extracted from a PDF page, with its position and font.

The JSON key order is fixed by `@JsonPropertyOrder` and equals the field declaration order.

## Structure

```mermaid
classDiagram
    class TextElement {
        -String text
        -BoundingBox bbox
        -int page
        -float fontSize
        -String fontName
        -double confidence
        -boolean dropCapMerged
        -BoundingBox dropCapBbox
        +isLikelyHeader(float baseFontSize) boolean
    }
    TextElement --> "0..1" BoundingBox : bbox
    TextElement --> "0..1" BoundingBox : dropCapBbox
```

## JSON Mapping

Keys are listed in the order they are written.

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `text` | `text` | `String` | always |
| `bbox` | `bbox` | `BoundingBox` | always |
| `page` | `page` | `int` | always |
| `fontSize` | `font_size` | `float` | always |
| `fontName` | `font_name` | `String` | always |
| `confidence` | `confidence` | `double` | always |
| `dropCapMerged` | `drop_cap_merged` | `boolean` | when true |
| `dropCapBbox` | `drop_cap_bbox` | `BoundingBox` | when not null |

## Position in the Document

```mermaid
flowchart LR
    TextBlock --> TextElement
    PhysicalBlock["VimDocument.PhysicalBlock"] --> TextElement
    TextElement --> BoundingBox
```
