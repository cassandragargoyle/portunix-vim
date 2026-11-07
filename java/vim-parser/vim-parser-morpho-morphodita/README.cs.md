# VIM Parser - JNI obal pro MorphoDiTa

Java JNI obal pro morfologický analyzátor [MorphoDiTa](https://github.com/ufal/morphodita) z ÚFAL (Univerzita Karlova).

## Vlastnosti

- **Lemmatizace** — vrací slovníkové tvary slov (nikoli pouze kmeny)
- **POS tagging** — kompletní morfologická analýza s detailními tagy
- **Morfologická analýza** — všechny možné interpretace u nejednoznačných slov
- **Jazyky** — čeština, slovenština, angličtina
- **Výkon** — 50 000+ slov/sekundu

## Požadavky

- JDK 21+
- Python 3 (pro build skripty)
- Nativní knihovny (stahují se automaticky)
- Jazykové modely (instalují se přes Portunix)

## Rychlý start

### 1. Stažení nativních knihoven

```bash
cd scripts
./fetch-natives.sh
```

### 2. Instalace jazykových modelů

```bash
portunix install morphodita-model-czech
```

### 3. Použití v Javě

```java
try (MorphoDiTa md = MorphoDiTa.loadDefault()) {
    // Lemmatizace jednoho slova
    LemmaResult result = md.lemmatize("programování");
    System.out.println(result.lemma());  // "programování"
    System.out.println(result.tag());    // "NNNS1-----A----"

    // Zpracování textu
    List<TaggedToken> tokens = md.process("Faktura za služby");
    for (TaggedToken token : tokens) {
        System.out.printf("%s -> %s (%s)%n",
            token.form(), token.lemma(), token.tag());
    }

    // Získání všech možných analýz
    List<LemmaResult> analyses = md.analyze("běželi");
    for (LemmaResult a : analyses) {
        System.out.printf("%s: %s%n", a.lemma(), a.tag());
    }
}
```

## API reference

### MorphoDiTa

Hlavní třída pro morfologickou analýzu.

| Metoda | Popis |
| ------ | ----- |
| `loadDefault()` | Načte výchozí český model |
| `loadForLanguage(language)` | Načte model pro daný jazyk |
| `load(modelPath)` | Načte z konkrétního souboru s modelem |
| `lemmatize(word)` | Vrátí lemma pro jedno slovo |
| `analyze(word)` | Vrátí všechny možné analýzy |
| `process(text)` | Tokenizuje a otaguje celý text |
| `isAvailable()` | Ověří, zda je nativní knihovna k dispozici |
| `checkForUpdates()` | Zkontroluje novější verze na GitHubu |

### LemmaResult

Výsledek lemmatizace.

| Pole | Popis |
| ---- | ----- |
| `lemma` | Slovníkový tvar (vyčištěný) |
| `tag` | Morfologický tag |
| `rawLemma` | Původní lemma s technickými příponami |

### TaggedToken

Token ze zpracování textu.

| Pole | Popis |
| ---- | ----- |
| `form` | Původní tvar slova |
| `lemma` | Slovníkový tvar |
| `tag` | Morfologický tag |
| `start` | Počáteční offset v textu |
| `end` | Koncový offset v textu |

## Sestavení

```bash
# Z adresáře vim-parser-morphodita
mvn clean package

# Přeskočení stažení nativních knihoven, pokud již existují
mvn clean package -DskipNativeFetch=true
```

## Kontrola verze

Knihovna umí zkontrolovat aktualizace:

```java
Optional<UpdateInfo> update = MorphoDiTa.checkForUpdates();
if (update.isPresent()) {
    System.out.println(update.get().getMessage());
}
```

## Licence

- Tento obal: MIT License
- Knihovna MorphoDiTa: MPL-2.0
- Jazykové modely: CC BY-NC-SA 4.0

## Zdroje

- [MorphoDiTa GitHub](https://github.com/ufal/morphodita)
- [Dokumentace MorphoDiTa](https://ufal.mff.cuni.cz/morphodita)
- [Jazykové modely](https://lindat.mff.cuni.cz/repository/xmlui/handle/11234/1-5457)
