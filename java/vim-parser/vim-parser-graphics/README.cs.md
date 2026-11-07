# vim-parser-graphics

Plugin pro extrakci grafiky z VIM Parseru využívající PDFBox. Lokalizuje a kataloguje všechny grafické prvky v PDF dokumentech: obrázky, cesty, čáry, obdélníky a stínované výplně.

## Vlastnosti

- Extrakce vložených i inline obrázků ze stránek PDF
- Detekce vektorových cest, čar a obdélníků z obsahového streamu
- Zachycení stínovaných výplní (gradienty)
- Výpočet bounding boxu pro všechny grafické prvky
- Ohlášení pixelových rozměrů a formátu obrázků (JPEG, PNG, TIFF, JPEG2000)
- Klasifikace tvarů: IMAGE, LINE, RECT, PATH, SHADING
- Sdílení extrahovaných grafických dat s ostatními pluginy přes kontext

## Algoritmus

1. Zpracování každé stránky pomocí `PDFGraphicsStreamEngine`
2. **Akumulace cesty**: `moveTo/lineTo/curveTo/closePath/appendRectangle` přidává body do `currentPath`
3. **Emitace cesty**: `strokePath()`, `fillPath()`, `fillAndStrokePath()` vyšlou grafický prvek a resetují cestu
4. **Klasifikace tvaru**:
   - `appendRectangle` → RECT
   - 1 úsečka → LINE
   - 4 úsečky tvořící obdélník → RECT
   - Všechny ostatní cesty → PATH
5. `drawImage()` → okamžitě vyšle prvek IMAGE
6. `shadingFill()` → okamžitě vyšle prvek SHADING
7. `endPath()` → cestu zahodí (pouze ořez), reset
8. `clip()` → pouze zaznamená do logu, žádný prvek se neemituje

## Výstup

### Prvek IMAGE

```json
{
  "id": "graphic_1",
  "page": 1,
  "element_type": "IMAGE",
  "image_type": "embedded",
  "format": "jpeg",
  "width_px": 800,
  "height_px": 600,
  "color_space": "DeviceRGB",
  "bits_per_component": 8,
  "bbox": {"x": 120, "y": 340, "width": 400, "height": 300}
}
```

### Prvek RECT

```json
{
  "id": "graphic_2",
  "page": 1,
  "element_type": "RECT",
  "stroked": true,
  "filled": true,
  "stroke_width": 1.0,
  "bbox": {"x": 50, "y": 100, "width": 200, "height": 150},
  "points": [
    {"x": 50, "y": 100, "type": "move"},
    {"x": 250, "y": 100, "type": "line"},
    {"x": 250, "y": 250, "type": "line"},
    {"x": 50, "y": 250, "type": "line"}
  ]
}
```

### Prvek LINE

```json
{
  "id": "graphic_3",
  "page": 1,
  "element_type": "LINE",
  "stroked": true,
  "stroke_width": 0.5,
  "bbox": {"x": 50, "y": 200, "width": 500, "height": 0},
  "points": [
    {"x": 50, "y": 200, "type": "move"},
    {"x": 550, "y": 200, "type": "line"}
  ]
}
```

## Konfigurace

- Priorita: 25 (spouští se po extrakci čárových kódů)
- Předchůdci: `barcode`
- Automaticky povoleno: ano
- Režimy provádění: SYNC, ASYNC_THREAD, ASYNC_GRPC

## Závislosti

- vim-parser-api
- PDFBox
