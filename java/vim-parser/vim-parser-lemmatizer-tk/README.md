# vim-parser-lemmatizer-tk

TK Stemmer backend for `vim-parser-lemmatizer`. Provides stemming for 33 languages via the native C++ TK Stemmer library exposed through JNI.

## Features

- **33 supported languages** via the TK Stemmer / Snowball engine
- Implements the `LemmatizerBackend` interface from `vim-parser-lemmatizer`
- Per-language `TKStemmer` instances cached in a `ConcurrentHashMap`
- Unicode-aware tokenization (`\\p{L}+`) with offset tracking
- Discovered as a PF4J extension (`@Extension`)

## Backend identification

| Field | Value |
| ----- | ----- |
| Backend ID | `tk` |
| Backend name | `TK Stemmer (33 languages)` |
| Plugin ID | `tk-backend-plugin` |

## Supported languages

Selected via ISO 639-1 codes; mapped internally to TK language names:

`ar`, `hy`, `eu`, `ca`, `cs`, `da`, `nl`, `en`, `et`, `fi`, `fr`, `de`, `el`, `hi`, `hu`, `id`, `ga`, `it`, `lt`, `ne`, `no`, `pl`, `pt`, `ro`, `ru`, `sr`, `es`, `sv`, `ta`, `tr`, `uk`, `yi`

If an unmapped language is requested, the backend falls back to `english`.

## Usage

```bash
# Use TK backend explicitly
portunix vim-parser auto invoice.pdf --stemming --backend=tk

# Combined with explicit language
portunix vim-parser auto invoice.pdf --stemming --backend=tk --lang=cs
```

## Algorithm

1. Resolve ISO 639-1 input language to a TK language name (`cs` → `czech`)
2. Get or lazily create a `TKStemmer` instance for that language
3. Tokenize input text with the Unicode letters pattern `\\p{L}+`
4. For each token, lowercase and call `stemmer.stem(word)`
5. Emit a `StemmedToken` with id `t001`, `t002`, … plus surface form, stem, and `[start, end]` offset
6. Aggregate into `StemmedResult` and compute statistics (token count, processing time)

## Output

The backend produces the same `StemmedResult` structure consumed by `vim-parser-lemmatizer`:

```json
{
  "language": "cs",
  "backend": "tk",
  "tokens": [
    {
      "id": "t001",
      "surface": "Faktura",
      "stem": "faktur",
      "offset": { "start": 0, "end": 7 }
    }
  ],
  "statistics": {
    "totalTokens": 156,
    "uniqueStems": 89,
    "processingTimeMs": 4
  }
}
```

## Availability

`isAvailable()` returns `true` only when the underlying native TK Stemmer library can be loaded (see `vim-parser-stemmer-tk` for native build instructions). When the native library is missing the backend reports unavailable and the orchestrator falls back to a different backend (e.g. Lucene).

## Dependencies

- vim-parser-api (provided)
- vim-parser-lemmatizer (provided — defines `LemmatizerBackend`)
- vim-parser-stemmer-tk (JNI wrapper around the native TK Stemmer)
- PF4J (provided)
