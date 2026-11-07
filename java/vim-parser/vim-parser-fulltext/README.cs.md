# vim-parser-fulltext

Plugin pro extrakci textových bloků z VIM Parseru využívající PDFBox.

## Vlastnosti

- Extrakce textu včetně pozičních informací
- Clusterizace textu do logických bloků
- Detekce vícesloupcového layoutu
- Rozlišování záhlaví od obsahu
- Zachování informací o fontech

## Algoritmus

1. Extrakce textových elementů se souřadnicemi pomocí PDFBox; znaky se skládají do běhů
   podle účaří, takže vzdálené nebo překrývající se popisky na jednom řádku PDFBox zůstanou oddělené
2. Detekce sloupců na základě horizontálních mezer
3. Clusterizace elementů do bloků podle vertikální blízkosti a horizontálního překryvu
4. Analýza struktury bloku (detekce záhlaví)

## Výstup

```json
{
  "id": "block_1",
  "page": 1,
  "type": "labeled_content",
  "header": "Invoice Details",
  "content": ["Date: 2026-01-31", "Invoice #: 12345"],
  "bbox": {"x": 50, "y": 100, "width": 200, "height": 80},
  "confidence": 0.95
}
```

## Typy bloků

- `labeled_content` — blok se záhlavím a řádky obsahu
- `content` — prostý obsah bez záhlaví
- `table` — tabulková data (do budoucna)

## Konfigurace

- Práh horizontální mezery: 50 pt (detekce sloupců); v rámci řádku a mezi blokem
  a elementem nejvýše `min(50 pt, 3 em)`
- Práh vertikální mezery: 15 pt + výška řádku (oddělení bloků), nejvýše 3 em
- Tolerance účaří v rámci běhu: 0,5 em
- Priorita: 10 (spouští se jako první)

## Závislosti

- vim-parser-api
- PDFBox
