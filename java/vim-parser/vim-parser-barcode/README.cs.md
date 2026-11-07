# vim-parser-barcode

Plugin pro extrakci čárových kódů z VIM Parseru využívající knihovnu ZXing.

## Podporované formáty

- QR Code
- Data Matrix
- PDF417
- Aztec

## Použití

Plugin je automaticky objeven pomocí ServiceLoader. Extrahuje čárové kódy ze stránek PDF tak, že je nejprve vykreslí jako obrázky a poté je skenuje pomocí knihovny ZXing.

## Výstup

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

## Detekce typu obsahu

- `spayd` - SPAYD platební QR kódy (začínají na `SPD*`)
- `url` - HTTP/HTTPS URL adresy
- `vcard` - vCard kontaktní data
- `text` - prostý text

## Konfigurace

- Rozlišení vykreslování: 150 DPI (konfigurovatelné — kompromis mezi kvalitou a rychlostí)
- Priorita: 20 (spouští se po extrakci plného textu)

## Závislosti

- vim-parser-api
- ZXing (core, javase)
- PDFBox (pro vykreslování PDF)
