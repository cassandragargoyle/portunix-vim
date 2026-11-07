# VIM JSON Schema Documentation

VIM (Layered Information Model) is a JSON format for representing extracted document structure with multiple semantic layers.

## Version

Latest version of the layers that `vim-parser` fills: `1.5`

`vim-parser` writes the version that matches the layers it filled in the run, `1.4` at most
today. `1.5` is the default of a new `VimDocument` object.

Versions `2.0` and `2.1` add layers that other producers write. The model in `vim-parser-api`
reads and writes such documents without data loss and keeps their `vim_version`.

**Changelog:**

- `2.1` - Added optional `prescriptive` layer (provisions, references, and terms of a definition file)
- `2.0` - Added optional `extraction` layer (template-driven extraction results)
- `1.5` - Added block significance classification (`primary`/`auxiliary`) and page number detection
- `1.4` - Added `toc` layer for chapter-to-page mapping (TOC enrichment)
- `1.3` - Replaced `images` with `graphics` array supporting IMAGE, LINE, RECT, PATH, SHADING elements
- `1.2` - Added document metadata (Schema.org) and fulltext content extraction
- `1.1` - Added barcode detection (QR, DataMatrix, PDF417, Aztec) and SPAYD payment parsing
- `1.0` - Initial version with text block extraction

## Root Structure

```json
{
  "vim_version": "1.5",
  "document": { ... },
  "layers": {
    "physical": { ... },
    "structural": { ... },
    "semantic": { ... },
    "toc": { ... },
    "extraction": { ... },
    "prescriptive": { ... }
  }
}
```

The layers `toc`, `extraction`, and `prescriptive` are optional and are left out when absent.

## Document Info

```json
{
  "document": {
    "source": "filename.pdf",
    "pages": 6,
    "metadata": {
      "encodingFormat": "application/pdf",
      "contentSize": 1024000,
      "dateCreated": "2026-01-15T10:30:00Z",
      "dateModified": "2026-01-20T14:45:00Z",
      "sha256": "a1b2c3d4e5f6...",
      "inLanguage": "cs",
      "creator": "Jan Novak",
      "producer": "Microsoft Word 2021",
      "title": "Invoice 2026-001",
      "additionalProperties": {
        "pageCount": "6"
      }
    }
  }
}
```

| Field | Type | Description |
| ----- | ---- | ----------- |
| `source` | string | Original filename |
| `pages` | integer | Total page count |
| `metadata` | object | Schema.org compatible document metadata (v1.2+) |

### Document Metadata Fields (v1.2+)

| Field | Type | Description |
| ----- | ---- | ----------- |
| `encodingFormat` | string | MIME type (e.g., "application/pdf") |
| `contentSize` | integer | File size in bytes |
| `dateCreated` | string | ISO-8601 creation timestamp |
| `dateModified` | string | ISO-8601 modification timestamp |
| `extraction_date` | string | ISO-8601 timestamp of the VIM creation; read and kept, not written by the parser itself |
| `sha256` | string | SHA-256 hash (optional, use `--hash` flag) |
| `inLanguage` | string | ISO language code (e.g., "cs", "en") |
| `creator` | string | Document author |
| `producer` | string | Application that created the document |
| `title` | string | Document title |
| `additionalProperties` | object | Additional metadata key-value pairs |

## Physical Layer

The physical layer contains raw text blocks with their exact positions on the page.

```json
{
  "physical": {
    "blocks": [
      {
        "id": "block_1",
        "page": 1,
        "bbox": {
          "x": 50.0,
          "y": 100.0,
          "width": 200.0,
          "height": 150.0
        },
        "textElements": [
          {
            "text": "Sample text",
            "bbox": { "x": 50.0, "y": 100.0, "width": 80.0, "height": 12.0 },
            "page": 1,
            "fontSize": 12.0,
            "fontName": "Arial"
          },
          {
            "text": "The quick brown fox...",
            "bbox": { "x": 29.35, "y": 241.85, "width": 223.17, "height": 27.25 },
            "page": 1,
            "fontSize": 9.0,
            "fontName": "TimesNewRoman",
            "drop_cap_merged": true,
            "drop_cap_bbox": { "x": 29.35, "y": 241.85, "width": 23.17, "height": 27.25 }
          }
        ]
      }
    ]
  }
}
```

### Physical Block Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `id` | string | Unique block identifier (e.g., "block_1") |
| `page` | integer | Page number (1-based) |
| `bbox` | BoundingBox | Block bounding box |
| `textElements` | array | List of text elements in the block |

