# VIM Parser - MorphoDiTa Lemmatizer Backend Plugin

**Issue**: #2
**Origin**: [portunix-plugins#36](https://github.com/cassandragargoyle/portunix-plugins/issues/36)
**Status**: 📋 Open
**Priority**: High
**Type**: Feature
**Labels**: feature, plugin, vim-parser, pf4j, morphology, lemmatizer, morphodita, backend
**Related**: Issue #1, Issue portunix-plugins#35
**Blocked By**: Issue portunix-plugins#35

## Summary

Create a **PF4J plugin** that integrates the MorphoDiTa JNI library (Issue portunix-plugins#35) into vim-parser-lemmatizer as an additional `LemmatizerBackend`. This plugin provides true lemmatization (dictionary forms) with full morphological analysis for Czech, Slovak, and English, using the state-of-the-art MorphoDiTa analyzer from ÚFAL.

## Background / Context

### Architecture Overview

The vim-parser-lemmatizer (Issue #1) uses a **pluggable backend architecture**:

```text
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                    vim-parser-lemmatizer Plugin System                              │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  vim-parser-lemmatizer/                     BackendRegistry                         │
│  ┌─────────────────────────────────────┐   ┌─────────────────────────┐              │
│  │  LemmatizerPlugin (@Extension)      │──►│  register(backend)      │              │
│  │  - discovers backends               │   │  getBackendForLanguage()│              │
│  │  - orchestrates processing          │   │  getAllBackends()       │              │
│  └─────────────────────────────────────┘   └─────────────────────────┘              │
│                                                      │                              │
│                          ┌───────────────────────────┴───────────────┐              │
│                          │                     │                     │              │
│                    ┌─────▼─────┐        ┌──────▼────────┐       ┌──────▼──────┐     │ 
│                    │  Lucene   │        │   TK       │       │ MorphoDiTa  │     │
│                    │  Backend  │        │   Backend     │       │   Backend   │     │
│                    │  (built-in)│       │ (plugins#34)│       │   (this)    │     │
│                    │  6 langs  │        │  33 langs     │       │  3 langs    │     │
│                    └───────────┘        └───────────────┘       └─────────────┘     │
│                     Pure Java            JNI (stemming)     JNI (lemmatization)     │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### MorphoDiTa vs Other Backends

| Feature | MorphoDiTa (this) | TK Stemmer | Lucene |
| ------- | ----------------- | ------------- | ------ |
| Operation | True lemmatization | Stemming | Stemming |
| Output | Lemma + POS tag | Stem only | Stem only |
| Languages | 3 (cs, sk, en) | 33 | 6 |
| Czech quality | Excellent (MorfFlex2) | Good (Snowball) | Basic |
| Morphological info | Full POS tags | None | None |
| Models | Required (~50MB) | None | None |

### Why MorphoDiTa for Czech?

For Czech language processing, MorphoDiTa provides significant advantages:

1. **True lemmas** - returns dictionary form (e.g., "běželi" → "běžet"), not just stem
2. **Morphological tags** - provides POS, gender, number, case, tense information
3. **MorfFlex2 dictionary** - 1.6M lemmas, comprehensive Czech morphology
4. **Disambiguation** - chooses correct lemma based on context
5. **95.89% accuracy** - state-of-the-art for Czech

### Relationship to Other Issues

- **Issue #1**: Defines `LemmatizerBackend` interface and `BackendRegistry`
- **Issue portunix-plugins#35**: Provides `MorphoDiTa` JNI library (standalone)
- **Issue #2** (this): Thin adapter connecting portunix-plugins#35 to #1
- **Issue portunix-plugins#33/#34**: TK Stemmer - alternative backend for more languages

## Requirements

### Functional Requirements

1. **FR-01**: Implement `LemmatizerBackend` interface using MorphoDiTa JNI
2. **FR-02**: Support Czech, Slovak, and English languages
3. **FR-03**: Register automatically with `BackendRegistry` on plugin load
4. **FR-04**: Map ISO 639-1 language codes to MorphoDiTa model names
5. **FR-05**: Return both lemma and morphological tag in results
6. **FR-06**: Handle model loading failures gracefully
7. **FR-07**: Report `isAvailable() = false` if models not installed

### Non-Functional Requirements

1. **NFR-01**: Minimal adapter code (delegate to MorphoDiTa JNI)
2. **NFR-02**: No additional dependencies beyond morphodita-jni
3. **NFR-03**: Plugin ZIP size < 50KB (models via Portunix)
4. **NFR-04**: Backend ID: `"morphodita"`
5. **NFR-05**: Higher priority than Lucene/TK for Czech (better quality)

## Technical Design

### Plugin Architecture

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                 vim-parser-lemmatizer-morphodita Plugin                  │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  MorphoDiTaPf4jPlugin extends Plugin                               │  │
│  │  - PF4J lifecycle management                                       │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  MorphoDiTaBackend implements LemmatizerBackend                    │  │
│  │  @Extension                                                        │  │
│  ├────────────────────────────────────────────────────────────────────┤  │
│  │  - backendId = "morphodita"                                        │  │
│  │  - backendName = "MorphoDiTa Lemmatizer (cs, sk, en)"              │  │
│  │  - supportedLanguages = {cs, sk, en}                               │  │
│  │  - lemmatize() delegates to MorphoDiTa                             │  │
│  │  - provides morphological tags                                     │  │
│  │  - isAvailable() = model files exist                               │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│            │                                                             │
│            │ dependency                                                  │
│            ▼                                                             │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  libs/morphodita-jni (Issue portunix-plugins#35)                                  │  │
│  │  - MorphoDiTa.load(modelPath)                                      │  │
│  │  - MorphoDiTa.lemmatize(word)                                      │  │
│  │  - MorphoDiTa.process(text)                                        │  │
│  │  - MorphoDiTa.isAvailable()                                        │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Language to Model Mapping

```java
// Model files installed via: portunix install morphodita-model-czech
private static final Map<String, String> LANG_TO_MODEL = Map.of(
    "cs", "czech-morfflex2-pdtc.tagger",
    "sk", "slovak-morfflex-pdt.tagger",
    "en", "english-morphium-wsj.tagger"
);

// Model location (Portunix data directory)
private static final String MODEL_DIR = System.getenv("PORTUNIX_DATA") + "/models/morphodita/";
```

### MorphoDiTaBackend Implementation

```java
package org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.morphodita;

import org.cassandragargoyle.morphodita.MorphoDiTa;
import org.cassandragargoyle.morphodita.TaggedToken;
import org.cassandragargoyle.portunix.vimparser.lemmatizer.api.LemmatizerBackend;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.StemmedToken;
import org.pf4j.Extension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MorphoDiTa backend for vim-parser-lemmatizer
 * Provides true lemmatization with morphological analysis for Czech, Slovak, English
 */
@Extension
public class MorphoDiTaBackend implements LemmatizerBackend
{
    public static final String BACKEND_ID = "morphodita";
    public static final String BACKEND_NAME = "MorphoDiTa Lemmatizer (cs, sk, en)";

    // ISO 639-1 to model file mapping
    private static final Map<String, String> LANG_TO_MODEL = Map.of(
        "cs", "czech-morfflex2-pdtc.tagger",
        "sk", "slovak-morfflex-pdt.tagger",
        "en", "english-morphium-wsj.tagger"
    );

    // Portunix data directory for models
    private static final String MODEL_DIR = getModelDirectory();

    // Cached MorphoDiTa instances per language
    private final Map<String, MorphoDiTa> instances = new ConcurrentHashMap<>();

    @Override
    public String getBackendId()
    {
        return BACKEND_ID;
    }

    @Override
    public String getBackendName()
    {
        return BACKEND_NAME;
    }

    @Override
    public Set<String> getSupportedLanguages()
    {
        return LANG_TO_MODEL.keySet();
    }

    @Override
    public int getPriority()
    {
        // Higher priority than TK/Lucene for supported languages
        return 100;
    }

    @Override
    public boolean isAvailable()
    {
        // Check if at least Czech model is installed
        return MorphoDiTa.isAvailable() && isModelInstalled("cs");
    }

    @Override
    public boolean supportsLanguage(String language)
    {
        return LANG_TO_MODEL.containsKey(language) && isModelInstalled(language);
    }

    @Override
    public StemmedResult stem(String text, String language)
    {
        long startTime = System.currentTimeMillis();
        StemmedResult result = new StemmedResult(language, BACKEND_ID);

        if (text == null || text.isEmpty())
        {
            result.calculateStatistics(System.currentTimeMillis() - startTime);
            return result;
        }

        MorphoDiTa md = getInstance(language);
        if (md == null)
        {
            result.calculateStatistics(System.currentTimeMillis() - startTime);
            return result;
        }

        // Process text - returns tokens with lemmas and POS tags
        int tokenId = 1;
        for (TaggedToken token : md.process(text))
        {
            StemmedToken stemmedToken = new StemmedToken(
                "t" + String.format("%03d", tokenId++),
                token.form(),          // Original word
                token.lemma(),         // Dictionary form (true lemma)
                token.start(),
                token.end()
            );

            // Store morphological tag as metadata
            stemmedToken.setMorphTag(token.tag());

            result.addToken(stemmedToken);
        }

        result.calculateStatistics(System.currentTimeMillis() - startTime);
        return result;
    }

    private MorphoDiTa getInstance(String language)
    {
        return instances.computeIfAbsent(language, lang -> {
            try
            {
                String modelFile = LANG_TO_MODEL.get(lang);
                if (modelFile == null)
                {
                    return null;
                }
                Path modelPath = Path.of(MODEL_DIR, modelFile);
                if (!Files.exists(modelPath))
                {
                    System.err.println("[MorphoDiTa] Model not found: " + modelPath);
                    System.err.println("[MorphoDiTa] Install with: portunix install morphodita-model-" + lang);
                    return null;
                }
                return MorphoDiTa.load(modelPath.toString());
            }
            catch (Exception e)
            {
                System.err.println("[MorphoDiTa] Failed to load model for " + lang + ": " + e.getMessage());
                return null;
            }
        });
    }

    private boolean isModelInstalled(String language)
    {
        String modelFile = LANG_TO_MODEL.get(language);
        if (modelFile == null)
        {
            return false;
        }
        return Files.exists(Path.of(MODEL_DIR, modelFile));
    }

    private static String getModelDirectory()
    {
        String portunixData = System.getenv("PORTUNIX_DATA");
        if (portunixData != null)
        {
            return portunixData + "/models/morphodita";
        }
        // Fallback to user home
        return System.getProperty("user.home") + "/.portunix/data/models/morphodita";
    }

    /**
     * Close all cached instances
     */
    public void close()
    {
        instances.values().forEach(MorphoDiTa::close);
        instances.clear();
    }
}
```

### PF4J Plugin Class

```java
package org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.morphodita;

import org.pf4j.Plugin;

/**
 * PF4J plugin wrapper for MorphoDiTa backend
 */
public class MorphoDiTaPf4jPlugin extends Plugin
{
    // Plugin lifecycle is managed by PF4J
    // MorphoDiTaBackend is discovered automatically via @Extension annotation
}
```

## Module Structure

```text
java/vim-parser/vim-parser-lemmatizer-morphodita/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/org/cassandragargoyle/portunix/vimparser/lemmatizer/backend/morphodita/
│   │   │   ├── MorphoDiTaPf4jPlugin.java       # PF4J Plugin class
│   │   │   └── MorphoDiTaBackend.java          # LemmatizerBackend implementation
│   │   └── resources/
│   │       └── plugin.properties               # PF4J metadata
│   └── test/java/org/cassandragargoyle/portunix/vimparser/lemmatizer/backend/morphodita/
│       └── MorphoDiTaBackendTest.java
└── target/
    └── morphodita-backend-plugin-{version}.zip  # Distributable plugin
```

## Maven Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.cassandragargoyle.portunix</groupId>
        <artifactId>vim-parser-parent</artifactId>
        <version>0.1.2-SNAPSHOT</version>
    </parent>

    <artifactId>vim-parser-lemmatizer-morphodita</artifactId>
    <packaging>jar</packaging>

    <name>VIM Parser - MorphoDiTa Lemmatizer Backend</name>
    <description>MorphoDiTa backend plugin for vim-parser-lemmatizer (true lemmatization for cs, sk, en)</description>

    <properties>
        <plugin.id>morphodita-backend-plugin</plugin.id>
    </properties>

    <dependencies>
        <!-- Lemmatizer API (for LemmatizerBackend interface) -->
        <dependency>
            <groupId>org.cassandragargoyle.portunix</groupId>
            <artifactId>vim-parser-api</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- MorphoDiTa JNI library (Issue portunix-plugins#35) -->
        <dependency>
            <groupId>org.cassandragargoyle</groupId>
            <artifactId>morphodita-jni</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- PF4J -->
        <dependency>
            <groupId>org.pf4j</groupId>
            <artifactId>pf4j</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Assembly Plugin for creating plugin ZIP -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <descriptors>
                        <descriptor>../src/assembly/plugin-zip.xml</descriptor>
                    </descriptors>
                    <finalName>${plugin.id}-${project.version}</finalName>
                    <appendAssemblyId>false</appendAssemblyId>
                </configuration>
                <executions>
                    <execution>
                        <id>make-plugin-zip</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### plugin.properties

```properties
plugin.id=morphodita-backend-plugin
plugin.class=org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.morphodita.MorphoDiTaPf4jPlugin
plugin.version=0.1.2-SNAPSHOT
plugin.provider=CassandraGargoyle
plugin.description=MorphoDiTa lemmatizer backend for vim-parser-lemmatizer (true lemmatization for Czech, Slovak, English)
plugin.dependencies=lemmatizer-plugin
plugin.requires=*
```

## Implementation Plan

### Phase 1: Core Implementation

1. Create `vim-parser-lemmatizer-morphodita` module
2. Implement `MorphoDiTaPf4jPlugin` class
3. Implement `MorphoDiTaBackend` class
4. Create language-to-model mapping
5. Add `plugin.properties`

### Phase 2: Testing & Integration

1. Unit tests for `MorphoDiTaBackend`
2. Integration test with vim-parser pipeline
3. Test plugin loading/unloading
4. Test fallback when models not installed

### Phase 3: Documentation & Distribution

1. README with installation instructions
2. Document model installation via Portunix
3. Add to vim-parser dist plugins
4. Update Issue #1 documentation

## Acceptance Criteria

### Core Functionality

- [ ] **AC-01**: Plugin loads successfully via PF4J
- [ ] **AC-02**: `MorphoDiTaBackend` implements `LemmatizerBackend` interface
- [ ] **AC-03**: `getBackendId()` returns `"morphodita"`
- [ ] **AC-04**: `getSupportedLanguages()` returns `{cs, sk, en}`
- [ ] **AC-05**: `stem()` produces correct lemmas for Czech text
- [ ] **AC-06**: `isAvailable()` checks for model files in Portunix data directory

### Lemmatization Quality

- [ ] **AC-07**: "běželi" → lemma "běžet" (not stem "běž")
- [ ] **AC-08**: "programování" → lemma "programování" (noun form)
- [ ] **AC-09**: Morphological tags available in result metadata
- [ ] **AC-10**: Higher priority than TK/Lucene for Czech

### Integration

- [ ] **AC-11**: Backend registers automatically with `BackendRegistry`
- [ ] **AC-12**: Selectable via `--backend=morphodita` CLI option
- [ ] **AC-13**: Shows in `--list-backends` output
- [ ] **AC-14**: Works in full vim-parser pipeline

### Error Handling

- [ ] **AC-15**: Graceful fallback when models not installed
- [ ] **AC-16**: Clear error message suggesting `portunix install morphodita-model-czech`
- [ ] **AC-17**: No crash when plugin loaded but models missing

## Test Cases

### Unit Tests

```java
@Test
void shouldImplementLemmatizerBackend() {
    MorphoDiTaBackend backend = new MorphoDiTaBackend();
    assertEquals("morphodita", backend.getBackendId());
    assertTrue(backend.getSupportedLanguages().contains("cs"));
}

@Test
void shouldLemmatizeCzechVerb() {
    MorphoDiTaBackend backend = new MorphoDiTaBackend();
    assumeTrue(backend.isAvailable());  // Skip if model not installed

    StemmedResult result = backend.stem("Děti běžely domů", "cs");
    // "běžely" should lemmatize to "běžet", not just stem to "běž"
    var token = result.getTokens().stream()
        .filter(t -> t.getOriginal().equals("běžely"))
        .findFirst()
        .orElseThrow();
    assertEquals("běžet", token.getStemmed());
}

@Test
void shouldProvideMorphologicalTags() {
    MorphoDiTaBackend backend = new MorphoDiTaBackend();
    assumeTrue(backend.isAvailable());

    StemmedResult result = backend.stem("Faktura", "cs");
    var token = result.getTokens().get(0);
    assertNotNull(token.getMorphTag());
    assertTrue(token.getMorphTag().startsWith("NN"));  // Noun
}

@Test
void shouldHaveHigherPriority() {
    MorphoDiTaBackend backend = new MorphoDiTaBackend();
    assertTrue(backend.getPriority() > 50);  // Higher than Lucene/TK
}

@Test
void shouldHandleMissingModels() {
    // Simulate missing model
    MorphoDiTaBackend backend = new MorphoDiTaBackend();
    if (!backend.supportsLanguage("sk")) {
        StemmedResult result = backend.stem("test", "sk");
        assertTrue(result.getTokens().isEmpty());
    }
}
```

### Integration Tests

```java
@Test
void shouldRegisterWithBackendRegistry() {
    BackendRegistry registry = new BackendRegistry();
    MorphoDiTaBackend backend = new MorphoDiTaBackend();

    registry.register(backend);

    Optional<LemmatizerBackend> found = registry.getBackend("morphodita");
    assertTrue(found.isPresent());
}

@Test
void shouldBePreferredForCzech() {
    BackendRegistry registry = new BackendRegistry();
    registry.register(new LuceneBackend());
    registry.register(new TKBackend());
    registry.register(new MorphoDiTaBackend());

    // MorphoDiTa should be selected for Czech due to higher priority
    LemmatizerBackend selected = registry.getBackendForLanguage("cs");
    assertEquals("morphodita", selected.getBackendId());
}

@Test
void shouldWorkInPipeline() {
    // Full pipeline test with PDF -> fulltext -> lemmatization
    // ...
}
```

## Risks

| Risk | Probability | Impact | Mitigation |
| ---- | ----------- | ------ | ---------- |
| Model files not installed | High | Medium | Clear error message with install command |
| Natives not loading | Medium | High | Graceful isAvailable() check |
| Model loading slow | Low | Medium | Lazy loading, cache instances |
| Plugin dependency issues | Low | Medium | Proper PF4J configuration |

## Dependencies

### Build Dependencies

- vim-parser-api (provided)
- morphodita-jni (Issue portunix-plugins#35)
- pf4j (provided)
- junit-jupiter (test)

### Runtime Dependencies

- vim-parser-lemmatizer plugin must be loaded first
- morphodita-jni with embedded natives
- Model files installed via Portunix: `portunix install morphodita-model-czech`

## Model Installation

Users must install language models before using this backend:

```bash
# Install Czech model (required for cs language)
portunix install morphodita-model-czech

# Optional: Install other models
portunix install morphodita-model-english
portunix install morphodita-model-slovak

# Verify installation
ls $PORTUNIX_DATA/models/morphodita/
# czech-morfflex2-pdtc.tagger
# english-morphium-wsj.tagger
# slovak-morfflex-pdt.tagger
```

## Related Issues

- **Issue #1**: VIM Parser - Lemmatizer Plugin (defines backend interface)
- **Issue portunix-plugins#35**: MorphoDiTa JNI Wrapper Library (provides MorphoDiTa Java API)
- **Issue portunix-plugins#33/#34**: TK Stemmer (alternative backend with more languages)
