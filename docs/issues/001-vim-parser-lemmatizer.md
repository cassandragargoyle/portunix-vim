# VIM Parser - Lemmatizer Plugin

**Issue**: #1
**Origin**: [portunix-plugins#32](https://github.com/cassandragargoyle/portunix-plugins/issues/32)
**Status**: 🚧 In Progress
**Priority**: High
**Type**: Feature
**Labels**: feature, plugin, vim-parser, stemming, nlp, lucene, czech
**Related**: Issue portunix-plugins#24, Issue portunix-plugins#30

## Summary

Implement a **general-purpose lemmatizer plugin** for VIM Parser with pluggable backend architecture. The plugin provides stemming/lemmatization for extracted text, storing results in the VIM JSON semantic layer. First implementation will use **Apache Lucene Czech Stemmer** (simple, pure Java, no external dependencies). Future backends (MorphoDiTa, Hunspell) can provide higher accuracy.

## Background / Context

### Why Lemmatization?

Lemmatization is essential for NLP tasks on morphologically rich languages:

- **Search improvement**: Finding "faktura" also matches "faktury", "faktuře", "fakturách"
- **Keyword extraction**: Normalized terms improve semantic analysis
- **Entity recognition**: Better identification of named entities
- **Text analytics**: Word frequency analysis on lemmas instead of surface forms
- **Vector embeddings**: Better input for downstream ML models

### Plugin Architecture Philosophy

The lemmatizer plugin follows **provider pattern**:

```text
┌─────────────────────────────────────────────────────┐
│            Lemmatizer Plugin (Abstract)             │
├─────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │   Lucene    │  │ MorphoDiTa  │  │  Hunspell   │  │
│  │  (v1.0)     │  │  (future)   │  │  (future)   │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  │
│    CS (stem)        CS/SK (lemma)    CS (lemma)     │
└─────────────────────────────────────────────────────┘
```

This allows:

- Adding new backends without changing core plugin
- Language-specific optimizations
- Fallback chains (if one backend fails, try another)

### First Backend: Apache Lucene Czech Stemmer

Apache Lucene provides a simple Czech stemmer out of the box:

- **URL**: https://lucene.apache.org/
- **License**: Apache 2.0
- **Languages**: Czech (stemming)
- **Features**: Suffix stripping, stop words removal
- **Why first**: Pure Java, no external dependencies, already in project (via vim-parser-fulltext)

**Limitations**:

- Stemming ≠ lemmatization ("fakturách" → "faktur", not "faktura")
- No POS tagging
- No morphological features
- Sufficient for search/indexing, not for linguistic analysis

### Future Backends

#### MorphoDiTa (higher accuracy)

- **URL**: https://ufal.mff.cuni.cz/morphodita
- **Languages**: Czech, Slovak, English
- **Features**: True lemmatization, POS tagging, morphological analysis
- **Integration**: REST API or JNI bindings

#### Hunspell (alternative)

- **URL**: https://hunspell.github.io/
- **Languages**: Czech (via dictionary)
- **Features**: True lemmatization via morphological dictionary

### Current VIM Parser State

VIM Parser currently extracts:

- Physical layer: text blocks, barcodes, tables, images
- Structural layer: labeled blocks
- Semantic layer: extracted entities, structured data

Missing: linguistic analysis (lemmas, POS tags, morphological features)

## Requirements

### Functional Requirements

#### Core Plugin (Abstract)

1. **FR-01**: Pluggable backend architecture (provider pattern)
2. **FR-02**: Stem/lemmatize all text content from fulltext extractor output
3. **FR-03**: Store stemmed tokens in VIM JSON semantic layer
4. **FR-04**: Preserve token-to-source mapping (position in original text)
5. **FR-05**: Allow manual language/backend override via CLI option
6. **FR-06**: Backend discovery and registration mechanism

#### Lucene Czech Stemmer Backend (v1.0)

7. **FR-07**: Support Czech language stemming
8. **FR-08**: Use Lucene CzechStemmer for word normalization
9. **FR-09**: Integrate with existing Lucene dependency

#### Future Backends (optional)

10. **FR-10**: MorphoDiTa backend with POS tagging and morphological features
11. **FR-11**: Hunspell backend for true lemmatization

### Non-Functional Requirements

1. **NFR-01**: Processing speed > 10000 tokens/second (Lucene stemmer is fast)
2. **NFR-02**: Memory usage < 50MB (no large models for Lucene backend)
3. **NFR-03**: Offline operation (no external API dependencies)
4. **NFR-04**: No additional dependencies for Lucene backend (already in project)
5. **NFR-05**: Backend interface must be stable for future implementations

## Technical Approach

### Architecture Decision Record (ADR)

#### Context

We need a general-purpose lemmatizer plugin for VIM Parser that:

- Supports multiple NLP backends (MorphoDiTa, UDPipe, Stanza, etc.)
- Can be extended without modifying core plugin code
- Provides consistent output format regardless of backend used

#### Decision

Implement **provider pattern** architecture:

1. Abstract `LemmatizerBackend` interface defining contract
2. `BackendRegistry` for discovery and selection
3. Unified output format (`StemmedResult`)
4. **Apache Lucene Czech Stemmer** as first backend (simplest, pure Java)

Future backends (MorphoDiTa, Hunspell) can be added when higher accuracy is needed.

#### Consequences

- **Positive**: Fast implementation, no new dependencies, extensible for future
- **Negative**: Stemming is less accurate than true lemmatization
- **Mitigation**: Backend interface allows easy upgrade to MorphoDiTa later

### Plugin Architecture

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                   VIM Parser Text Processing Pipeline                    │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────┐   ┌─────────────┐   ┌───────────────┐   ┌───────────┐   │
│  │  metadata   │──►│  fulltext   │──►│  lemmatizer   │──►│ (future)  │   │
│  │  (pri:10)   │   │  (pri:11)   │   │   (pri:15)    │   │ keywords  │   │
│  └─────────────┘   └─────────────┘   └───────────────┘   └───────────┘   │
│                           │                  │                           │
│                    FulltextContent    LemmatizedContent                  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                    Lemmatizer Plugin Architecture                        │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────┐    ┌──────────────────┐    ┌────────────────────┐   │
│  │  PluginContext  │    │ LemmatizerPlugin │    │  BackendRegistry   │   │
│  │  (fulltext)     │───►│  (orchestrator)  │───►│                    │   │
│  └─────────────────┘    └──────────────────┘    └────────────────────┘   │
│                                │                         │               │
│                                ▼                         ▼               │
│                   ┌───────────────────────┐    ┌────────────────────┐    │
│                   │  LemmatizerBackend    │◄───│ Available Backends │    │
│                   │  <<interface>>        │    ├────────────────────┤    │
│                   ├───────────────────────┤    │ • Lucene (v1.0)    │    │
│                   │ + stem(text, lang)    │    │ • MorphoDiTa (fut) │    │
│                   │ + getSupportedLangs() │    │ • Hunspell (fut)   │    │
│                   │ + getBackendId()      │    └────────────────────┘    │
│                   │ + isAvailable()       │                              │
│                   └───────────────────────┘                              │
│                                │                                         │
│                                ▼                                         │
│                   ┌───────────────────────┐                              │
│                   │   LemmatizedResult    │                              │
│                   │   - tokens[]          │                              │
│                   │   - language          │                              │
│                   │   - backendId         │                              │
│                   │   - statistics        │                              │
│                   └───────────────────────┘                              │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Backend Interface

```java
public interface LemmatizerBackend {
    // Identity
    String getBackendId();                    // "lucene", "morphodita", etc.
    String getBackendName();                  // Human-readable name

    // Capabilities
    Set<String> getSupportedLanguages();      // "cs", "sk", "en", etc.
    boolean supportsLanguage(String lang);

    // Lifecycle
    boolean isAvailable();                    // Check if backend is ready

    // Core operation
    StemmedResult stem(String text, String language);
}
```

### Lucene Czech Stemmer Usage

```java
import org.apache.lucene.analysis.cz.CzechStemmer;

// Simple stemming
CzechStemmer stemmer = new CzechStemmer();
char[] word = "fakturách".toCharArray();
int len = stemmer.stem(word, word.length);
String stem = new String(word, 0, len);  // "faktur"

// With analyzer (tokenization + stemming)
import org.apache.lucene.analysis.cz.CzechAnalyzer;

CzechAnalyzer analyzer = new CzechAnalyzer();
TokenStream stream = analyzer.tokenStream("text", "Faktura za služby");
// Produces: "faktur", "služb" (stems, not lemmas)
```

## VIM JSON Schema Extension

### New Semantic Layer Content

```json
{
  "vim_version": "1.3",
  "document": {
    "source": "invoice.pdf",
    "pages": 2,
    "metadata": {}
  },
  "layers": {
    "physical": {},
    "structural": {},
    "semantic": {
      "stemming": {
        "language": "cs",
        "backend": "lucene",
        "tokens": [
          {
            "id": "t001",
            "surface": "Faktura",
            "stem": "faktur",
            "offset": {
              "start": 0,
              "end": 7
            },
            "page": 1,
            "blockId": "b001"
          },
          {
            "id": "t002",
            "surface": "služby",
            "stem": "služb",
            "offset": {
              "start": 12,
              "end": 18
            },
            "page": 1,
            "blockId": "b001"
          }
        ],
        "statistics": {
          "totalTokens": 156,
          "uniqueStems": 89,
          "processingTimeMs": 12
        }
      }
    }
  }
}
```

**Note**: Lucene backend produces stems (suffix-stripped), not true lemmas. Future backends (MorphoDiTa) can add `pos`, `tag`, and `features` fields.

## Module Structure

```text
vim-parser-lemmatizer/
├── pom.xml
├── src/main/java/org/cassandragargoyle/portunix/vimparser/lemmatizer/
│   ├── LemmatizerPlugin.java           # PF4J @Extension (orchestrator)
│   ├── LemmatizerPf4jPlugin.java       # PF4J Plugin class
│   ├── api/
│   │   ├── LemmatizerBackend.java      # Backend interface
│   │   └── BackendRegistry.java        # Backend discovery & selection
│   ├── backend/
│   │   └── lucene/                     # Lucene backend (v1.0)
│   │       └── LuceneCzechBackend.java # CzechStemmer wrapper
│   ├── model/
│   │   ├── StemmedToken.java           # Token with stem
│   │   └── StemmedResult.java          # Unified result (backend-agnostic)
│   └── util/
│       └── Tokenizer.java              # Simple text tokenization
└── src/test/java/
    └── ...
```

## CLI Commands

```bash
# Basic stemming (default: Lucene Czech backend)
portunix vim-parser auto invoice.pdf --stemming

# Specify backend explicitly
portunix vim-parser auto invoice.pdf --stemming --backend=lucene

# Stemming statistics only
portunix vim-parser auto invoice.pdf --stemming --stats-only

# List available backends
portunix vim-parser stemming --list-backends
```

## Implementation Plan

### Phase 1: Core Plugin + Lucene Backend

1. Create `vim-parser-lemmatizer` module
2. Define `LemmatizerBackend` interface
3. Implement `BackendRegistry` with discovery mechanism
4. Implement `LuceneCzechBackend` using CzechStemmer
5. Implement `LemmatizerPlugin` orchestrator
6. Add VIM JSON schema extension for stemming
7. CLI integration (`--stemming` flag)
8. Basic tests

### Phase 2: Feature Complete

1. Token-to-block mapping (offset tracking)
2. Statistics generation
3. Add `--backend` CLI option
4. Full test coverage

### Future Phases (optional)

- **MorphoDiTa Backend**: True lemmatization with POS tags
- **Hunspell Backend**: Alternative lemmatization
- **Slovak Support**: Additional language models

## Dependencies

### Maven Dependencies (v1.0 - Lucene)

```xml
<!-- Lucene Czech Analyzer (already in project via vim-parser-fulltext) -->
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-analysis-common</artifactId>
    <version>9.10.0</version>
</dependency>
```

**Note**: No additional dependencies required - Lucene is already used in vim-parser-fulltext module.

### Future Dependencies (MorphoDiTa Backend)

When implementing MorphoDiTa backend for higher accuracy:

```xml
<!-- Option A: REST API -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.3</version>
</dependency>

<!-- Option B: JNI bindings (requires native libs) -->
<dependency>
    <groupId>cz.cuni.mff.ufal.morphodita</groupId>
    <artifactId>morphodita-java</artifactId>
    <version>1.11.2</version>
</dependency>
```

| Language | Model | Size | URL |
| -------- | ----- | ---- | --- |
| Czech | czech-morfflex2.0-pdtc1.0-220710 | ~50MB | lindat.mff.cuni.cz |
| Slovak | slovak-morfflex1.0-240201 | ~30MB | lindat.mff.cuni.cz |

## Acceptance Criteria

### Core Plugin

- [ ] **AC-01**: Plugin loads and registers with VIM Parser
- [ ] **AC-02**: BackendRegistry discovers and registers available backends
- [ ] **AC-03**: `--list-backends` CLI command works
- [ ] **AC-04**: `--backend=<id>` CLI option selects specific backend
- [ ] **AC-05**: VIM JSON includes `semantic.stemming` section with backend info

### Lucene Czech Backend

- [ ] **AC-06**: Lucene backend registers with BackendRegistry
- [ ] **AC-07**: Stems Czech text from fulltext extractor
- [ ] **AC-08**: Output contains correct stems for test documents
- [ ] **AC-09**: Token positions map correctly to source text
- [ ] **AC-10**: Processing speed meets NFR-01 (>10000 tokens/sec)
- [ ] **AC-11**: `--stemming` CLI flag works
- [ ] **AC-12**: Statistics are accurate

## Test Cases

### Unit Tests

1. **TC-01**: CzechStemmer produces expected stems
2. **TC-02**: Token offset calculation
3. **TC-03**: Backend registry discovery

### Integration Tests

1. **TC-04**: Full pipeline with Czech invoice PDF
2. **TC-05**: Large document performance test

### Sample Test Input

```text
Faktura č. 2024001
Dodavatel: ABC s.r.o.
Odběratel: XYZ a.s.
Celková částka: 12 500 Kč
```

Expected stems (Lucene): `faktur, dodavatel, odběratel, celkov, částk`

**Note**: Stems are suffix-stripped, not true lemmas. "fakturách" → "faktur" (not "faktura").

## Risks

| Risk | Probability | Impact | Mitigation |
| ---- | ----------- | ------ | ---------- |
| Stemming accuracy insufficient | Medium | Low | Add MorphoDiTa backend later |
| Lucene version conflicts | Low | Medium | Use same version as vim-parser-fulltext |

## Related Issues

- **Issue portunix-plugins#24**: VIM Parser - Text Block Identification (provides input)
- **Issue portunix-plugins#30**: VIM Parser - Document Metadata and Fulltext Integration (provides fulltext)
- **Issue portunix-plugins#15**: Fulltext Plugin - Lucene Engine (potential consumer of lemmas)

## Resources

### Lucene (v1.0)

- [Apache Lucene](https://lucene.apache.org/)
- [Lucene CzechAnalyzer JavaDoc](https://lucene.apache.org/core/9_10_0/analysis/common/org/apache/lucene/analysis/cz/CzechAnalyzer.html)
- [Lucene CzechStemmer Source](https://github.com/apache/lucene/blob/main/lucene/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechStemmer.java)

### MorphoDiTa (future)

- [MorphoDiTa Documentation](https://ufal.mff.cuni.cz/morphodita)
- [MorphoDiTa REST API](https://lindat.mff.cuni.cz/services/morphodita/api-reference.php)
- [Czech Morphological Tagset](https://ufal.mff.cuni.cz/pdt/Morphology_and_Tagging/Doc/PDT-TAGS-2.1.pdf)
