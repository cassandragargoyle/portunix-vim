# ADR-001: vim-parser Core Architecture

**Status**: Accepted
**Date**: 2026-01-30
**Origin**: ADR-011 in [portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins/blob/main/docs/adr/011-vim-parser-core-architecture.md)
**Author**: Architecture review

## Context

vim-parser is a new plugin for semantic extraction from documents. We need to establish fundamental architecture decisions before implementation.

## Decisions

### 1. VIM Format Version

**Decision**: Include `vim_version: "1.0"` in output JSON.

**Rationale**: Enables future parsers to detect version and migrate data if format changes.

### 2. Dependency on text-extractor

**Decision**: vim-parser calls text-extractor internally (compile-time Maven dependency).

**Rationale**:

- User calls only vim-parser, simpler UX
- Both plugins are Java, natural integration
- text-extractor already handles raw text extraction

```xml
<dependency>
    <groupId>org.cassandragargoyle.portunix</groupId>
    <artifactId>text-extractor</artifactId>
    <version>1.0.4</version>
</dependency>
```

### 3. Output File Naming

**Decision**: Output files use `.vim.json` suffix appended to original filename.

**Example**: `sample1.pdf` → `sample1.pdf.vim.json`

**Rationale**: Clear association with source file, no collision with other outputs.

### 4. CLI Interface

**Decision**: `portunix vim-parser parse <file>`

**Rationale**: Consistent with other Portunix plugins command structure.

### 5. Storage Strategy

**Decision**: File-based storage initially, DB-ready JSON format.

**Rationale**: Start simple, JSON format is compatible with MongoDB, PostgreSQL JSONB, Elasticsearch for future integration.

### 6. Dependency on fulltext

**Decision**: vim-parser uses fulltext `describe` command for Schema.org metadata (compile-time Maven dependency).

**Rationale**:

- Schema.org JSON-LD already implemented in fulltext plugin
- Provides standardized document metadata (name, size, hash, dates)
- Reuse existing code, no duplication

```xml
<dependency>
    <groupId>org.cassandragargoyle.portunix</groupId>
    <artifactId>fulltext-plugin</artifactId>
    <version>1.1.0</version>
</dependency>
```

### 7. Fragment-based Data Model

**Decision**: Use normalized data fragments with metadata instead of duplicated text.

**Rationale**:

- No data duplication (text stored once per fragment)
- Fragments can be referenced from multiple structures (pages, tables, entities)
- Metadata enables provenance tracking (source, page, confidence)
- Extensible for future layers (bbox, OCR confidence)

**Fragment structure**:

```json
{
  "fragments": {
    "df01": {
      "text": "content...",
      "page": 1,
      "source": "text-extractor",
      "type": "page_content"
    }
  }
}
```

**Fragment metadata fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `text` | string | yes | Fragment content |
| `page` | number | yes | Page number (1-based) |
| `source` | string | yes | Extractor name |
| `type` | string | yes | Content type: `page_content`, `table`, `header`, `footer` |
| `bbox` | array | no | Future: `[x, y, width, height]` |
| `confidence` | number | no | Future: 0.0-1.0 for OCR |

### 8. VIM JSON Structure

**Decision**: Standardized output format combining all layers.

```json
{
  "vim_version": "1.0",
  "schema_org": {
    "@context": "https://schema.org",
    "@type": "DigitalDocument",
    "name": "sample1.pdf",
    "encodingFormat": "application/pdf",
    "contentSize": "292KB",
    "sha256": "..."
  },
  "fragments": {
    "df01": { "text": "...", "page": 1, "source": "text-extractor", "type": "page_content" },
    "df02": { "text": "...", "page": 2, "source": "text-extractor", "type": "page_content" }
  },
  "pages": [
    { "page": 1, "content": ["df01"] },
    { "page": 2, "content": ["df02"] }
  ],
  "full_text": ["df01", "df02"]
}
```

**Resolution**: To get full text, resolve references: `full_text.map(id => fragments[id].text).join("\n")`

## Consequences

- vim-parser cannot be compiled without text-extractor and fulltext
- Single command extracts and transforms document
- Output format is forward-compatible with databases
- Version field enables future schema migrations
- Fragment model enables future semantic layers without data duplication
- Consumers must implement fragment resolution for full text access
