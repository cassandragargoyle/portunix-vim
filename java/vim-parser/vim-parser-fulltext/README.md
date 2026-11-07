# vim-parser-fulltext

Text block extraction plugin for VIM Parser using PDFBox.

## Features

- Extracts text with position information
- Clusters text into logical blocks
- Detects multi-column layouts
- Identifies headers vs content
- Preserves font information

## Algorithm

1. Extract text elements with coordinates using PDFBox; glyphs are grouped into runs per
   baseline, so distant or overlapping labels on one PDFBox line stay separate
2. Detect columns based on horizontal gaps
3. Cluster elements into blocks based on vertical proximity and horizontal overlap
4. Analyze block structure (header detection)

## Output

```json
{
  "id": "block_1",
  "page": 1,
  "type": "labeled_content",
  "header": "Invoice Details",
  "content": ["Date: 2026-01-31", "Invoice #: 12345"],
  "bbox": {"x": 50, "y": 100, "width": 200, "height": 80},
  "confidence": 0.95
}
```

## Block Types

- `labeled_content` - Block with header and content lines
- `content` - Plain content without header
- `table` - Tabular data (future)

## Configuration

- Horizontal gap threshold: 50pt (column detection); within a line and between a block and
  an element at most `min(50pt, 3 em)`
- Vertical gap threshold: 15pt + line height (block separation), at most 3 em
- Baseline tolerance within a run: 0.5 em
- Priority: 10 (runs first)

## Dependencies

- vim-parser-api
- PDFBox
