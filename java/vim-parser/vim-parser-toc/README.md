# vim-parser-toc

Table of Contents extraction plugin for VIM Parser. Detects chapters/headings and maps them to page numbers, producing a navigable document outline.

## Features

- Explicit TOC page detection (printed table-of-contents pages, e.g. "Obsah", "Contents")
- Heuristic heading detection based on font size, weight, and position
- Merging of explicit and heuristic results with confidence boosting
- Marks TOC source blocks with `type=toc` for downstream consumers
- Hierarchical entries (level + page + title)

## Algorithm

1. Read `textBlocks` from shared context (produced by `fulltext` plugin)
2. **Phase 1 — Explicit TOC**: scan blocks for a printed TOC page; extract `title → page` entries
3. **Phase 2 — Heuristic headings**: detect candidate headings via font/position heuristics
   - Pages that look like drawings (many blocks over a sheet larger than A3, little running text) are skipped
   - Rejected as noise: numbers, dimensions, marks and single characters (no run of 3 letters),
     form labels (`Label:` followed by a short value next to or below it), and titles repeated
     at the same position on 3+ pages
4. **Phase 3 — Merge**:
   - Explicit entries take priority
   - Matching heuristic entries boost the confidence of the explicit entry and contribute their `block_id`
   - Non-overlapping heuristic entries are kept as supplementary
   - Final list is sorted by page number

## Output

```json
{
  "toc": {
    "tocPage": 3,
    "entries": [
      {
        "title": "Introduction",
        "page": 5,
        "level": 1,
        "blockId": "block_42",
        "confidence": 0.95
      },
      {
        "title": "Architecture",
        "page": 12,
        "level": 1,
        "blockId": "block_88",
        "confidence": 0.90
      }
    ]
  }
}
```

## Shared context keys

- `toc` — the resulting `TableOfContents` object
- `tocBlockIds` — IDs of text blocks belonging to the printed TOC page (for structural-layer annotation)

## Configuration

- Priority: 50 (runs after fulltext (10) and barcode (20))
- Phase: `TRANSFORM`
- Dependencies: `fulltext`
- Predecessors: `fulltext`
- Auto-enabled: yes
- Execution modes: SYNC, ASYNC_THREAD

## Dependencies

- vim-parser-api
- org-cassandragargoyle-api (logging)