### BoundingBox Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `x` | float | Left edge X coordinate |
| `y` | float | Top edge Y coordinate |
| `width` | float | Block width |
| `height` | float | Block height |

### TextElement Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `text` | string | Text content |
| `bbox` | BoundingBox | Element bounding box |
| `page` | integer | Page number |
| `fontSize` | float | Font size in points |
| `fontName` | string | Font name |
| `drop_cap_merged` | boolean | Optional. `true` when this element was reconstructed by merging an oversized initial letter (drop cap) with the adjacent body text in the Layer 1.5 reconstruction pipeline. Omitted when `false`. |
| `drop_cap_bbox` | BoundingBox | Optional. Original bounding box of the merged drop cap glyph (before union with body text). Present only when `drop_cap_merged` is `true`. |

### Physical Barcodes (v1.1+)

```json
{
  "physical": {
    "blocks": [ ... ],
    "barcodes": [
      {
        "id": "barcode_1",
        "page": 1,
        "bbox": {"x": 450, "y": 280, "width": 80, "height": 80},
        "format": "QR_CODE",
        "raw_content": "SPD*1.0*ACC:CZ6508000000192000145399*AM:1667.83*CC:CZK"
      }
    ]
  }
}
```

### Physical Barcode Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `id` | string | Unique barcode identifier (e.g., "barcode_1") |
| `page` | integer | Page number (1-based) |
| `bbox` | BoundingBox | Barcode bounding box |
| `format` | string | Barcode format (see supported formats) |
| `raw_content` | string | Decoded barcode content |

### Supported Barcode Formats

| Format | Description |
| ------ | ----------- |
| `QR_CODE` | QR Code (most common for payments) |
| `DATA_MATRIX` | DataMatrix (product tracking) |
| `PDF_417` | PDF417 (ID documents, tickets) |
| `AZTEC` | Aztec Code (transport tickets) |

### Graphic Elements (v1.3+)

Graphic elements extracted from PDF content stream: images, vector paths, lines, rectangles, and shadings.

```json
{
  "physical": {
    "blocks": [ ... ],
    "barcodes": [ ... ],
    "tables": [ ... ],
    "graphics": [
      {
        "id": "graphic_1",
        "page": 1,
        "element_type": "IMAGE",
        "bbox": {"x": 120, "y": 340, "width": 400, "height": 300},
        "image_type": "embedded",
        "format": "jpeg",
        "width_px": 800,
        "height_px": 600,
        "color_space": "DeviceRGB",
        "bits_per_component": 8
      },
      {
        "id": "graphic_2",
        "page": 1,
        "element_type": "RECT",
        "bbox": {"x": 50, "y": 100, "width": 200, "height": 150},
        "stroked": true,
        "filled": false,
        "stroke_width": 1.0,
        "points": [
          {"x": 50, "y": 100, "type": "move"},
          {"x": 250, "y": 100, "type": "line"},
          {"x": 250, "y": 250, "type": "line"},
          {"x": 50, "y": 250, "type": "line"}
        ]
      }
    ]
  }
}
```

### Graphic Element Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `id` | string | Unique element identifier (e.g., "graphic_1") |
| `page` | integer | Page number (1-based) |
| `bbox` | BoundingBox | Element bounding box |
| `element_type` | string | Element type: `IMAGE`, `LINE`, `RECT`, `PATH`, `SHADING` |

### Image-specific Fields (element_type=IMAGE)

| Field | Type | Description |
| ----- | ---- | ----------- |
| `image_type` | string | `"embedded"` or `"inline"` |
| `format` | string | Image format (`"jpeg"`, `"png"`, `"tiff"`, `"jpeg2000"`) |
| `width_px` | integer | Pixel width |
| `height_px` | integer | Pixel height |
| `color_space` | string | Color space (e.g., `"DeviceRGB"`) |
| `bits_per_component` | integer | Bits per color component |

### Path-specific Fields (element_type=LINE, RECT, PATH)

| Field | Type | Description |
| ----- | ---- | ----------- |
| `points` | array | List of PathPoint objects |
| `stroke_width` | float | Line width in points |
| `filled` | boolean | Whether the path is filled |
| `stroked` | boolean | Whether the path is stroked |
| `winding_rule` | string | `"even_odd"` or `"non_zero"` (for fills) |

### PathPoint Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `x` | float | X coordinate |
| `y` | float | Y coordinate |
| `type` | string | Point type: `"move"`, `"line"`, `"curve_cp1"`, `"curve_cp2"`, `"curve_end"` |

