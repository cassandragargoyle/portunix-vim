# ADR-002: Modular Architecture for VIM Parser

**Status**: Proposed
**Date**: 2026-01-31
**Origin**: ADR-012 in [portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins/blob/main/docs/adr/012-vim-parser-modular-architecture.md)
**Author**: Architecture Team

---

## Context

Current vim-parser implementation is **monolithic** - all VIM JSON generation functionality is in a single JAR file:

```text
vim-parser.jar (fat JAR ~15MB)
├── TextBlockExtractor (text + clustering)
├── BarcodeExtractor (ZXing - QR, DataMatrix, PDF417, Aztec)
├── SpaydParser (SPAYD payments)
├── VimHtmlVisualizer (HTML output)
└── all dependencies (PDFBox, ZXing, Jackson, Picocli)
```

**Problems with monolith:**

1. **Size** - fat JAR contains all dependencies even when not needed
2. **Flexibility** - cannot add new extractor without rebuilding entire JAR
3. **Scalability** - CPU-intensive operations (barcode detection) block main thread
4. **Isolation** - error in one extractor can crash entire process
5. **Extensibility** - third parties cannot easily add custom extractors

**Requirements for new architecture:**

- Main JAR without direct dependencies on extractors
- Ability to add new extractors as separate JARs
- Three execution modes: sequential, asynchronous (thread), asynchronous (gRPC process)
- Maintain backward compatibility with existing VIM JSON schema

---

## Decision

We introduce a **plugin-based architecture** with these components:

### 1. Module Structure

```text
vim-parser/
├── vim-parser-core/          # Main orchestrator
│   ├── PluginManager         # Discovery and lifecycle
│   ├── ExecutionEngine       # Sequential/async execution
│   ├── VimDocument model     # Shared data structures
│   └── gRPC server/client    # For process isolation
│
├── vim-parser-api/           # Plugin interfaces
│   ├── ExtractorPlugin       # Interface for extractors
│   ├── ParserPlugin          # Interface for parsers
│   ├── PluginContext         # Runtime context
│   └── PluginResult          # Standard output
│
├── vim-parser-fulltext/      # Plugin: Text extraction
│   └── TextBlockExtractor
│
├── vim-parser-barcode/       # Plugin: Barcode detection
│   └── BarcodeExtractor
│
├── vim-parser-spayd/         # Plugin: SPAYD parser
│   └── SpaydParser
│
└── vim-parser-visualizer/    # Plugin: HTML visualizer
    └── VimHtmlVisualizer
```

### 2. Plugin Discovery

Plugins are discovered using **ServiceLoader** (SPI) without direct Maven dependencies:

```java
// vim-parser-api: ExtractorPlugin.java
public interface ExtractorPlugin {
    String getId();                              // "barcode", "text"
    int getPriority();                           // Execution order
    Set<String> getDependencies();               // Dependencies on other plugins
    PluginResult extract(PluginContext ctx);     // Main method
    ExecutionMode[] supportedModes();            // SYNC, ASYNC_THREAD, ASYNC_GRPC
}

// vim-parser-barcode: META-INF/services/org.cg.vim.api.ExtractorPlugin
org.cg.vim.barcode.BarcodeExtractor
```

### 3. Execution Modes

#### A) Sequential (SYNC)

All plugins run in single thread, one after another.

```text
┌─────────────────────────────────────────────────────────────┐
│                     vim-parser-core                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐               │
│  │  Text    │───>│ Barcode  │───>│  SPAYD   │──> VimJSON    │
│  │ Extractor│    │ Extractor│    │  Parser  │               │
│  └──────────┘    └──────────┘    └──────────┘               │
│       ▲               ▲               ▲                     │
│       └───────────────┴───────────────┘                     │
│              Sequential in single thread                    │
└─────────────────────────────────────────────────────────────┘
```

**Sequence diagram:**

```text
Core          TextPlugin      BarcodePlugin    SpaydPlugin
 │                │                │               │
 │──extract()────>│                │               │
 │<──TextBlocks───│                │               │
 │                                 │               │
 │──extract(ctx+TextBlocks)───────>│               │
 │<──Barcodes─────────────────────│               │
 │                                                 │
 │──parse(ctx+Barcodes)───────────────────────────>│
 │<──SpaydPayments────────────────────────────────│
 │                                                 │
 │──buildVimDocument()                             │
 │──return VimJSON                                 │
```

#### B) Asynchronous - Thread Pool (ASYNC_THREAD)

