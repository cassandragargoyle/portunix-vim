# vim-parser

Layered Information Model (VIM) Parser - semantic extraction from documents.

## What is VIM?

**VIM** (Vrstvený Informační Model / Layered Information Model) is a structured representation of document content extracted in semantic layers. Instead of treating a document as flat text, VIM decomposes it into independent layers — text blocks, tables, barcodes, graphics, metadata, table of contents — each extracted by a specialized plugin. The result is a rich JSON model (`vim.json`) that preserves the spatial, structural, and semantic relationships within the original document.

Key concepts:

- **Layered extraction** — each plugin processes one aspect of the document independently
- **Automatic orchestration** — plugins execute in dependency order with zero configuration
- **Provenance tracking** — every extracted element links back to its source location in the document
- **Composable pipeline** — plugins can be added, removed, or replaced without affecting others

## Architecture

### Module Structure

```text
vim-parser/
├── vim-parser-api/          # Plugin interfaces + PF4J ExtensionPoints
├── vim-parser-core/         # Orchestrator, CLI, fat JAR launcher
├── vim-parser-barcode/      # Barcode extraction (ZXing) [plugin]
├── vim-parser-fulltext/     # Text extraction (PDFBox) [plugin]
├── vim-parser-lemmatizer/   # Stemming/lemmatization (Lucene) [plugin]
├── vim-parser-spayd/        # SPAYD payment parser [plugin]
├── vim-parser-visualizer/   # HTML visualizer [plugin]
└── plugins/                 # Runtime plugin directory (ZIP files)
```

### PF4J Plugin System

