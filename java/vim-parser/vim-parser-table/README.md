# vim-parser-table

Table extraction plugin for VIM Parser. Detects tabular data in PDF documents and extracts structured cell content using PDFBox.

## Features

- Detects tables in PDF documents page-by-page
- Extracts rows, columns, and individual cells
- Reports cell positions (bounding boxes) and text content
- Page-level scoping (single page or whole document)
- Thin adapter — delegates detection to the shared `table-detector-pdf` library

## Architecture

The plugin is a thin adapter over the standalone `table-detector` libraries:

```text
┌──────────────────────────────────────┐
│    TableExtractorPlugin (adapter)    │
└──────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────┐
│  table-detector-pdf                  │
│    └─ PdfTableDetector               │
└──────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────┐
│  table-detector-api (TableModel)     │
└──────────────────────────────────────┘
```

This separation lets the detection algorithm evolve independently from the VIM plugin contract.

## Algorithm

1. Load the PDF via PDFBox
2. Determine page range from `PluginContext` (single page or all pages)
3. For each page, run `PdfTableDetector.detectTables(document, pageNum)`
4. Assign monotonically increasing IDs (`table-1`, `table-2`, …) and the source page number
5. Store the resulting list under shared key `tables` for downstream plugins
6. Return aggregated metadata (table count, cell count, processing time)

## Output

```json
{
  "id": "table-1",
  "page": 3,
  "rows": 4,
  "cols": 3,
  "bbox": {"x": 50, "y": 200, "width": 500, "height": 180},
  "cells": [
    {
      "row": 0,
      "col": 0,
      "text": "Item",
      "bbox": {"x": 50, "y": 200, "width": 160, "height": 25}
    },
    {
      "row": 0,
      "col": 1,
      "text": "Quantity",
      "bbox": {"x": 210, "y": 200, "width": 160, "height": 25}
    }
  ]
}
```

## Shared context keys

- `tables` — list of `TableModel` objects detected across the processed pages

## Configuration

- Priority: 30 (runs after barcode extraction)
- Phase: `EXTRACT`
- Dependencies: none
- Predecessors: none
- Auto-enabled: yes
- Execution modes: SYNC, ASYNC_THREAD

## Dependencies

- vim-parser-api
- table-detector-api
- table-detector-pdf
- PDFBox
- Jackson (JSON)
- org-cassandragargoyle-api (logging)