### Shading-specific Fields (element_type=SHADING)

| Field | Type | Description |
| ----- | ---- | ----------- |
| `shading_name` | string | PDF shading resource name |

### Fulltext Content (v1.2+)

Raw fulltext content extracted from the entire document. Useful for full-text search indexing.

```json
{
  "physical": {
    "blocks": [ ... ],
    "barcodes": [ ... ],
    "tables": [ ... ],
    "graphics": [ ... ],
    "fulltext": {
      "content": "Complete document text content...",
      "wordCount": 1234,
      "characterCount": 7890
    }
  }
}
```

### Fulltext Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `content` | string | Complete text content of the document |
| `wordCount` | integer | Total word count |
| `characterCount` | integer | Total character count |

**Note:** Fulltext is only included when using the `--fulltext` CLI flag (disabled by default due to potentially large output).

## Structural Layer

The structural layer groups text into logical blocks with headers and content.

```json
{
  "structural": {
    "blocks": [
      {
        "id": "block_1",
        "type": "labeled_content",
        "significance": "primary",
        "page": 1,
        "header": "Zákazník",
        "content": [
          "Jan Novák",
          "Hlavní 123",
          "120 00 Praha 2"
        ],
        "confidence": 0.85
      },
      {
        "id": "block_5",
        "type": "page_number",
        "significance": "auxiliary",
        "page": 1,
        "header": null,
        "content": ["1"]
      }
    ]
  }
}
```

### Structural Block Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `id` | string | Block identifier (matches physical block) |
| `type` | string | Block type (see Block Types below) |
| `significance` | string | Block significance: `"primary"` or `"auxiliary"` (v1.5+) |
| `page` | integer | Page number |
| `header` | string | Block header/title (optional) |
| `content` | array | List of content lines |
| `confidence` | float | Detection confidence (0.0-1.0) |

### Block Types

| Type | Significance | Description |
| ---- | ------------ | ----------- |
| `labeled_content` | primary | Block with identified header (chapters, sections) |
| `content` | primary | Block without header (plain text, paragraphs) |
| `toc` | auxiliary | Block on explicit TOC page (v1.4+) |
| `page_number` | auxiliary | Page number block (v1.5+) |

### Block Significance (v1.5+)

Blocks are classified into two significance levels:

| Significance | Description |
| ------------ | ----------- |
| `primary` | Semantically important blocks (chapters, paragraphs, labeled sections) |
| `auxiliary` | Decorative/navigational blocks (page numbers, TOC entries) |

The HTML visualizer hides auxiliary blocks by default and provides a toggle to show them.

### Structural Barcodes (v1.1+)

```json
{
  "structural": {
    "blocks": [ ... ],
    "barcodes": [
      {
        "id": "barcode_1",
        "format": "QR_CODE",
        "content_type": "spayd",
        "nearby_blocks": ["block_5", "block_6"],
        "confidence": 0.95
      }
    ]
  }
}
```

### Structural Barcode Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `id` | string | Barcode identifier (matches physical barcode) |
| `format` | string | Barcode format |
| `content_type` | string | Detected content type (see types below) |
| `nearby_blocks` | array | IDs of text blocks in proximity |
| `confidence` | float | Detection confidence (0.0-1.0) |

### Barcode Content Types

| Type | Description |
| ---- | ----------- |
| `spayd` | Czech QR Platba payment (SPAYD format) |
| `url` | HTTP/HTTPS URL |
| `vcard` | vCard contact information |
| `text` | Plain text or unknown format |

## Semantic Layer

The semantic layer contains recognized entities with typed fields.

```json
{
  "semantic": {
    "entities": [
      {
        "type": "customer",
        "sourceBlock": "block_1",
        "confidence": 0.85,
        "fields": {
          "name": "Jan Novák",
          "street": "Hlavní 123",
          "postal_address": "120 00 Praha 2"
        }
      }
    ]
  }
}
```

### Entity Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `type` | string | Entity type (see types below) |
| `sourceBlock` | string | ID of source block |
| `confidence` | float | Recognition confidence (0.0-1.0) |
| `fields` | object | Key-value pairs of extracted data |

### Entity Types

| Type | Description | Common Fields |
| ---- | ----------- | ------------- |
| `customer` | Customer/buyer information | name, street, postal_address, city |
| `recipient` | Recipient/addressee | name, street, postal_address, city |
| `supplier` | Supplier/vendor | name, street, postal_address, city |
| `invoice` | Invoice details | number, date, amount |
| `account` | Account information | number, iban |

