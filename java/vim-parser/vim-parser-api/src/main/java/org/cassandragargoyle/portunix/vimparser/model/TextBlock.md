---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# TextBlock

Spatially grouped collection of text elements. It is the working model of the layout
analysis, the VIM document stores it converted to `PhysicalBlock` and `StructuralBlock`.

The JSON key order is fixed by `@JsonPropertyOrder` and equals the field declaration order.

## Structure

```mermaid
classDiagram
    class TextBlock {
        -String id
        -BoundingBox bbox
        -int page
        -List~TextElement~ textElements
        -String header
        -List~String~ content
        -String type
        -String significance
        -double confidence
        +addTextElement(TextElement element) void
        +getFullText() String
        +analyzeStructure(float baseFontSize) void
    }
    TextBlock --> "0..1" BoundingBox : bbox
    TextBlock --> "*" TextElement : textElements
```

## JSON Mapping

Keys are listed in the order they are written.

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `id` | `id` | `String` | always |
| `bbox` | `bbox` | `BoundingBox` | always |
| `page` | `page` | `int` | always |
| `textElements` | `text_elements` | `List<TextElement>` | always |
| `header` | `header` | `String` | always |
| `content` | `content` | `List<String>` | always |
| `type` | `type` | `String` | always |
| `significance` | `significance` | `String` | always |
| `confidence` | `confidence` | `double` | always |

## Main Flow

```mermaid
sequenceDiagram
    participant Parser
    participant TextBlock
    participant BoundingBox
    participant VimDocument
    Parser->>TextBlock: addTextElement(element)
    TextBlock->>BoundingBox: expandToInclude(element.bbox)
    Parser->>TextBlock: analyzeStructure(baseFontSize)
    TextBlock-->>Parser: header, content, type, significance
    Parser->>VimDocument: PhysicalBlock.fromTextBlock(block)
    Parser->>VimDocument: StructuralBlock.fromTextBlock(block)
```
