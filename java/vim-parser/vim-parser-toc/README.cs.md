# vim-parser-toc

Plugin pro extrakci obsahu (Table of Contents) ve VIM Parseru. Detekuje kapitoly/nadpisy a mapuje je na čísla stránek, čímž vytváří navigovatelnou osnovu dokumentu.

## Vlastnosti

- Detekce explicitní stránky obsahu (vytištěné stránky typu „Obsah", „Contents")
- Heuristická detekce nadpisů na základě velikosti písma, řezu a pozice
- Sloučení explicitních a heuristických výsledků s navýšením confidence
- Označení zdrojových bloků obsahu typem `type=toc` pro navazující pluginy
- Hierarchické položky (úroveň + stránka + název)

## Algoritmus

1. Načti `textBlocks` ze sdíleného kontextu (vyprodukováno pluginem `fulltext`)
2. **Fáze 1 — Explicitní obsah**: prohledej bloky, najdi vytištěnou stránku obsahu a extrahuj záznamy `název → stránka`
3. **Fáze 2 — Heuristické nadpisy**: detekuj kandidátní nadpisy heuristikami nad fontem a pozicí
   - Stránky, které vypadají jako výkres (mnoho bloků na listu větším než A3, málo běžného textu), se přeskočí
   - Jako šum se zahodí: čísla, kóty, značky a jednotlivé znaky (bez úseku 3 písmen),
     popisky formulářových polí (`Popisek:` následovaný krátkou hodnotou vedle nebo pod ním)
     a názvy opakované na stejném místě na 3+ stránkách
4. **Fáze 3 — Sloučení**:
   - Explicitní záznamy mají přednost
   - Odpovídající heuristické záznamy zvyšují confidence explicitního záznamu a poskytnou jeho `block_id`
   - Heuristické záznamy bez překryvu jsou ponechány jako doplňkové
   - Výsledný seznam je seřazen podle čísla stránky

## Výstup

```json
{
  "toc": {
    "tocPage": 3,
    "entries": [
      {
        "title": "Úvod",
        "page": 5,
        "level": 1,
        "blockId": "block_42",
        "confidence": 0.95
      },
      {
        "title": "Architektura",
        "page": 12,
        "level": 1,
        "blockId": "block_88",
        "confidence": 0.90
      }
    ]
  }
}
```

## Klíče sdíleného kontextu

- `toc` — výsledný objekt `TableOfContents`
- `tocBlockIds` — ID textových bloků náležejících k vytištěné stránce obsahu (pro anotaci ve strukturální vrstvě)

## Konfigurace

- Priorita: 50 (spouští se po fulltextu (10) a barcode (20))
- Fáze: `TRANSFORM`
- Závislosti: `fulltext`
- Předchůdci: `fulltext`
- Automaticky povoleno: ano
- Režimy provádění: SYNC, ASYNC_THREAD

## Závislosti

- vim-parser-api
- org-cassandragargoyle-api (logování)
