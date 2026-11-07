# VIM Parser - Stemmer TK

Java JNI obal pro C++ knihovnu TK Stemmer. Poskytuje stemming slov pro 33 jazyků pomocí algoritmů Snowball.

## Externí závislosti (povinné)

Tento modul vyžaduje **nativní C++ knihovny**, které NEJSOU součástí repozitáře. Před sestavením je nutné je získat a zkopírovat.

### Vyžadované knihovny

| Platforma | Cílový adresář | Vyžadované soubory |
| --------- | --------------- | ------------------ |
| Linux x86_64 | `src/main/resources/native/linux-x86_64/` | `libstemmer.so`, `libtkstemmer.so` |
| Windows x86_64 | `src/main/resources/native/windows-x86_64/` | `stemmer.dll`, `tkstemmer.dll` |
| macOS arm64 | `src/main/resources/native/macos-aarch64/` | `libstemmer.dylib`, `libtkstemmer.dylib` |

### Kde knihovny získat

**Možnost 1: Předkompilované knihovny**

- Kontaktuj tým CassandraGargoyle pro předkompilované binárky
- Zkopíruj je do odpovídajícího adresáře `src/main/resources/native/{platforma}/`

**Možnost 2: Sestavení ze zdrojů TK Stemmeru**

```bash
# Předpoklady (Linux)
sudo apt-get install cmake build-essential

# Sestavení pomocí Python skriptu
python3 scripts/build_native.py --source-dir /cesta/k/tk-stemmer-source
```

Zdrojový kód vychází ze stemmingového frameworku [Snowball](https://snowballstem.org/) s C++ obalem od TK.

### Ověření

Po zkopírování knihoven:

```bash
ls -la src/main/resources/native/linux-x86_64/
# Mělo by zobrazit: libstemmer.so, libtkstemmer.so
```

**Poznámka:** Bez nativních knihoven budou testy přeskočeny (nikoli neúspěšné) díky anotaci `@EnabledIf("isNativeAvailable")`.

## Vlastnosti

- **33 jazyků**: čeština, angličtina, němčina, francouzština, slovenština a mnoho dalších
- **Vysoký výkon**: nativní C++ implementace přes JNI
- **Podpora UTF-8**: plná práce s Unicode textem
- **Dávkové zpracování**: efektivní stemming seznamů slov
- **Bez konfigurace**: nativní knihovny jsou součástí JAR
- **Multiplatformní**: Linux x86_64 a Windows x86_64

## Použití

```java
// Vytvoření stemmeru pro daný jazyk
try (TKStemmer stemmer = TKStemmer.create("czech")) {
    // Stemming jednoho slova
    String stem = stemmer.stem("programování");  // "programov"

    // Dávkový stemming
    List<String> stems = stemmer.stemBatch(List.of("faktura", "fakturách", "faktuře"));
}

// Kontrola dostupnosti
if (TKStemmer.isAvailable()) {
    // Nativní knihovna je načtena
}

// Výpis podporovaných jazyků
Set<String> languages = TKStemmer.supportedLanguages();
```

## Podporované jazyky

arabština, arménština, baskičtina, katalánština, čeština, dánština, holandština, angličtina, finština, francouzština, němčina, řečtina, hindština, maďarština, indonéština, irština, italština, litevština, nepálština, norština, polština, portugalština, rumunština, ruština, srbština, slovenština, španělština, švédština, tamilština, turečtina, ukrajinština, jidiš

## Sestavení nativních knihoven

### Předpoklady

- Python 3.8+
- JDK 21+
- CMake 3.20+
- GCC 11+ (Linux) nebo Visual Studio 2022 (Windows)
- C++ knihovna TK Stemmer

### Příkazy pro sestavení

```bash
# Linux
export JAVA_HOME=/cesta/k/jdk21
./scripts/build-linux.sh /cesta/k/tk-stemmer

# Windows (PowerShell)
$env:JAVA_HOME = "C:\cesta\k\jdk21"
.\scripts\build-windows.ps1 C:\cesta\k\tk-stemmer

# Nebo přímo přes Python (multiplatformní)
python scripts/build_native.py /cesta/k/tk-stemmer --clean
```

### Sestavení Java knihovny

```bash
mvn clean package
```

## Struktura JARu

```text
vim-parser-stemmer-tk-{verze}.jar
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

## Maven závislost

```xml
<dependency>
    <groupId>org.cassandragargoyle.portunix</groupId>
    <artifactId>vim-parser-stemmer-tk</artifactId>
    <version>0.1.3-SNAPSHOT</version>
</dependency>
```

## Bezpečnost vláken

Každá instance `TKStemmer` NENÍ thread-safe. Pro souběžné použití vytvoř samostatné instance, nebo přístup synchronizuj externě.

## Ošetření chyb

- `UnsupportedLanguageException` — vyhozeno při vytváření stemmeru pro nepodporovaný jazyk
- `UnsatisfiedLinkError` — vyhozeno, pokud nelze načíst nativní knihovnu
- `IllegalStateException` — vyhozeno při použití stemmeru po `close()`

## Licence

Proprietární — CassandraGargoyle
