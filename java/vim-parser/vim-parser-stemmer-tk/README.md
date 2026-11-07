# VIM Parser - Stemmer TK

Java JNI wrapper for TK Stemmer C++ library. Provides word stemming for 33 languages using Snowball algorithms.

## External Dependencies (Required)

This module requires **native C++ libraries** that are NOT included in the repository. You must obtain and copy them before building.

### Required Libraries

| Platform | Target Directory | Required Files |
| -------- | ---------------- | -------------- |
| Linux x86_64 | `src/main/resources/native/linux-x86_64/` | `libstemmer.so`, `libtkstemmer.so` |
| Windows x86_64 | `src/main/resources/native/windows-x86_64/` | `stemmer.dll`, `tkstemmer.dll` |
| macOS arm64 | `src/main/resources/native/macos-aarch64/` | `libstemmer.dylib`, `libtkstemmer.dylib` |

### Where to Get Libraries

**Option 1: Pre-built libraries**

- Contact CassandraGargoyle team for pre-built binaries
- Copy to appropriate `src/main/resources/native/{platform}/` directory

**Option 2: Build from TK Stemmer source**

```bash
# Prerequisites (Linux)
sudo apt-get install cmake build-essential

# Build using Python script
python3 scripts/build_native.py --source-dir /path/to/tk-stemmer-source
```

The source code is based on [Snowball](https://snowballstem.org/) stemming framework with TK's C++ wrapper.

### Verification

After copying libraries:

```bash
ls -la src/main/resources/native/linux-x86_64/
# Should show: libstemmer.so, libtkstemmer.so
```

**Note:** Without native libraries, tests will be skipped (not failed) due to `@EnabledIf("isNativeAvailable")` annotation.

## Features

- **33 Languages**: Czech, English, German, French, Slovak, and many more
- **High Performance**: Native C++ implementation via JNI
- **UTF-8 Support**: Full Unicode text handling
- **Batch Processing**: Efficient stemming of word lists
- **Zero Configuration**: Native libraries embedded in JAR
- **Cross-Platform**: Linux x86_64 and Windows x86_64

## Usage

```java
// Create stemmer for a language
try (TKStemmer stemmer = TKStemmer.create("czech")) {
    // Stem single word
    String stem = stemmer.stem("programování");  // "programov"

    // Batch stemming
    List<String> stems = stemmer.stemBatch(List.of("faktura", "fakturách", "faktuře"));
}

// Check availability
if (TKStemmer.isAvailable()) {
    // Native library is loaded
}

// List supported languages
Set<String> languages = TKStemmer.supportedLanguages();
```

## Supported Languages

arabic, armenian, basque, catalan, czech, danish, dutch, english, finnish, french, german, greek, hindi, hungarian, indonesian, irish, italian, lithuanian, nepali, norwegian, polish, portuguese, romanian, russian, serbian, slovak, spanish, swedish, tamil, turkish, ukrainian, yiddish

## Building Native Libraries

### Prerequisites

- Python 3.8+
- JDK 21+
- CMake 3.20+
- GCC 11+ (Linux) or Visual Studio 2022 (Windows)
- TK Stemmer C++ library

### Build Commands

```bash
# Linux
export JAVA_HOME=/path/to/jdk21
./scripts/build-linux.sh /path/to/tk-stemmer

# Windows (PowerShell)
$env:JAVA_HOME = "C:\path\to\jdk21"
.\scripts\build-windows.ps1 C:\path\to\tk-stemmer

# Or directly with Python (cross-platform)
python scripts/build_native.py /path/to/tk-stemmer --clean
```

### Building Java Library

```bash
mvn clean package
```

## JAR Structure

```text
vim-parser-stemmer-tk-{version}.jar
├── org/cassandragargoyle/tk/
│   ├── TKStemmer.class
│   ├── UnsupportedLanguageException.class
│   └── internal/
│       └── NativeLoader.class
└── native/
    ├── linux-x86_64/
    │   ├── libtk-stemmer.so
    │   └── libtk-stemmer-jni.so
    └── windows-x86_64/
        ├── libtk-stemmer.dll
        └── tk-stemmer-jni.dll
```

## Maven Dependency

```xml
<dependency>
    <groupId>org.cassandragargoyle.portunix</groupId>
    <artifactId>vim-parser-stemmer-tk</artifactId>
    <version>0.1.3-SNAPSHOT</version>
</dependency>
```

## Thread Safety

Each `TKStemmer` instance is NOT thread-safe. Create separate instances for concurrent use, or synchronize access externally.

## Error Handling

- `UnsupportedLanguageException` - thrown when creating stemmer for unsupported language
- `UnsatisfiedLinkError` - thrown when native library cannot be loaded
- `IllegalStateException` - thrown when using stemmer after close()

## License

Proprietary - CassandraGargoyle
