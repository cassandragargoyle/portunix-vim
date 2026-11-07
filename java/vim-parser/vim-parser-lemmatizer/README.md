# vim-parser-lemmatizer

Stemming/lemmatization plugin for VIM Parser with pluggable backend architecture.

## Features

- **Pluggable backend architecture** - Easy to add new NLP backends
- **Multi-language support** - Czech, German, English, Spanish, French, Italian
- **Apache Lucene backend** - Pure Java, no external dependencies
- **Token position tracking** - Maps stems back to source text
- **Statistics** - Token count, unique stems, processing time

## Supported Languages

| Language | Code | Backend | Notes |
| -------- | ---- | ------- | ----- |
| Czech | cs | Lucene CzechAnalyzer | Suffix-stripping stemmer |
| German | de | Lucene GermanAnalyzer | |
| English | en | Lucene EnglishAnalyzer | |
| Spanish | es | Lucene SpanishAnalyzer | |
| French | fr | Lucene FrenchAnalyzer | |
| Italian | it | Lucene ItalianAnalyzer | |

## Architecture

```text
┌─────────────────────────────────────────────────┐
│            Lemmatizer Plugin (Abstract)         │
├─────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │
│  │   Lucene    │  │ MorphoDiTa  │  │ Hunspell │ │
│  │  (v1.0)     │  │  (future)   │  │ (future) │ │
│  └─────────────┘  └─────────────┘  └──────────┘ │
└─────────────────────────────────────────────────┘
```

## Usage

```bash
# Enable stemming in auto mode
portunix vim-parser auto invoice.pdf --stemming

# Specify backend explicitly
portunix vim-parser auto invoice.pdf --stemming --backend=lucene

# Override language detection
portunix vim-parser auto invoice.pdf --stemming --lang=cs
```

## Output

Stems are stored in the VIM JSON semantic layer:

```json
{
  "layers": {
    "semantic": {
      "stemming": {
        "language": "cs",
        "backend": "lucene",
        "tokens": [
          {
            "id": "t001",
            "surface": "Faktura",
            "stem": "faktur",
            "offset": { "start": 0, "end": 7 }
          },
          {
            "id": "t002",
            "surface": "služby",
            "stem": "služb",
            "offset": { "start": 12, "end": 18 }
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

## Known Limitations (Lucene Backend)

Lucene uses **suffix-stripping** stemming, which has known limitations for morphologically rich languages like Czech:

| Case | Example | Limitation |
| ---- | ------- | ---------- |
| Fleeting "e" | pes→psa | Different stems |
| Consonant alternation | kniha→knize | h/z not handled |
| Suppletive forms | člověk→lidé | Completely different stems |
| Dual forms | oko→oči | Not recognized |
| Verb conjugation | platit→platím | Inconsistent stems |

For higher accuracy, consider future backends:

- **MorphoDiTa** - True lemmatization with morphological analysis
- **Hunspell** - Dictionary-based lemmatization

## Configuration

| Parameter | Default | Description |
| --------- | ------- | ----------- |
| Priority | 15 | Runs after fulltext (10) and metadata (5) |
| Auto-enabled | No | Must be enabled with `--stemming` flag |
| Default language | cs | Czech (detected from metadata if available) |

## Dependencies

- vim-parser-api
- Apache Lucene Analysis Common 9.10.0

## Test Coverage

- **15 unit tests** covering:
  - Backend identity and language support
  - Czech noun/adjective stemming accuracy (70%+ threshold)
  - Token offset tracking
  - Processing speed (>10,000 tokens/sec)
  - Sample text processing

## Future Backends

### MorphoDiTa (planned)

True lemmatization with POS tagging for Czech/Slovak:

- URL: <https://ufal.mff.cuni.cz/morphodita>
- Accuracy: ~98% for Czech

### Hunspell (planned)

Dictionary-based lemmatization:

- URL: <https://hunspell.github.io/>
- Requires Czech dictionary