### Common Entity Fields

| Field | Description |
| ----- | ----------- |
| `name` | Person or company name |
| `street` | Street address |
| `postal_address` | Postal code and city |
| `city` | City name |
| `number` | Invoice/account number |

### Payments (v1.1+)

SPAYD (Short Payment Descriptor) payments extracted from QR codes.

```json
{
  "semantic": {
    "entities": [ ... ],
    "payments": [
      {
        "type": "payment_qr",
        "source_barcode": "barcode_1",
        "format": "spayd",
        "fields": {
          "account": "CZ6508000000192000145399",
          "amount": 1667.83,
          "currency": "CZK",
          "variable_symbol": "0000000001",
          "message": "Platba za elektrinu"
        },
        "confidence": 0.92
      }
    ]
  }
}
```

### Payment Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `type` | string | Always "payment_qr" |
| `source_barcode` | string | ID of source barcode |
| `format` | string | Payment format ("spayd") |
| `fields` | object | Payment data (see below) |
| `confidence` | float | Parsing confidence (0.0-1.0) |

### SPAYD Payment Data Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `account` | string | IBAN or Czech account number |
| `amount` | number | Payment amount |
| `currency` | string | Currency code (default: CZK) |
| `variable_symbol` | string | Variable symbol (X-VS) |
| `constant_symbol` | string | Constant symbol (X-KS) |
| `specific_symbol` | string | Specific symbol (X-SS) |
| `message` | string | Message for recipient |
| `recipient_name` | string | Recipient name |

## TOC Layer (v1.4+)

The TOC (Table of Contents) layer provides a structured chapter-to-page mapping using two detection strategies:

1. **Explicit TOC page detection** — parses printed Table of Contents pages (keywords: "Obsah", "Contents", "Inhalt", etc.)
2. **Heuristic heading detection** — analyzes font size, weight, and position to identify headings

Explicit entries take priority and are cross-referenced with heuristic results.

```json
{
  "toc": {
    "toc_page": 4,
    "entries": [
      {
        "title": "Úvod",
        "page": 5,
        "level": 1,
        "block_id": "block_1",
        "confidence": 1.00,
        "source": "explicit"
      },
      {
        "title": "I.",
        "page": 6,
        "level": 1,
        "block_id": "block_1",
        "confidence": 1.00,
        "source": "explicit"
      },
      {
        "title": "Obchodní platby",
        "page": 1,
        "level": 2,
        "block_id": "block_5",
        "confidence": 0.80,
        "source": "heuristic"
      }
    ]
  }
}
```

### TOC Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `toc_page` | integer? | Page number where explicit TOC was found (null if none) |
| `entries` | array | List of TOC entries |

### TOC Entry Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `title` | string | Heading text |
| `page` | integer | Target page number (1-based) |
| `level` | integer | Heading level (1 = main, 2 = sub, 3 = minor) |
| `block_id` | string | Reference to source structural block |
| `confidence` | float | Detection confidence (0.0-1.0) |
| `source` | string? | Detection method: `"explicit"` (from TOC page) or `"heuristic"` (from font analysis) |

### Explicit TOC Page Detection

Documents containing a printed Table of Contents (e.g., books, reports) are detected by:

- **TOC keywords** in page headers: "Obsah", "Table of Contents", "Contents", "Inhalt", "Sommaire"
- **Entry patterns** matching `Title.........PageNumber` or `Title    PageNumber`
- Minimum 3 matching entries required to confirm a TOC page

Structural blocks on the detected TOC page are marked with `"type": "toc"` in the structural layer.

### Heuristic Heading Detection

Fallback/supplementary detection based on:

- **Font size** — text significantly larger than the document's base font size
- **Font weight** — bold fonts (Bold, ExtraBold, SemiBold, Heavy, Black)
- **Position** — elements at the top of a page receive a confidence bonus
- **Existing headers** — leverages structural block headers from fulltext extraction

### Heading Level Assignment

Levels are assigned relative to the document's heading font size distribution:

- **Level 1** — largest heading font size (or roman numerals, chapter numbers from explicit TOC)
- **Level 2** — second largest heading font size (or sub-section numbering like "1.1")
- **Level 3** — third largest or any remaining heading size

**Note:** The `toc` layer is only present when headings are detected. Documents without clear headings will not include this layer.

## Prescriptive Layer (v2.1+)

The prescriptive layer stores what a **definition file** prescribes. A definition file is a
document that prescribes something, for example a specification, a guideline, or a contract. An
item of the layer is a **provision**.

