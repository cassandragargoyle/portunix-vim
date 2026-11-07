---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# PluginManager

Discovers vim-parser plugins with PF4J and keeps them by type: extractors, parsers and
visualizers. It also tells the CLI where plugins were looked for, so that a run without
plugins can fail with a clear message instead of writing an empty document.

## Structure

```mermaid
classDiagram
    class PluginManager {
        ~String PLUGINS_DIR_NAME
        -Path pluginsDir
        -boolean developmentMode
        -org.pf4j.PluginManager pf4jManager
        +PluginManager()
        +PluginManager(Path pluginsDir)
        +discoverPlugins()
        ~defaultPluginsDir(Path codeSource)$ Path
        +getPluginsDir() Path
        +isDevelopment() boolean
        +describePluginSource() String
        +hasExtractionPlugins() boolean
        +getExtractorPlugins() List
        +getParserPlugins() List
        +getVisualizerPlugins() List
        +shutdown()
    }
    PluginManager --> "*" ExtractorPlugin
    PluginManager --> "*" ParserPlugin
    PluginManager --> "*" VisualizerPlugin
    PluginManager --> DefaultPluginManager : PF4J
```

## Default Plugins Directory

The no-argument constructor resolves the plugins directory from the location of the
running code, so an installed JAR finds its plugins from any working directory.

```mermaid
flowchart TD
    Start["new PluginManager()"] --> Src["code source of PluginManager"]
    Src --> IsJar{"regular file (JAR)?"}
    IsJar -- no --> Cwd["plugins (relative to working directory)"]
    IsJar -- yes --> Beside{"&lt;jar dir&gt;/plugins is a directory?"}
    Beside -- yes --> Jar["&lt;jar dir&gt;/plugins"]
    Beside -- no --> Cwd
```

A classes directory (IDE, unit tests) keeps the relative `plugins` directory.

## Discovery

```mermaid
sequenceDiagram
    participant Main
    participant PM as PluginManager
    participant PF4J as DefaultPluginManager
    Main->>PM: discoverPlugins()
    PM->>PM: isDevelopmentMode()
    alt plugins directory missing or empty
        PM->>PF4J: classpath mode (isDevelopment = true)
    else plugins directory has content
        PM->>PF4J: DefaultPluginManager(pluginsDir)
    end
    PM->>PF4J: loadPlugins(), startPlugins()
    PM->>PF4J: getExtensions(Extractor / Parser / Visualizer)
    Main->>PM: hasExtractionPlugins()
    alt no extractor and no parser
        Main->>PM: describePluginSource()
        Main-->>Main: print error, exit 1
    end
```

`describePluginSource()` returns the absolute plugins directory, or a classpath note naming
that directory in development mode.
