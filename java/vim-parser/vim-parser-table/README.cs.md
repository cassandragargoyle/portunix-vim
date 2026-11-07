# vim-parser-table

Plugin pro extrakci tabulek ve VIM Parseru. Detekuje tabulková data v PDF dokumentech a extrahuje strukturovaný obsah buněk pomocí PDFBox.

## Vlastnosti

- Detekce tabulek v PDF dokumentech po jednotlivých stránkách
- Extrakce řádků, sloupců a jednotlivých buněk
- Reportování pozic buněk (bounding boxů) a jejich textového obsahu
- Možnost rozsahu na úrovni stránky (jedna stránka nebo celý dokument)
- Tenký adaptér — detekci deleguje na sdílenou knihovnu `table-detector-pdf`

## Architektura

Plugin je tenký adaptér nad samostatnými knihovnami `table-detector`:

```text
┌──────────────────────────────────────┐
│    TableExtractorPlugin (adaptér)    │
└──────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────┐
│  table-detector-pdf                  │
│    └─ PdfTableDetector               │
└──────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────┐
│  table-detector-api (TableModel)     │
└──────────────────────────────────────┘
```

Toto oddělení umožňuje vyvíjet detekční algoritmus nezávisle na kontraktu pluginu pro VIM.

## Algoritmus

1. Načti PDF přes PDFBox
2. Z `PluginContext` urči rozsah stránek (jedna stránka nebo všechny)
3. Pro každou stránku spusť `PdfTableDetector.detectTables(document, pageNum)`
4. Přiděl monotónně rostoucí ID (`table-1`, `table-2`, …) a číslo zdrojové stránky
5. Výsledný seznam ulož pod sdíleným klíčem `tables` pro navazující pluginy
6. Vrať agregovaná metadata (počet tabulek, počet buněk, doba zpracování)

## Výstup

```json
{
  "id": "table-1",
  "page": 3,
  "rows": 4,
  "cols": 3,
  "bbox": {"x": 50, "y": 200, "width": 500, "height": 180},
  "cells": [
    {
      "row": 0,
      "col": 0,
      "text": "Položka",
      "bbox": {"x": 50, "y": 200, "width": 160, "height": 25}
    },
    {
      "row": 0,
      "col": 1,
      "text": "Množství",
      "bbox": {"x": 210, "y": 200, "width": 160, "height": 25}
    }
  ]
}
```

## Klíče sdíleného kontextu

- `tables` — seznam objektů `TableModel` detekovaných napříč zpracovanými stránkami

## Konfigurace

- Priorita: 30 (spouští se po extrakci čárových kódů)
- Fáze: `EXTRACT`
- Závislosti: žádné
- Předchůdci: žádní
- Automaticky povoleno: ano
- Režimy provádění: SYNC, ASYNC_THREAD

## Závislosti

- vim-parser-api
- table-detector-api
- table-detector-pdf
- PDFBox
- Jackson (JSON)
- org-cassandragargoyle-api (logování)
