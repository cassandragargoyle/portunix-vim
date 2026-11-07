---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# HeadingDetector

Heuristic heading detection for the `toc` layer. A block is a heading candidate when its header
(set by `TextBlock.analyzeStructure`) is larger than the document base font size by 15 % or is
bold, or when a short block starts with text larger than the base size by 30 %. The heading level
(1–3) comes from the distinct heading font sizes.

Candidates are then filtered as noise (issue #202):

- **Drawing pages**: 10 or more blocks spread over a sheet larger than A3 (longer side above
  1300 pt) and less than 20 % of blocks are running text (8+ words). The heuristic does not run
  on these pages
- **Numbers, dimensions, marks**: a title without a run of 3 letters or 2 CJK characters
  (`1.05`, `A101`, `±0,000`, `A`). Two-letter Latin titles (`AI`, `WC`) are rejected as well
- **Form labels**: a title ending with `:` followed by a short value (1–6 words) in the element
  after the label, in an element to the right on the same row (gap up to 200 pt), or right below
  it aligned to the left edge. Only the first value line is judged, so a heading followed by a
  paragraph or by running text in a second column stays a heading
- **Repeated titles**: the same title at the same position (±10 pt, clustered transitively) on
  3 or more pages, such as page headers and repeated table column labels. Candidates are first
  grouped by normalized title, so clustering is linear for distinct titles

Candidates are sorted by page and by the vertical position of their own source block.

## Structure

```mermaid
classDiagram
    class HeadingDetector {
        +detect(List~TextBlock~) List~TocEntry~
        ~calculateDocumentBaseFontSize(List~TextBlock~) float
        ~determineLevelThresholds(Map, float) List~Float~
        ~evaluateBlock(TextBlock, float, List~Float~) TocEntry
        ~assignLevel(float, List~Float~) int
        ~isBoldFont(String) boolean
        ~isMarkOrNumber(String) boolean
        ~isFormLabel(String, TextBlock, List~TextBlock~) boolean
        ~removeRepeatedTitles(List~Candidate~) List~TocEntry~
        ~detectDrawingPages(List~TextBlock~) Set~Integer~
        -isFormValue(String) boolean
    }
    class Candidate {
        <<record>>
        TocEntry entry
        TextBlock block
    }
    TocExtractorPlugin --> HeadingDetector
    HeadingDetector ..> Candidate
    HeadingDetector ..> TextBlock
    HeadingDetector ..> TocEntry
```

## Flow

```mermaid
flowchart TD
    A[text blocks] --> B[base font size and level thresholds]
    B --> C[detect drawing pages]
    C --> D{block on drawing page?}
    D -->|yes| Z[skip]
    D -->|no| E{evaluateBlock: large or bold header?}
    E -->|no| Z
    E -->|yes| F{no run of 3 letters?}
    F -->|yes| Z
    F -->|no| G{ends with colon and short value after, right or below?}
    G -->|yes| Z
    G -->|no| H[candidate]
    H --> S[sort by page and block position]
    S --> I{same title and position on 3+ pages?}
    I -->|yes| Z
    I -->|no| J[TocEntry]
```
