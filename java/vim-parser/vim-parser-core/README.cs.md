# vim-parser-core

Hlavní orchestrační modul VIM Parseru. Spravuje životní cyklus pluginů a jejich provádění.

## Komponenty

### PluginManager

Objevuje a spravuje pluginy přes Java ServiceLoader.

```java
PluginManager manager = new PluginManager();
manager.discoverPlugins();

List<ExtractorPlugin> extractors = manager.getExtractorPlugins();
ParserPlugin spayd = manager.getParserPlugin("spayd");
```

### ExecutionEngine

Spouští pluginy v konfigurovatelných režimech provádění.

```java
ExecutionEngine engine = new ExecutionEngine(manager, ExecutionMode.ASYNC_THREAD);
engine.setThreadPoolSize(4);

VimDocument result = engine.process(pdfContent);
```

## CLI příkazy

```bash
# extrakce z PDF
vim-parser extract input.pdf -o output.vim.json

# extrakce s konkrétním režimem a vybranými pluginy
vim-parser extract --mode=thread --threads=4 --plugins=fulltext,barcode input.pdf

# výpis dostupných pluginů
vim-parser plugins

# vizualizace VIM dokumentu
vim-parser visualize input.vim.json -o output.html
```

## Konfigurace

Pomocí `vim-parser.json`:

```json
{
  "execution": {
    "mode": "thread",
    "threadPoolSize": 4
  },
  "plugins": {
    "enabled": ["fulltext", "barcode", "spayd"],
    "disabled": []
  }
}
```

## Závislosti

- vim-parser-api
- Všechny moduly pluginů (runtime)
- Picocli (CLI)
- Jackson (JSON)
