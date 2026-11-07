# Czech Stemming Accuracy Analysis

Analýza přesnosti stemmerů pro český jazyk v kontextu VIM Parser.

## Co je stemming?

**Stemming** = odstraňování přípon (suffixů) za účelem získání společného kořene slova.

```text
programování → programov
programátor  → programát
program      → program
```

**Cíl:** Slova se stejným významem by měla mít stejný stem pro účely vyhledávání.

## Problém českého jazyka

Čeština je **morfologicky bohatý jazyk** s:

- 7 pádů (nominativ, genitiv, dativ, akuzativ, vokativ, lokál, instrumentál)
- 2 čísla (singulár, plurál)
- 3 rody (mužský, ženský, střední)
- Měkké a tvrdé skloňování
- Alternace hlásek (kniha→knize, ruka→ruce)
- Pohyblivé 'e' (pes→psa, den→dne)
- Supletivní tvary (člověk→lidé, dobrý→lepší)

### Příklad: slovo "faktura"

| Pád | Singulár | Plurál |
| --- | -------- | ------ |
| Nominativ | faktura | faktury |
| Genitiv | faktury | faktur |
| Dativ | faktuře | fakturám |
| Akuzativ | fakturu | faktury |
| Vokativ | fakturo | faktury |
| Lokál | faktuře | fakturách |
| Instrumentál | fakturou | fakturami |

**Všechny tyto tvary by měly mít stejný stem**, ale:

```text
faktura   → faktur   (Snowball Czech)
faktury   → faktur   ✓
faktuře   → faktuř   ✗ ODLIŠNÝ! (ř vs r)
fakturu   → faktur   ✓
fakturám  → fakturám ✗ (neodstraněna přípona)
```

## Typy testů

### 1. Declension Consistency (konzistence skloňování)

Měří: **Kolik slov má všechny své tvary se stejným stemem?**

```java
// Pro každé lemma (základní tvar)
Map<String, List<String>> stemToWords = groupByStem(allForms);
boolean isConsistent = stemToWords.size() == 1;  // Jen 1 unikátní stem?
```

**Příklad výsledku:**

```text
faktura: INCONSISTENT -> {faktur=9, faktuř=1}
moře:    CONSISTENT   -> {moř=5}
```

**Interpretace:**

- 100% = perfektní stemmer (všechny tvary → stejný stem)
- 0% = každý tvar má jiný stem
- Reálně pro češtinu: 3-10% (suffix-stripping má limity)

### 2. Overall Accuracy (celková přesnost)

Měří: **Kolik slov spadne do majoritního stemu své skupiny?**

```java
// Pro každé lemma
int maxCount = stemCounts.values().max();  // Nejvíc slov se stejným stemem
wordsWithMajorityStem += maxCount;
```

**Příklad:**

```text
faktura: {faktur=9, faktuř=1}
         ^^^^^^^^
         9 z 10 slov má majoritní stem = 90% pro toto slovo
```

**Interpretace:**

- Tolerantnější metrika - akceptuje drobné odchylky
- Reálně pro češtinu: 65-75%

### 3. Processing Speed

Měří: **Kolik slov za sekundu stemmer zpracuje?**

- Lucene (pure Java): ~10,000-50,000 tokens/sec
- TK/Snowball (native C++): ~50,000-200,000 tokens/sec

## Srovnání stemmerů

### Lucene Czech Stemmer

- **Typ:** Suffix-stripping (Java implementace)
- **Algoritmus:** Specifický pro češtinu, rule-based
- **Výhody:** Pure Java, žádné native dependencies
- **Nevýhody:** Omezená přesnost, nezvládá alternace

### TK Stemmer (Snowball)

- **Typ:** Suffix-stripping (C++ implementace)
- **Algoritmus:** Snowball framework, 33 jazyků
- **Výhody:** Rychlý (native), multi-language
- **Nevýhody:** Podobné limity jako Lucene pro češtinu

### MorphoDiTa (budoucí)

- **Typ:** Morfologická analýza + lemmatizace
- **Algoritmus:** Statistický model trénovaný na korpusu
- **Výhody:** Vysoká přesnost (95%+), zvládá alternace
- **Nevýhody:** Větší model (~50MB), pomalejší

## Problematické případy

### Pohyblivé 'e' (Fleeting vowel)

```text
pes  → pes/ps    (stemmer neví o alternaci)
psa  → ps
psovi→ ps
```

Stemmer vidí "pes" a "psa" jako různá slova.

### Alternace souhlásek

```text
kniha → knih
knize → kniz     (h→z alternace v dativu/lokálu)
```

### Supletivní tvary

```text
člověk → člověk
lidé   → lid     (úplně jiný kořen!)

dobrý  → dobr
lepší  → lepš    (komparativ má jiný kořen)
```

## Naměřené výsledky (aktuální testy)

Výsledky z testů na 41 podstatných jménech a 15 přídavných jménech:

| Metrika | Lucene Czech | TK (Snowball) | Vítěz |
| ------- | ------------ | ---------------- | ----- |
| **Noun Consistency** | 56.1% (23/41) | 12.2% (5/41) | Lucene |
| **Adjective Consistency** | 40.0% (6/15) | 13.3% (2/15) | Lucene |
| **Overall Accuracy** | 79.2% (270/341) | 72.7% (248/341) | Lucene |
| **Speed** | ~600k tok/s | ~4M tok/s | TK (7× rychlejší) |

### Klíčové poznatky

**Lucene Czech je překvapivě přesnější** než TK/Snowball, protože:

- Byl speciálně navržen pro češtinu
- Obsahuje pravidla pro české hláskové alternace
- Snowball používá obecný algoritmus pro všechny jazyky

**TK/Snowball je rychlejší** díky:

- Native C++ implementaci (vs pure Java)
- Jednodušší algoritmus (menší overhead)

### Doporučení výběru

| Použití | Doporučený stemmer |
| ------- | ------------------ |
| Fulltext search s důrazem na přesnost | **Lucene Czech** |
| Vysoký objem textu, rychlost kritická | **TK** |
| Vícejazyčné aplikace | **TK** (33 jazyků) |
| Přesná lemmatizace | **MorphoDiTa** (budoucí) |

**Závěr:** Suffix-stripping stemmery jsou vhodné pro:

- Rychlé zpracování velkých objemů textu
- Základní normalizaci pro fulltext search
- Případy kde není kritická 100% přesnost

Pro přesnou lemmatizaci je potřeba morfologický analyzátor (MorphoDiTa, Majka).

## Testovací data

Testovací data jsou v souboru `czech-stemming-test-data.json` a obsahují:

- **41 podstatných jmen** různých deklinačních vzorů (341 tvarů)
- **15 přídavných jmen** včetně stupňování
- **4 vzorové texty** (faktura, smlouva, obtížná morfologie, business report)
- **Thresholds** nastavené podle reálných výsledků stemmerů

### Kategorie testovacích slov

| Kategorie | Příklady | Obtížnost |
| --------- | -------- | --------- |
| Business terms | faktura, dodavatel, částka | Střední |
| Fleeting 'e' | pes, orel, den, sen, lev | Vysoká |
| Consonant alternation | kniha, ruka, noha, moucha | Vysoká |
| Neuter nouns | město, moře, kuře, dítě | Střední |
| Suppletive forms | člověk/lidé, rok/léta | Velmi vysoká |
| Dual forms | oko/oči, ucho/uši | Vysoká |
