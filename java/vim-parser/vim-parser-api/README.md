# vim-parser-api

Plugin API interfaces and shared models for VIM Parser.

## Purpose

This module defines the contract between vim-parser-core and all plugins. It contains:

- Plugin interfaces (`ExtractorPlugin`, `ParserPlugin`, `VisualizerPlugin`)
- Shared data models (`VimDocument`, `TextBlock`, `Barcode`, `SpaydPayment`)
- Execution context (`PluginContext`, `PluginResult`)

## Interfaces

### ExtractorPlugin

Extracts raw data from documents (text blocks, barcodes, images).

```java
public interface ExtractorPlugin {
    String getId();
    PluginResult extract(PluginContext context);
}
```

### ParserPlugin

Parses extracted data into semantic entities (SPAYD payments, addresses).

```java
public interface ParserPlugin {
    String getId();
    boolean canParse(String content);
    PluginResult parse(PluginContext context);
}
```

### VisualizerPlugin

Generates output representations (HTML, JSON).

```java
public interface VisualizerPlugin {
    String getId();
    String visualize(VimDocument document);
}
```

## Execution Modes

```java
public enum ExecutionMode {
    SYNC,           // Sequential execution
    ASYNC_THREAD,   // Thread pool execution
    ASYNC_GRPC      // gRPC remote execution (future)
}
```

## Dependencies

- Jackson annotations (JSON serialization)
- No PDFBox, ZXing, or other heavy dependencies