Independent plugins run in parallel in thread pool.

```text
┌─────────────────────────────────────────────────────────────┐
│                     vim-parser-core                         │
│  ┌───────────────────────────────────────────┐              │
│  │            ExecutorService                 │             │
│  │  ┌──────────┐         ┌──────────┐        │              │
│  │  │  Text    │ parallel│ Barcode  │        │  ┌────────┐  │
│  │  │ Extractor│─────────│ Extractor│────────┼─>│ SPAYD  │  │
│  │  └──────────┘         └──────────┘        │  │ Parser │  │
│  │       │                    │              │  └────────┘  │
│  │       └────────┬───────────┘              │      │       │
│  │            merge results                  │      ▼       │
│  └───────────────────────────────────────────┘  VimJSON     │
└─────────────────────────────────────────────────────────────┘
```

**Sequence diagram:**

```text
Core          ThreadPool      TextPlugin      BarcodePlugin    SpaydPlugin
 │                │               │               │               │
 │──submit(text)─>│               │               │               │
 │──submit(barcode)>              │               │               │
 │                │──extract()───>│               │               │
 │                │──extract()────────────────────>│               │
 │                │               │               │               │
 │                │<──TextBlocks──│               │               │
 │                │<──Barcodes────────────────────│               │
 │                │               │               │               │
 │<──Future.all()─│               │               │               │
 │                                                                │
 │──parse(merged context)────────────────────────────────────────>│
 │<──SpaydPayments───────────────────────────────────────────────│
 │                                                                │
 │──return VimJSON                                                │
```

#### C) Asynchronous - gRPC Process (ASYNC_GRPC)

Plugins run as separate processes, communication via gRPC.

```text
┌─────────────────────────────────────────────────────────────┐
│                     vim-parser-core                         │
│  ┌──────────────────────────────────────────────┐           │
│  │              gRPC Client Manager             │           │
│  └──────────────────────────────────────────────┘           │
│          │              │              │                    │
│          ▼              ▼              ▼                    │
└──────────┼──────────────┼──────────────┼────────────────────┘
           │              │              │
     ┌─────┴─────┐  ┌─────┴─────┐  ┌─────┴─────┐
     │  Process  │  │  Process  │  │  Process  │
     │  :50051   │  │  :50052   │  │  :50053   │
     │ ┌───────┐ │  │ ┌───────┐ │  │ ┌───────┐ │
     │ │ Text  │ │  │ │Barcode│ │  │ │ SPAYD │ │
     │ │Plugin │ │  │ │Plugin │ │  │ │Plugin │ │
     │ └───────┘ │  │ └───────┘ │  │ └───────┘ │
     └───────────┘  └───────────┘  └───────────┘
          gRPC           gRPC           gRPC
```

**Sequence diagram:**

```text
Core          gRPCManager     TextProcess     BarcodeProcess   SpaydProcess
 │                │               │               │               │
 │──startProcess()>               │               │               │
 │                │──spawn───────>│               │               │
 │                │──spawn───────────────────────>│               │
 │                │──spawn───────────────────────────────────────>│
 │                │               │               │               │
 │                │<──ready(:50051)│              │               │
 │                │<──ready(:50052)───────────────│               │
 │                │<──ready(:50053)───────────────────────────────│
 │                │               │               │               │
 │──extract(pdf)─>│               │               │               │
 │                │──gRPC:extract()>              │               │
 │                │──gRPC:extract()──────────────>│               │
 │                │               │               │               │
 │                │<──TextBlocks──│               │               │
 │                │<──Barcodes────────────────────│               │
 │                │               │               │               │
 │                │──gRPC:parse(barcodes)────────────────────────>│
 │                │<──SpaydPayments───────────────────────────────│
 │                │               │               │               │
 │<──VimJSON──────│               │               │               │
 │                │               │               │               │
 │──shutdown()───>│               │               │               │
 │                │──terminate───>│               │               │
 │                │──terminate───────────────────>│               │
 │                │──terminate───────────────────────────────────>│
```

### 4. gRPC Service Definition

