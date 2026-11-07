# Terminology

Glossary of terms used in the vim-parser project. The goal is to unify the meaning of expressions across code, documentation, and user-facing communication.

## Core concepts

### VIM (Layered Information Model)

Structured representation of document content decomposed into semantic layers (Czech: Vrstvený Informační Model).

### vim.json

Output JSON file containing the complete VIM model of a document.

### Layer

Independent level of document representation (physical / structural / semantic / provenance).

### Pipeline

Sequence of plugins executed in dependency order.

### File comprehension

Process of converting a raw file into a semantically rich model.

## Plugin architecture

### Plugin

Independent module extending vim-parser functionality (extractor, parser, visualizer).

### Extractor

Plugin that extracts raw data from documents (text, barcodes, tables, graphics).

### Parser

Plugin transforming extracted data into semantic entities (e.g. SPAYD payment).

### Visualizer

Plugin generating output representations of the VIM model (HTML, JSON).

### PF4J

Plugin Framework for Java — runtime for dynamic plugin loading.

### ExtensionPoint

Extension point defined in the API that plugins implement.

### ServiceLoader

Standard Java mechanism for discovering interface implementations.

### Fat JAR (Uber JAR)

JAR containing all dependencies and the PF4J runtime.

### Hot-reload

Ability to add a plugin at runtime without restarting the application.

### Orchestrator

Component controlling plugin execution order based on dependencies.

### Lifecycle

Sequence of plugin states: load → start → stop → unload.

## Data model

### Entity

Semantic unit in a document (e.g. invoice, invoice line, address).

### TextBlock

Contiguous span of text with a position on the page.

### Bounding box (bbox)

Rectangular region in the document given by coordinates `[x1, y1, x2, y2]`.

### Page

Individual page of a document (1-based numbering).

### Metadata

Structured information about the document (author, date, language, Schema.org).

### Fulltext

Raw document content as plain text.

### Barcode

QR Code, Data Matrix, PDF417, Aztec.

### SPAYD

Short Payment Descriptor — Czech payment QR code format (`SPD*…`).

### TOC (Table of Contents)

Document outline — heading hierarchy.

## Provenance and trust

### Provenance

Tracking the source of an extracted element (file, page, bbox).

### Confidence

Numeric score `[0,1]` expressing extraction certainty.

### Extraction method

Identifier of the algorithm/version that produced the data.

### Alternative interpretation

Competing interpretation of the same input under uncertainty.

## Text processing

### Stemming

Reducing a word to its stem (algorithmic, lossy).

### Lemmatization

Converting a word to its base form (lemma) using a dictionary/morphology.

### Backend

Implementation of stemming/lemmatization (Lucene, MorphoDiTa, TK).

### Lemma

Base (dictionary) form of a word.

### Token

Unit of text after tokenization (word, number, punctuation).

## VIM layers

### Layer 1 — Physical (raw)

What is physically present in the file (characters, positions).

### Layer 2 — Structural

How content is organized (tables, columns, cells).

### Layer 3 — Semantic

What the content means (entity, fields, units).

### Layer 4 — Provenance & Trust

Where data comes from and how much we trust it.

## CLI and runtime

### Auto mode

Automatic execution of all available plugins.

### Dry run

Show the execution plan without actually running.

### Plugin exclusion

The `--exclude` option for skipping specific plugins.

### Verbose

Detailed output intended for diagnostics.

### Distribution

Built `dist/` package ready for deployment.

## Related standards

### Schema.org

Vocabulary of structured data describing entities (Document, Person, Organization, …).

### JSON-LD

JSON for Linking Data — semantic format extending JSON with context.

### RDF

Resource Description Framework — model for describing relationships between entities.

### ALTO XML

Analyzed Layout and Text Object — XML format describing the layout of scanned pages.

### PAGE XML

Page Analysis and Ground-truth Elements — XML for document layout and ground-truth.

### DSM (Document Semantic Model)

Semantic model of a document — future work.

## Acronyms

### VIM

Layered Information Model (Czech: Vrstvený Informační Model).

### PF4J

Plugin Framework for Java — plugin runtime.

### TOC

Table of Contents — document outline.

### SPAYD

Short Payment Descriptor — Czech payment QR code format.

### DPI

Dots Per Inch — page rendering resolution for barcode scanning.

### API

Application Programming Interface — programmatic interface (`vim-parser-api` module).

### CLI

Command-Line Interface — command line.

### DSM

Document Semantic Model — semantic model of a document.

## Naming conventions

- **English** — code, identifiers, JSON keys, commit messages, technical documentation in `README.md`
- **Czech** — user documentation `*.cs.md`, conceptual texts, terminology
- **Plugin ID** — short name without the `vim-parser-` prefix (e.g. `barcode`, `fulltext`, `spayd`)
- **Maven module** — name with the `vim-parser-` prefix (e.g. `vim-parser-barcode`)
