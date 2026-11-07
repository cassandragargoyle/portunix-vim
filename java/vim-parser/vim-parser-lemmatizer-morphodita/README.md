# vim-parser-lemmatizer-morphodita

MorphoDiTa backend for `vim-parser-lemmatizer`. Provides true dictionary-form lemmatization (not just stemming) for Czech, Slovak, and English using the [MorphoDiTa](https://github.com/ufal/morphodita) morphological analyzer from ÚFAL (Charles University) via JNI.

## Features

- **True lemmatization** — returns dictionary forms, not algorithmic stems (`programování` → `programování`, `běželi` → `běžet`)
- **Morphological tagging** — full POS / morphology tag for each token
- **High accuracy** — ~98 % for Czech (vs. ~70 % for suffix-stripping stemmers)
- Implements the `LemmatizerBackend` interface from `vim-parser-lemmatizer`
- Discovered as a PF4J extension (`@Extension`)

## Backend identification

| Field | Value |
| ----- | ----- |
| Backend ID | `morphodita` |
| Backend name | MorphoDiTa Lemmatizer |
| Plugin ID | `morphodita-backend-plugin` |

## Supported languages

| ISO 639-1 | Language | Notes |
| --------- | -------- | ----- |
| `cs` | Czech | Default model |
| `sk` | Slovak | |
| `en` | English | |

The set of available languages depends on which MorphoDiTa models are installed.

## Prerequisites

1. **Native libraries** — provided by `vim-parser-morpho-morphodita` (built via its `scripts/fetch-natives.sh`)
2. **Language models** — installed via Portunix:

```bash
portunix install morphodita-model-czech
portunix install morphodita-model-slovak
portunix install morphodita-model-english
```

Without an installed model the backend reports the language as unsupported and the orchestrator falls back to another backend (e.g. TK or Lucene).

## Usage

```bash
# Use MorphoDiTa explicitly
portunix vim-parser auto invoice.pdf --stemming --backend=morphodita

# Combined with explicit language
portunix vim-parser auto invoice.pdf --stemming --backend=morphodita --lang=cs
```

## Output

The backend produces the same `StemmedResult` structure consumed by `vim-parser-lemmatizer`, but the `stem` field carries true lemmas (and may include the morphological tag in metadata):

```json
{
  "language": "cs",
  "backend": "morphodita",
  "tokens": [
    {
      "id": "t001",
      "surface": "Faktury",
      "stem": "faktura",
      "tag": "NNFP1-----A----",
      "offset": { "start": 0, "end": 7 }
    },
    {
      "id": "t002",
      "surface": "běželi",
      "stem": "běžet",
      "tag": "VpMP----R-AA---",
      "offset": { "start": 12, "end": 18 }
    }
  ],
  "statistics": {
    "totalTokens": 156,
    "uniqueStems": 89,
    "processingTimeMs": 5
  }
}
```

## When to choose MorphoDiTa over other backends

| Scenario | Recommended backend |
| -------- | ------------------- |
| Highest accuracy for Czech / Slovak / English | **morphodita** |
| 33 languages, no per-language model install | tk |
| Pure-Java fallback, no native code | lucene |

## Availability

`isAvailable()` returns `true` only when:

1. The MorphoDiTa native library can be loaded (`MorphoDiTa.isAvailable()`)
2. At least one language model is installed and loadable

## Dependencies

- vim-parser-api (provided)
- vim-parser-lemmatizer (provided — defines `LemmatizerBackend`)
- vim-parser-morpho-morphodita (JNI wrapper around the native MorphoDiTa library)
- PF4J (provided)

## License

- This adapter: MIT License
- MorphoDiTa library: MPL-2.0
- Language models: CC BY-NC-SA 4.0

## Resources

- [MorphoDiTa GitHub](https://github.com/ufal/morphodita)
- [MorphoDiTa documentation](https://ufal.mff.cuni.cz/morphodita)
- [Language models (LINDAT/CLARIAH-CZ)](https://lindat.mff.cuni.cz/repository/xmlui/handle/11234/1-5457)