```protobuf
// vim-parser-api/src/main/proto/plugin.proto

syntax = "proto3";
package org.cg.vim.grpc;

service ExtractorService {
    rpc Extract(ExtractRequest) returns (ExtractResponse);
    rpc GetInfo(Empty) returns (PluginInfo);
    rpc Shutdown(Empty) returns (Empty);
}

message ExtractRequest {
    bytes pdf_content = 1;           // PDF as byte array
    int32 page_number = 2;           // -1 for all pages
    map<string, string> options = 3; // Plugin-specific options
    bytes context = 4;               // Serialized PluginContext
}

message ExtractResponse {
    bool success = 1;
    string error_message = 2;
    bytes result = 3;                // Serialized PluginResult
    map<string, string> metadata = 4;
}

message PluginInfo {
    string id = 1;
    string version = 2;
    repeated string dependencies = 3;
    repeated ExecutionMode supported_modes = 4;
}

enum ExecutionMode {
    SYNC = 0;
    ASYNC_THREAD = 1;
    ASYNC_GRPC = 2;
}
```

### 5. Execution Configuration

```json
{
  "execution": {
    "defaultMode": "ASYNC_THREAD",
    "threadPoolSize": 4,
    "grpc": {
      "enabled": true,
      "portRange": "50051-50060",
      "processTimeoutMs": 30000
    }
  },
  "plugins": {
    "text": {
      "enabled": true,
      "mode": "SYNC",
      "priority": 10
    },
    "barcode": {
      "enabled": true,
      "mode": "ASYNC_GRPC",
      "priority": 20,
      "options": {
        "formats": ["QR_CODE", "DATA_MATRIX", "PDF_417", "AZTEC"],
        "dpi": 150
      }
    },
    "spayd": {
      "enabled": true,
      "mode": "SYNC",
      "priority": 30,
      "dependsOn": ["barcode"]
    }
  }
}
```

### 6. CLI Interface

```bash
# Sequential (default for simple cases)
vim-parser extract --mode=sync input.pdf -o output.json

# Asynchronous thread pool
vim-parser extract --mode=thread --threads=4 input.pdf -o output.json

# Asynchronous gRPC processes
vim-parser extract --mode=grpc input.pdf -o output.json

# Selective plugins
vim-parser extract --plugins=text,barcode input.pdf -o output.json

# Batch processing with gRPC (plugins stay running)
vim-parser extract --mode=grpc --keep-alive *.pdf -o output/
```

---

## Consequences

### Positive

| Aspect | Benefit |
| ------ | ------- |
| **Modularity** | Plugins can be added/removed without rebuilding core |
| **Scalability** | CPU-intensive tasks can be distributed |
| **Isolation** | Plugin crash doesn't bring down entire process (gRPC mode) |
| **Testability** | Each plugin can be tested in isolation |
| **Extensibility** | Third parties can write custom plugins |
| **Size** | Base JAR will be smaller (~2MB vs ~15MB) |
| **Flexibility** | Different modes for different use-cases |

### Negative

| Aspect | Trade-off | Mitigation |
| ------ | --------- | ---------- |
| **Complexity** | More JAR files, more complex build | Maven multi-module project |
| **gRPC Latency** | Serialization and IPC overhead | Use only for CPU-intensive tasks |
| **Debugging** | Harder to trace across processes | Structured logging, correlation ID |
| **Deployment** | More artifacts to distribute | Assembly plugin, distribution ZIP |
| **Memory** | Each gRPC process has its own JVM | Pool management, lazy start |

### Risks

| Risk | Probability | Impact | Mitigation |
| ---- | ----------- | ------ | ---------- |
| Plugin version mismatch | Medium | High | API versioning, compatibility checks |
| gRPC port conflicts | Low | Medium | Dynamic port allocation |
| Plugin discovery failures | Low | High | Graceful degradation, logging |
| Performance regression | Medium | Medium | Benchmarking, mode selection |

---

## Alternatives Considered

### 1. OSGi Framework

**Rejected** - too complex for given use-case, steep learning curve.

### 2. Thread Pool Only (without gRPC)

**Partially accepted** - thread pool as default, gRPC for special cases (isolation, distribution).

### 3. Message Queue (RabbitMQ/Kafka)

**Rejected** - unnecessary infrastructure dependency for local processing.

### 4. Shared Memory / Memory-mapped files

**Rejected** - platform-specific, complex synchronization.

---

## Implementation Milestones

1. **Phase 1**: Create vim-parser-api module with interfaces
2. **Phase 2**: Refactor to multi-module Maven project
3. **Phase 3**: Implement ServiceLoader discovery
4. **Phase 4**: Thread pool executor
5. **Phase 5**: gRPC communication
6. **Phase 6**: CLI integration and configuration

---

## References

- [Java ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
- [gRPC Java](https://grpc.io/docs/languages/java/)
- [Maven Multi-Module](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
