# VIM Parser - MorphoDiTa JNI Wrapper

Java JNI wrapper for [MorphoDiTa](https://github.com/ufal/morphodita) morphological analyzer from ÚFAL (Charles University).

## Features

- **Lemmatization** - Returns dictionary forms of words (not just stems)
- **POS Tagging** - Full morphological analysis with detailed tags
- **Morphological Analysis** - All possible interpretations for ambiguous words
- **Languages** - Czech, Slovak, English
- **Performance** - 50,000+ words/second

## Requirements

- JDK 21+
- Python 3 (for build scripts)
- Native libraries (downloaded automatically)
- Language models (installed via Portunix)

## Quick Start

### 1. Download Native Libraries

```bash
cd scripts
./fetch-natives.sh
```

### 2. Install Language Models

```bash
portunix install morphodita-model-czech
```

### 3. Use in Java

```java
try (MorphoDiTa md = MorphoDiTa.loadDefault()) {
    // Lemmatize single word
    LemmaResult result = md.lemmatize("programování");
    System.out.println(result.lemma());  // "programování"
    System.out.println(result.tag());    // "NNNS1-----A----"

    // Process text
    List<TaggedToken> tokens = md.process("Faktura za služby");
    for (TaggedToken token : tokens) {
        System.out.printf("%s -> %s (%s)%n",
            token.form(), token.lemma(), token.tag());
    }

    // Get all possible analyses
    List<LemmaResult> analyses = md.analyze("běželi");
    for (LemmaResult a : analyses) {
        System.out.printf("%s: %s%n", a.lemma(), a.tag());
    }
}
```

## API Reference

### MorphoDiTa

Main class for morphological analysis.

| Method | Description |
| ------ | ----------- |
| `loadDefault()` | Load with default Czech model |
| `loadForLanguage(language)` | Load model for specific language |
| `load(modelPath)` | Load from specific model file |
| `lemmatize(word)` | Get lemma for single word |
| `analyze(word)` | Get all possible analyses |
| `process(text)` | Tokenize and tag entire text |
| `isAvailable()` | Check if native library is available |
| `checkForUpdates()` | Check for newer versions on GitHub |

### LemmaResult

Result of lemmatization.

| Field | Description |
| ----- | ----------- |
| `lemma` | Dictionary form (cleaned) |
| `tag` | Morphological tag |
| `rawLemma` | Original lemma with technical suffixes |

### TaggedToken

Token from text processing.

| Field | Description |
| ----- | ----------- |
| `form` | Original word form |
| `lemma` | Dictionary form |
| `tag` | Morphological tag |
| `start` | Start offset in text |
| `end` | End offset in text |

## Build

```bash
# From vim-parser-morphodita directory
mvn clean package

# Skip native fetch if libraries already present
mvn clean package -DskipNativeFetch=true
```

## Version Checking

The library can check for updates:

```java
Optional<UpdateInfo> update = MorphoDiTa.checkForUpdates();
if (update.isPresent()) {
    System.out.println(update.get().getMessage());
}
```

## License

- This wrapper: MIT License
- MorphoDiTa library: MPL-2.0
- Language models: CC BY-NC-SA 4.0

## Resources

- [MorphoDiTa GitHub](https://github.com/ufal/morphodita)
- [MorphoDiTa Documentation](https://ufal.mff.cuni.cz/morphodita)
- [Language Models](https://lindat.mff.cuni.cz/repository/xmlui/handle/11234/1-5457)
