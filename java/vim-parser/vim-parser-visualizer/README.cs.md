# vim-parser-visualizer

Plugin pro HTML vizualizaci VIM Parseru.

## Vlastnosti

- Interaktivní HTML výstup s zobrazením vrstev v záložkách
- Tři pohledy na vrstvy: Strukturální, Fyzická, Sémantická
- Barevně odlišené textové bloky
- Detaily platebních QR kódů
- Zobrazení sémantických entit
- Escapování HTML proti XSS

## Výstup

Generuje samostatný HTML soubor s vloženým CSS a JavaScriptem.

### Záložky

1. **Strukturální** — logické textové bloky se záhlavími a obsahem
2. **Fyzická** — surové textové prvky se souřadnicemi
3. **Sémantická** — extrahované entity a platby

## Použití

```bash
vim-parser visualize input.vim.json -o output.html
```

Nebo programově:

```java
VimHtmlVisualizerPlugin visualizer = new VimHtmlVisualizerPlugin();
String html = visualizer.visualize(vimDocument);
```

## Přípona souboru

Výstupní soubory používají příponu `.vim.html` jako indikátor VIM vizualizace.

## Závislosti

- vim-parser-api
- Žádné externí závislosti (generuje samostatné HTML)
