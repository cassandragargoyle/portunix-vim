# vim-parser-core

Core orchestrator module for VIM Parser. Manages plugin lifecycle and execution.

## Components

### PluginManager

Discovers and manages plugins via Java ServiceLoader.

```java
PluginManager manager = new PluginManager();
manager.discoverPlugins();

List<ExtractorPlugin> extractors = manager.getExtractorPlugins();
ParserPlugin spayd = manager.getParserPlugin("spayd");
```

### ExecutionEngine

Executes plugins with configurable execution modes.

```java
ExecutionEngine engine = new ExecutionEngine(manager, ExecutionMode.ASYNC_THREAD);
engine.setThreadPoolSize(4);

VimDocument result = engine.process(pdfContent);
```

## CLI Commands

```bash
# Extract from PDF
vim-parser extract input.pdf -o output.vim.json

# Extract with specific mode and plugins
vim-parser extract --mode=thread --threads=4 --plugins=fulltext,barcode input.pdf

# List available plugins
vim-parser plugins

# Visualize VIM document
vim-parser visualize input.vim.json -o output.html
```

## Configuration

Via `vim-parser.json`:

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

## Dependencies

- vim-parser-api
- All plugin modules (runtime)
- Picocli (CLI)
- Jackson (JSON)
