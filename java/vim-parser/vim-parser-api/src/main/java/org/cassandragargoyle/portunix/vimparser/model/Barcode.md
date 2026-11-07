---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Barcode

Barcode detected on a page. It is the working model that the barcode plugin shares with the
other plugins. The VIM document stores it converted to `PhysicalBarcode` and `StructuralBarcode`.

The JSON key order is fixed by `@JsonPropertyOrder` and lists every written key. The helper
methods `isSpaydPayment` and `isUrl` are marked with `@JsonIgnore` and are not written.

## Structure

```mermaid
classDiagram
    class Barcode {
        -String id
        -BoundingBox bbox
        -int page
        -String format
        -String rawContent
        -String contentType
        -double confidence
        +isSpaydPayment() boolean
        +isUrl() boolean
    }
    Barcode --> "0..1" BoundingBox : bbox
```

## JSON Mapping

Keys are listed in the order they are written.

| Java field | JSON key | Type | Written |
| ---------- | -------- | ---- | ------- |
| `id` | `id` | `String` | always |
| `bbox` | `bbox` | `BoundingBox` | always |
| `page` | `page` | `int` | always |
| `format` | `format` | `String` | always |
| `rawContent` | `raw_content` | `String` | always |
| `contentType` | `content_type` | `String` | always |
| `confidence` | `confidence` | `double` | always |

## Main Flow

```mermaid
sequenceDiagram
    participant BarcodePlugin as BarcodeExtractorPlugin
    participant Context as PluginContext
    participant Spayd as SpaydParserPlugin
    participant Engine as ExecutionEngine
    BarcodePlugin->>Context: shared data "barcodes" (List of Barcode)
    Spayd->>Context: read "barcodes"
    Spayd->>Spayd: isSpaydPayment() selects payment codes
    Engine->>Context: read "barcodes"
    Engine->>Engine: convert to PhysicalBarcode and StructuralBarcode
```