vim-parser uses [PF4J](https://pf4j.org/) (Plugin Framework for Java) for plugin management:

- **Dynamic loading** - Plugins are loaded at runtime from the `plugins/` directory next to the
  JAR (fallback: `plugins/` in the working directory); `auto --plan` shows the directory used.
  When no plugin is found, `auto` and `extract` exit with code 1 and write no output
- **Lifecycle management** - load, start, stop, unload
- **Versioning** - Each plugin has independent version
- **Hot-reload capable** - Add new plugins without restart

### Distribution Structure

```text
dist/
├── vim-parser-0.1.0.jar     # Fat JAR (core + API + PF4J runtime)
├── plugin.json              # Portunix metadata
└── plugins/
    ├── barcode-plugin-0.1.0.zip
    ├── fulltext-plugin-0.1.0.zip
    ├── lemmatizer-plugin-0.1.0.zip
    ├── spayd-plugin-0.1.0.zip
    └── visualizer-plugin-0.1.0.zip
```

### Build

Run from the repository root (the `Makefile` lives there):

```bash
# Build all modules + plugin ZIPs
make build

# Create distribution package
make dist

# Run tests
make test

# List available plugins
make plugins
```

### Usage with Portunix

```bash
# Automatic extraction using all plugins
portunix vim-parser auto invoice.pdf

# Include document metadata (default: on)
portunix vim-parser auto --metadata invoice.pdf

# Include raw fulltext content (default: off)
portunix vim-parser auto --fulltext invoice.pdf

# Include SHA-256 hash in metadata (default: off)
portunix vim-parser auto --hash invoice.pdf

# Combine options
portunix vim-parser auto --fulltext --hash invoice.pdf

# Enable stemming (for search/NLP)
portunix vim-parser auto --stemming invoice.pdf

# Stemming with specific backend and language
portunix vim-parser auto --stemming --backend=lucene --lang=cs invoice.pdf

# List installed plugins
portunix vim-parser plugins --deps

# Generate HTML visualization
portunix vim-parser visualize output.vim.json
```

`visualize` reads the VIM document strictly: a scalar of the wrong JSON type (for example a number
in a text field, the string `"0.9"` or `"NaN"` in a number field, or `2.5` in an integer field) is
a load error that names the field. Unknown fields are ignored.

### CLI Options (auto command)

| Option | Default | Description |
| ------ | ------- | ----------- |
| `--metadata` / `--no-metadata` | on | Include document metadata (Schema.org) |
| `--fulltext` | off | Include raw fulltext content |
| `--hash` | off | Include SHA-256 hash in metadata |
| `--stemming` | off | Enable stemming/lemmatization |
| `--backend <id>` | lucene | Stemming backend to use |
| `--lang <code>` | auto | Language for stemming (cs, de, en, es, fr, it) |
| `--exclude <plugins>` | - | Comma-separated list of plugins to exclude |
| `--dry-run` | off | Show execution plan without running |
| `-v, --verbose` | off | Verbose output |

---

## Concept

**Universal "Perfect" Format for Storing Information from Any File** - design notes: how to store
the result of file comprehension so that data can be easily extracted and computed.

## 1) The Real Problem (Not PDF)

PDF is just a symptom. The real problem is:

> **Loss of semantics during data transfer between systems**

Typical chain:

```text
DB → reporting tool → PDF → human → manual transcription
```

The customer doesn't want PDF. The customer wants:

- values
- relationships
- meaning (what is the column, unit, currency, period)
- proof of origin (that the data actually comes from the given document)

---

## 2) Fundamental Truth

> **There is no single universal format for all files.**
> **There is a universal inter-layer model for information representation.**

The question is not: "What format stores everything?"
But: **"How to store the result of file comprehension?"**

---

## 3) Ideal Goal: Machine-Readable Reconstruction of Reality

Using a table in PDF as an example:

The customer doesn't need:

- text
- graphics
- layout

The customer needs:

- **entity**: table
- **structure**: columns, rows, cells
- **types**: number, currency, date
- **meaning**: e.g., `net_amount_eur`, `vat_rate`, `period`
- **origin**: where in the document it was (page, bounding box)

---

## 4) Principles of the "Perfect" Format

### 4.1 Separate Content, Structure, and Meaning

- content: `123 456`
- structure: cell → column → table
- meaning: `net_amount`, `currency=CZK`, `vat_included=false`

### 4.2 Be Layered

One layer is never enough (raw → layout → structure → semantics).

### 4.3 Preserve Origin (Provenance)

- from which file
- from which page
- from which region of the document

### 4.4 Be Bidirectional

- from the model, **calculations** can be made
- from the model, the document can be (at least approximately) **reconstructed**

### 4.5 Handle Uncertainty

PDF parsing won't be 100% reliable, therefore:

- confidence score
- extraction method
- alternative interpretations

---

## 5) Architecture Proposal: Layered Information Model (VIM)

The "perfect format" is not one thing, but a **pipeline + data model**.

### Layer 1 – Physical Representation (raw/layout)

Answers: *what is actually there*

```json
{
  "page": 3,
  "bbox": [120, 340, 560, 410],
  "text": "Total excl. VAT"
}
```

### Layer 2 – Structural Model

Answers: *what it looks like*

```json
{
  "type": "table",
  "columns": ["Item", "Quantity", "Price", "Total"],
  "rows": [
    ["A", "2", "100", "200"],
    ["B", "1", "300", "300"]
  ]
}
```

### Layer 3 – Semantic Model

Answers: *what it means*

```json
{
  "entity": "invoice_line",
  "fields": {
    "item_name": "A",
    "quantity": 2,
    "unit_price": { "value": 100, "currency": "CZK" },
    "line_total": { "value": 200, "currency": "CZK", "vat_included": false }
  }
}
```

### Layer 4 – Provenance & Trust

Answers: *why we believe it*

```json
{
  "source": {
    "file": "invoice.pdf",
    "page": 3,
    "bbox": [120, 340, 560, 410]
  },
  "confidence": 0.87,
  "method": "table-detection-v2"
}
```

---

## 6) What Format to Actually Use

| Level      | What it solves | Typical formats        |
| ---------- | -------------- | ---------------------- |
| Raw file   | original       | PDF / DOCX / XLSX      |
| Layout     | text + position| ALTO / PAGE XML        |
| Structure  | tables         | JSON + Schema          |
| Semantics  | meaning        | JSON-LD / RDF          |
| Computation| aggregation    | tabular model          |
| Storage    | queries        | document DB / graph DB |

---

## 7) Ideal Output for Customer

```json
{
  "document_type": "invoice",
  "totals": {
    "net": 123456,
    "vat": 25925,
    "gross": 149381,
    "currency": "CZK"
  }
}
```

```sql
SELECT SUM(net) FROM invoices;
```

---

## 8) Summary

> **The perfect format does not exist as a single file.**
> There is a **layered model + pipeline** that preserves:

- layout
- structure
- semantics
- origin and trust

---

## 9) Future Work

- Document Semantic Model (DSM)
- JSON Schema for layers
- table ontology
- layout → meaning mapping
- handling uncertainty
