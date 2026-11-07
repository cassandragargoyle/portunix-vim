---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# Main

picocli entry point of `vim-parser`. It sets up logging, discovers plugins, builds the
execution engine and telemetry, and runs one of the subcommands.

## Structure

```mermaid
classDiagram
    class Main {
        -PluginManager pluginManager$
        -ExecutionEngine executionEngine$
        -TelemetryProvider telemetryProvider$
        +main(String[] args)$
        -checkPluginsFound()$ boolean
        -initTelemetry()
    }
    class AutoCommand
    class ExtractCommand
    class VisualizeCommand
    class PluginsCommand
    Main *-- AutoCommand : auto
    Main *-- ExtractCommand : extract
    Main *-- VisualizeCommand : visualize
    Main *-- PluginsCommand : plugins
    Main --> PluginManager
    Main --> ExecutionEngine
    Main ..> VimJsonMapper : JSON read/write
    AutoCommand --> AutoOrchestrator
```

## Subcommands

| Command | Purpose |
| ------- | ------- |
| `auto` | Run all plugins in dependency order via `AutoOrchestrator`; `--plan` shows the plan |
| `extract` | Run selected plugins via `ExecutionEngine` |
| `visualize` | Render a VIM JSON file to HTML, read with strict scalar types |
| `plugins` | List discovered plugins |

## Main Flow of `auto` and `extract`

```mermaid
flowchart TD
    Start["main(args)"] --> Discover["PluginManager.discoverPlugins()"]
    Discover --> Exec["cmd.execute(args)"]
    Exec --> Validate["validate input file"]
    Validate --> Check{"checkPluginsFound()"}
    Check -- "no extractor or parser" --> Err["print searched location, exit 1"]
    Check -- found --> Plan{"auto --plan?"}
    Plan -- yes --> ShowPlan["print plugins location and ExecutionPlan"]
    Plan -- no --> Run["process PDF, write VIM JSON"]
```

Without plugins nothing is written, so a caller never gets an empty VIM document with exit
code 0.

All JSON mappers come from `VimJsonMapper`: `auto` and `extract` write with `writer(indent)`,
`visualize` reads with `strictReader()`, so a scalar of the wrong type fails the load (#192).
