---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# vim-parser-fulltext / TextBlockExtractorPlugin

First extractor of the vim-parser pipeline (plugin id `fulltext`, priority 10). It reads the PDF with
PDFBox, builds positioned **text elements** (one label or line fragment each), clusters them into
**text blocks**, runs the text reconstruction (drop caps), classifies the blocks and stores them in the
shared context under `textBlocks` for later plugins (TOC, lemmatizer, table, ...).

## Structure

```mermaid
classDiagram
    class TextBlockExtractorPlugin {
        +extract(PluginContext) PluginResult
        -extractTextElements(PDDocument, int) List~TextElement~
        -clusterIntoBlocks(List~TextElement~, int) List~TextBlock~
        -detectColumns(List~TextElement~) List~List~TextElement~~
        -calculateBaseFontSize(List~TextElement~) float
        -relativeLimit(float, float, float) float
    }
    class TextRun {
        text StringBuilder
        startX, endX float
        baseline float
        height, fontSize float
        fontName String
    }
    class OpenBlock {
        block TextBlock
        lastY, lastHeight float
        lastFontSize float
    }
    TextBlockExtractorPlugin ..> TextRun : builds elements from
    TextBlockExtractorPlugin ..> OpenBlock : builds blocks from
    TextBlockExtractorPlugin ..> TextReconstructionPipeline : drop cap merge
    TextBlockExtractorPlugin ..> PageNumberDetector : page numbers
    TextBlockExtractorPlugin --> TextBlock : produces
    TextBlock o-- TextElement
```

## Main path

```mermaid
sequenceDiagram
    participant E as ExecutionEngine
    participant P as TextBlockExtractorPlugin
    participant S as PDFTextStripper (anonymous)
    participant R as TextReconstructionPipeline
    participant N as PageNumberDetector
    E->>P: extract(context)
    loop each page
        P->>S: getText (sortByPosition)
        S-->>P: TextElements (one per TextRun)
        P->>P: clusterIntoBlocks (columns, open blocks)
    end
    P->>R: process(blocks) - DropCapMerger
    P->>P: analyzeStructure per block
    P->>N: detect(blocks)
    P->>P: default significance
    P-->>E: PluginResult + shared textBlocks, pageCount
```

## Text elements from runs

PDFBox groups glyphs into one line whenever their vertical extents overlap and sorts them by X. On
drawings this mixes labels that sit near each other in height. The stripper therefore keeps several
open **runs** per PDFBox line and assigns each glyph to the run it continues:

```mermaid
flowchart TD
    G["glyph (x, baseline, fontSize)"] --> W{whitespace?}
    W -- yes --> L["append to last used run"]
    W -- no --> F["findRun: open run with"]
    F --> C1{"|baseline - run.baseline| <= 0.5 em"}
    C1 -- no --> N["new run"]
    C1 -- yes --> C2{"-0.5 em <= x - run.endX <= min(50 pt, 3 em)"}
    C2 -- no --> N
    C2 -- yes --> B["best run = smallest gap + baseline difference"]
    B --> A["append glyph, update endX and height"]
    N --> A
    A --> E["line separator / end of page: each run becomes a TextElement"]
```

- The em is the font size of the glyph itself, so a large drop cap does not absorb the body text
  next to it
- Two labels on the same baseline with a gap above `min(50 pt, 3 em)` become two elements
- Two labels on different baselines never share an element, so their letters are not interleaved

## Clustering into blocks

Elements are sorted by Y, then split into columns (`detectColumns`, boundaries voted by at least two
lines). Within a column several blocks stay open at the same time:

```mermaid
flowchart TD
    E["element (sorted by Y)"] --> O["for each open block"]
    O --> V{"vertical gap <= min(15 pt + last height, 3 em)"}
    V -- no --> X["close block (later elements are lower)"]
    V -- yes --> H{"horizontal overlap with block bbox, tolerance min(50 pt, 3 em)"}
    H -- no --> O
    H -- yes --> K["candidate, keep smallest vertical gap"]
    K --> D{"candidate found?"}
    D -- yes --> J["add element to candidate"]
    D -- no --> M["new block (numbered in creation order)"]
```

- Without the horizontal check, labels at the same height in different parts of a large drawing
  sheet joined into one block as wide as the sheet (issue #201)
- The font-relative limits only tighten the absolute ones, so normal body text (about 10 pt) keeps
  the previous thresholds; small drawing labels get proportionally smaller ones