`vim-parser` does not produce the layer. Another producer writes it, and the Java model in
`vim-parser-api` (package `model.prescriptive`) reads and writes it without data loss. The
binding source for names, types, required fields, and enum values are the contract schemas
`vim-prescriptive.schema.json` and `vim-prescriptive-provision.schema.json` of the `api`
repository.

```json
{
  "prescriptive": {
    "definition": {
      "identifier": "DF-1234",
      "edition": "3",
      "issued": "2025-04-01",
      "title": "Drawing title block rules",
      "publisher": "Example Org"
    },
    "provenance": {
      "extraction_method": "llm_extract",
      "engine": "model-id",
      "timestamp": "2026-10-09T10:00:00Z"
    },
    "provisions": [
      {
        "provision_id": "p-5.3.2-1",
        "kind": "requirement",
        "clause": "5.3.2",
        "text": "The title block shall contain the drawing number.",
        "text_hash": "1f77c4a6db023bea",
        "anchors": [
          { "page": 7, "element_type": "block", "element_id": "block_41" }
        ],
        "references": ["r-1"],
        "terms": ["t-title-block"],
        "relations": [
          { "provision_id": "p-5.3.1-1", "relation": "refines" }
        ],
        "verification": {
          "method": "automatic",
          "subject": "drawing",
          "rule": { "field": "drawing_number", "check": "present" }
        },
        "status": "confirmed",
        "confidence": 0.82
      }
    ],
    "references": [
      {
        "reference_id": "r-1",
        "scope": "external",
        "binding": "mandatory",
        "target": { "identifier": "DF-0456", "edition": "2", "clause": "4.1" }
      }
    ],
    "terms": [
      {
        "term_id": "t-title-block",
        "term": "title block",
        "definition": "Area of a drawing that holds its identification data.",
        "clause": "3.4"
      }
    ]
  }
}
```

### Prescriptive Layer Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `definition` | object? | Identity of the definition file: `identifier`, `edition`, `issued`, `title`, `publisher` |
| `provenance` | object? | Default provenance of all items in the layer |
| `provisions` | array | Provisions of the definition file, in reading order (required) |
| `references` | array? | References to other documents and clauses |
| `terms` | array? | Terms that the definition file defines |

### Provision Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `provision_id` | string | Stable identifier, unique in the layer |
| `kind` | string | `requirement` (shall, must), `recommendation` (should), or `permission` (may, need not) |
| `clause` | string? | Number or label of the clause that holds the provision |
| `text` | string? | Verbatim text; at least one of `text` and `statement` is present |
| `statement` | string? | The provision in the words of the producer |
| `text_hash` | string? | First 16 hexadecimal characters of the SHA-256 hash of the normalized verbatim text |
| `anchors` | array | Places of the provision in the definition file, at least one |
| `condition` | string? | When the provision applies |
| `references` | array? | `reference_id` values that the provision uses |
| `terms` | array? | `term_id` values that the provision uses |
| `relations` | array? | Links to other provisions: `provision_id` and `relation` |
| `verification` | object? | How the provision can be checked: `method`, `subject`, `rule` |
| `status` | string? | `proposed`, `confirmed`, or `rejected`; an absent value means `proposed` and is not added on write |
| `confidence` | number? | Confidence of the producer (0.0-1.0) |
| `provenance` | object? | Provenance of this provision; overrides the provenance of the layer |
| `revisions` | array? | Audit trail; `property` of a revision names the changed property |

### Anchor Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `page` | integer | Page number (1-based) |
| `element_type` | string? | `block`, `table`, or `graphic` |
| `element_id` | string? | Identifier of the element in the physical layer of the same document |
| `bbox` | object? | Region on the page |

### Reference Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `reference_id` | string | Identifier, unique in the layer |
| `scope` | string | `external` (another document) or `internal` (another clause of this file) |
| `binding` | string? | `mandatory` or `informative` |
| `target` | object | `identifier`, `edition`, `title`, `clause`, `uri`; an external target has an identifier, an internal target has a clause |
| `anchors` | array? | Places where the reference is written |
| `confidence` | number? | Confidence of the producer (0.0-1.0) |
| `provenance` | object? | Provenance of this reference |

### Term Fields

| Field | Type | Description |
| ----- | ---- | ----------- |
| `term_id` | string | Identifier, unique in the layer |
| `term` | string | The term |
| `definition` | string? | Text of the definition |
| `clause` | string? | Clause that defines the term |
| `anchors` | array? | Places of the definition in the file |
| `confidence` | number? | Confidence of the producer (0.0-1.0) |
| `provenance` | object? | Provenance of this term |

