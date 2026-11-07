# vim-parser-barcode

Barcode extraction plugin for VIM Parser using ZXing library.

## Supported Formats

- QR Code
- Data Matrix
- PDF417
- Aztec

## Usage

The plugin is automatically discovered via ServiceLoader. It extracts barcodes from PDF pages by rendering them as images and scanning with ZXing.

## Output

```json
{
  "id": "barcode_1",
  "page": 1,
  "format": "QR_CODE",
  "rawContent": "SPD*1.0*ACC:CZ123...",
  "contentType": "spayd",
  "bbox": {"x": 100, "y": 200, "width": 150, "height": 150},
  "confidence": 1.0
}
```

## Content Type Detection

- `spayd` - SPAYD payment QR codes (starts with `SPD*`)
- `url` - HTTP/HTTPS URLs
- `vcard` - vCard contact data
- `text` - Plain text

## Configuration

- Render DPI: 150 (configurable for quality vs speed tradeoff)
- Priority: 20 (runs after fulltext extraction)

## Dependencies

- vim-parser-api
- ZXing (core, javase)
- PDFBox (for PDF rendering)
