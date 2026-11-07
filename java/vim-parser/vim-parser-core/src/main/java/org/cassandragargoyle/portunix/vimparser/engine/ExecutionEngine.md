---
warning: Only the code is the source of truth. This document is for humans and may lag behind the code.
---

# ExecutionEngine

Runs the extractor and parser plugins over one PDF and builds the `VimDocument` from the
results the plugins left in the shared data of `PluginContext`.

The engine has two entry points. `process` runs the plugins itself and then builds the
document. `buildVimDocumentFromContext` only builds the document and is used by
`AutoOrchestrator`, which runs the plugins on its own.

## Structure

```mermaid
classDiagram
    class ExecutionEngine {
        -PluginManager pluginManager
        -ExecutionMode defaultMode
        -int threadPoolSize
        -ExecutorService executor
        -TelemetryProvider telemetryProvider
        +process(byte[] pdfContent, String sourceName, boolean verbose, Set enabledPlugins) VimDocument
        +buildVimDocumentFromContext(VimDocument vimDoc, PluginContext context) void
        +shutdown() void
        -runExtractorsSync(List extractors, PluginContext context) void
        -runExtractorsAsync(List extractors, PluginContext context) void
        -runParsersSync(List parsers, PluginContext context) void
        -buildVimDocument(VimDocument vimDoc, PluginContext context) void
        -extractSemanticEntities(VimDocument vimDoc, List blocks) void
        -toBoundingBox(Object bbox) BoundingBox
        -calculateDistance(BoundingBox a, BoundingBox b) float
    }
    ExecutionEngine --> PluginManager : gets plugins
    ExecutionEngine --> PluginContext : reads shared data
    ExecutionEngine --> VimDocument : fills
    AutoOrchestrator --> ExecutionEngine : buildVimDocumentFromContext
    Main --> ExecutionEngine : process
```

## Processing Flow

```mermaid
sequenceDiagram
    participant Caller
    participant Engine as ExecutionEngine
    participant PM as PluginManager
    participant Plugins
    participant Ctx as PluginContext

    Caller->>Engine: process(pdfContent, sourceName, verbose, enabledPlugins)
    Engine->>PM: getExtractorPlugins, getParserPlugins
    alt SYNC
        Engine->>Plugins: extract, one by one
    else ASYNC_THREAD or ASYNC_GRPC
        Engine->>Plugins: extract, independent ones in parallel
        Engine->>Plugins: extract, dependent ones one by one
    end
    Plugins->>Ctx: putSharedData
    Engine->>Plugins: parse, one by one
    Plugins->>Ctx: putSharedData
    Engine->>Ctx: getSharedData
    Engine-->>Caller: VimDocument
```

`ASYNC_GRPC` is not implemented and falls back to the thread mode. A plugin whose dependency
did not succeed is skipped. A plugin that throws is logged and recorded as a failed result,
the run continues.

## Shared Data Mapping

`buildVimDocument` reads these keys. A missing key is skipped.

| Shared data key    | Target in `VimDocument`                               | Version set |
| ------------------ | ----------------------------------------------------- | ----------- |
| `textBlocks`       | `layers.physical.blocks`, `layers.structural.blocks`  |             |
| `pageCount`        | `document.pages`                                      |             |
| `barcodes`         | `layers.physical.barcodes`, `layers.structural`       | 1.1         |
| `spaydPayments`    | `layers.semantic.payments`                            |             |
| `tables`           | `layers.physical.tables`                              |             |
| `textBlocks`       | `layers.semantic.entities`, by header keywords        |             |
| `documentMetadata` | `document.metadata`                                   | 1.2         |
| `fulltextContent`  | `layers.physical.fulltext`                            | 1.2         |
| `stemmingResult`   | `layers.semantic.stemming`                            | 1.3         |
| `toc`              | `layers.toc`, marks structural blocks of the TOC page | 1.4         |

## Table Conversion

The core has no compile dependency on the table plugin, so the tables are read by reflection.
The table plugin returns its own `BoundingBox` class from `table-detector-api`. It has the
same fields as the core class but is a different type, so `toBoundingBox` copies the values
instead of casting.

```mermaid
flowchart TD
    Tables["shared data: tables"] --> Loop["for each table"]
    Loop --> Props["read id, page, rows, columns, confidence"]
    Props --> TableBbox["getBoundingBox()"]
    TableBbox --> Convert["toBoundingBox"]
    Convert --> Cells["for each cell: row, column, text, header, bbox"]
    Cells --> Convert
    Cells --> Add["layers.physical.tables"]
    Props -. exception .-> Skip["log warning, skip this table"]
    Convert -. exception .-> Skip
    Skip --> Loop
    Add --> Loop
```

```mermaid
flowchart LR
    Input["bbox object"] --> Same{"core BoundingBox?"}
    Same -- yes --> Return["returned as is"]
    Same -- no --> Reflect["getX, getY, getWidth, getHeight by reflection"]
    Reflect --> New["new core BoundingBox"]
```

A table that cannot be converted is skipped with the warning `Error converting table` and
does not stop the other tables.
