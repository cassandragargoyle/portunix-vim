---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# BoundingBox

Rectangle in the PDF coordinate system, used as the `bbox` value across the VIM document.

The JSON key order is fixed by `@JsonPropertyOrder`, so every writer of a VIM document
(Java, Python in `portunix-reco`) produces the same order. `right` and `bottom` are computed
from the stored fields and are ignored on read (`ignoreUnknown`).

## Structure

```mermaid
classDiagram
    class BoundingBox {
        -float x
        -float y
        -float width
        -float height
        +getRight() float
        +getBottom() float
        +horizontallyOverlaps(BoundingBox other) boolean
        +isInSameColumn(BoundingBox other, float tolerance) boolean
        +expandToInclude(BoundingBox other) void
    }
```

## JSON Mapping

Keys are listed in the order they are written.

| JSON key | Source | Type | Written |
| -------- | ------ | ---- | ------- |
| `right` | `getRight()` | `float` | always, computed as `x + width` |
| `bottom` | `getBottom()` | `float` | always, computed as `y + height` |
| `x` | `x` | `float` | always |
| `y` | `y` | `float` | always |
| `width` | `width` | `float` | always |
| `height` | `height` | `float` | always |

## Serialization Flow

```mermaid
flowchart LR
    Fields["x, y, width, height"] --> Getters["getRight(), getBottom()"]
    Fields --> Order["@JsonPropertyOrder"]
    Getters --> Order
    Order --> Json["right, bottom, x, y, width, height"]
```

## Position in the Document

```mermaid
flowchart LR
    TextElement --> BoundingBox
    TextBlock --> BoundingBox
    GraphicElement --> BoundingBox
    ExtractionField --> BoundingBox
    ExtractionTable --> BoundingBox
    VimDocument_Blocks["VimDocument blocks"] --> BoundingBox
```
