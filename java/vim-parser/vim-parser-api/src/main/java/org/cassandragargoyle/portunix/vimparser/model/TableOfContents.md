---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# TableOfContents

Table of contents of a document, derived from heading detection.

The JSON key order is fixed by `@JsonPropertyOrder` and equals the field declaration order.

## Structure

```mermaid
classDiagram
    class TableOfContents {
        -List~TocEntry~ entries
        -Integer tocPage
        +addEntry(TocEntry entry) void
        +isEmpty() boolean
        +size() int
    }
    TableOfContents --> "*" TocEntry : entries
```

## JSON Mapping

Keys are listed in the order they are written.

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `entries` | `entries` | `List<TocEntry>` | always |
| `tocPage` | `toc_page` | `Integer` | when not null |

## Position in the Document

```mermaid
flowchart LR
    Layers["VimDocument.Layers (toc)"] --> TableOfContents
    TableOfContents --> TocEntry
```