### Other Enum Values

| Field | Values |
| ----- | ------ |
| `relations[].relation` | `refines`, `depends_on`, `exception_to`, `conflicts_with` |
| `verification.method` | `automatic`, `test`, `manual` |

### Round Trip Rules

- Unknown properties inside the layer are ignored on read
- A value outside an enum or of a wrong JSON type inside the layer is a load error of the whole
  document, as in the `extraction` layer
- Null values and empty optional arrays are not written; `provisions` and `anchors` are always written
- An empty optional array in the input (`"references": []`) is left out on write
- A `bbox` is written with the computed keys `right` and `bottom`, as in the other layers
- The golden fixture `vim-parser-api/src/test/resources/fixtures/prescriptive-layer-v2.1.vim.json`
  is the shared reference for the Java and the Python model

## Example: Complete VIM Document

```json
{
  "vim_version": "1.0",
  "document": {
    "source": "invoice.pdf",
    "pages": 1
  },
  "layers": {
    "physical": {
      "blocks": [
        {
          "id": "block_1",
          "page": 1,
          "bbox": {"x": 50, "y": 80, "width": 200, "height": 100},
          "textElements": [
            {"text": "Zákazník", "bbox": {"x": 50, "y": 80, "width": 70, "height": 14}, "page": 1, "fontSize": 14, "fontName": "Arial-Bold"},
            {"text": "Jan Novák", "bbox": {"x": 50, "y": 100, "width": 80, "height": 12}, "page": 1, "fontSize": 12, "fontName": "Arial"}
          ]
        },
        {
          "id": "block_2",
          "page": 1,
          "bbox": {"x": 300, "y": 80, "width": 200, "height": 100},
          "textElements": [
            {"text": "Adresát", "bbox": {"x": 300, "y": 80, "width": 60, "height": 14}, "page": 1, "fontSize": 14, "fontName": "Arial-Bold"},
            {"text": "Eva Nová", "bbox": {"x": 300, "y": 100, "width": 70, "height": 12}, "page": 1, "fontSize": 12, "fontName": "Arial"}
          ]
        }
      ]
    },
    "structural": {
      "blocks": [
        {
          "id": "block_1",
          "page": 1,
          "type": "labeled_content",
          "header": "Zákazník",
          "content": ["Jan Novák"],
          "confidence": 0.9
        },
        {
          "id": "block_2",
          "page": 1,
          "type": "labeled_content",
          "header": "Adresát",
          "content": ["Eva Nová"],
          "confidence": 0.9
        }
      ]
    },
    "semantic": {
      "entities": [
        {
          "type": "customer",
          "sourceBlock": "block_1",
          "confidence": 0.8,
          "fields": {"name": "Jan Novák"}
        },
        {
          "type": "recipient",
          "sourceBlock": "block_2",
          "confidence": 0.8,
          "fields": {"name": "Eva Nová"}
        }
      ]
    }
  }
}
```

## CLI Usage

### Extract blocks from PDF

```bash
vim-parser extract-blocks invoice.pdf              # Output to stdout (with barcodes)
vim-parser extract-blocks -w invoice.pdf           # Write to invoice.pdf.vim.json
vim-parser extract-blocks -o out.json invoice.pdf  # Write to out.json
vim-parser extract-blocks --compact invoice.pdf    # Compact JSON
vim-parser extract-blocks --no-barcodes invoice.pdf # Disable barcode detection (faster)
vim-parser extract-blocks -v invoice.pdf           # Verbose mode
```

### Extract only barcodes (v1.1+)

```bash
vim-parser extract-barcodes invoice.pdf            # Output barcode list to stdout
vim-parser extract-barcodes -o barcodes.json invoice.pdf  # Write to file
vim-parser extract-barcodes -v invoice.pdf         # Verbose mode
```

### Visualize VIM JSON as HTML

```bash
vim-parser visualize invoice.pdf.vim.json          # Output to stdout
vim-parser visualize -w invoice.pdf.vim.json       # Write to invoice.pdf.vim.html
vim-parser visualize -o view.html invoice.pdf.vim.json
```

## Notes

- Coordinates use PDF coordinate system (origin at bottom-left, Y increases upward)
- Font names may include style suffixes (e.g., "Arial-Bold", "Times-Italic")
- Confidence scores range from 0.0 (low) to 1.0 (high)
- Block IDs are unique within a document and consistent across layers
