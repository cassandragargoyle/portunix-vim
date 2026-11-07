# vim-parser-api

Rozhraní plugin API a sdílené modely pro VIM Parser.

## Účel

Tento modul definuje kontrakt mezi vim-parser-core a všemi pluginy. Obsahuje:

- Rozhraní pluginů (`ExtractorPlugin`, `ParserPlugin`, `VisualizerPlugin`)
- Sdílené datové modely (`VimDocument`, `TextBlock`, `Barcode`, `SpaydPayment`)
- Kontext provádění (`PluginContext`, `PluginResult`)

## Rozhraní

### ExtractorPlugin

Extrahuje surová data z dokumentů (textové bloky, čárové kódy, obrázky).

```java
public interface ExtractorPlugin {
    String getId();
    PluginResult extract(PluginContext context);
}
```

### ParserPlugin

Parsuje extrahovaná data na sémantické entity (SPAYD platby, adresy).

```java
public interface ParserPlugin {
    String getId();
    boolean canParse(String content);
    PluginResult parse(PluginContext context);
}
```

### VisualizerPlugin

Generuje výstupní reprezentace (HTML, JSON).

```java
public interface VisualizerPlugin {
    String getId();
    String visualize(VimDocument document);
}
```

## Režimy provádění

```java
public enum ExecutionMode {
    SYNC,           // sekvenční provádění
    ASYNC_THREAD,   // provádění v thread poolu
    ASYNC_GRPC      // vzdálené provádění přes gRPC (do budoucna)
}
```

## Závislosti

- Jackson annotations (serializace JSON)
- Žádné těžké závislosti jako PDFBox, ZXing apod.
