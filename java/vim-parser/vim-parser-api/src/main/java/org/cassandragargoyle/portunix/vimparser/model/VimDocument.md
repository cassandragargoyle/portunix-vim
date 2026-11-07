---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# VimDocument

Root of a VIM (Layered Information Model) document: the format version, the document info, and
the layers. The nested classes hold the physical, structural, and semantic layers. The optional
layers `toc`, `extraction`, and `prescriptive` have their own classes.

The JSON key order is fixed by `@JsonPropertyOrder` on every class. Unknown properties are
ignored on read.

## Structure

```mermaid
classDiagram
    class VimDocument {
        -String vimVersion
        -DocumentInfo document
        -Layers layers
    }
    class DocumentInfo {
        -String source
        -List~DocumentSource~ sources
        -Integer pages
        -DocumentMetadata metadata
    }
    class Layers {
        -PhysicalLayer physical
        -StructuralLayer structural
        -SemanticLayer semantic
        -TableOfContents toc
        -ExtractionLayer extraction
        -PrescriptiveLayer prescriptive
    }
    VimDocument --> "1" DocumentInfo : document
    VimDocument --> "1" Layers : layers
    Layers --> "1" PhysicalLayer : physical
    Layers --> "1" StructuralLayer : structural
    Layers --> "1" SemanticLayer : semantic
    Layers --> "0..1" TableOfContents : toc
    Layers --> "0..1" ExtractionLayer : extraction
    Layers --> "0..1" PrescriptiveLayer : prescriptive
    PhysicalLayer --> "*" PhysicalBlock : blocks
    PhysicalLayer --> "*" PhysicalBarcode : barcodes
    PhysicalLayer --> "*" PhysicalTable : tables
    PhysicalLayer --> "*" GraphicElement : graphics
    PhysicalLayer --> "0..1" FulltextContent : fulltext
    PhysicalTable --> "*" PhysicalTableCell : cells
    StructuralLayer --> "*" StructuralBlock : blocks
    StructuralLayer --> "*" StructuralBarcode : barcodes
    SemanticLayer --> "*" SemanticEntity : entities
    SemanticLayer --> "*" SpaydPayment : payments
    SemanticLayer --> "0..1" StemmedResult : stemming
```

## JSON Mapping

Keys are listed in the order they are written.

| Class | Java field | JSON key | Written |
| ----- | ---------- | -------- | ------- |
| `VimDocument` | `vimVersion` | `vim_version` | always, default `1.5` |
| `VimDocument` | `document` | `document` | always |
| `VimDocument` | `layers` | `layers` | always |
| `Layers` | `physical` | `physical` | always |
| `Layers` | `structural` | `structural` | always |
| `Layers` | `semantic` | `semantic` | always |
| `Layers` | `toc` | `toc` | when not null |
| `Layers` | `extraction` | `extraction` | when not null |
| `Layers` | `prescriptive` | `prescriptive` | when not null |

## Round Trip

`vim-parser` fills the physical, structural, semantic, and `toc` layers. Other producers write
`extraction` (VIM 2.0) and `prescriptive` (VIM 2.1). The model reads and writes them without data
loss, and `vim_version` is kept as read.

```mermaid
flowchart LR
    Json["VIM JSON"] -->|ObjectMapper.readValue| VimDocument
    VimDocument -->|ObjectMapper.writeValueAsString| Out["VIM JSON"]
    VimDocument --> Optional["toc, extraction, prescriptive: omitted when null"]
```
