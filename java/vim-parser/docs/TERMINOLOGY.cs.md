# Terminologie

Slovník pojmů používaných v projektu vim-parser. Cílem je sjednotit význam výrazů napříč kódem, dokumentací a komunikací s uživateli.

## Základní pojmy

### VIM (Layered Information Model)

Vrstvený Informační Model — strukturovaná reprezentace obsahu dokumentu rozložená do sémantických vrstev.

### vim.json

Výstupní JSON soubor obsahující kompletní VIM model dokumentu.

### Vrstva (Layer)

Nezávislá úroveň reprezentace dokumentu (fyzická / strukturální / sémantická / původ).

### Pipeline

Posloupnost pluginů spuštěných v pořadí podle závislostí.

### Pochopení souboru (File comprehension)

Proces převodu surového souboru na sémanticky bohatý model.

## Architektura pluginů

### Plugin

Nezávislý modul rozšiřující funkčnost vim-parseru (extraktor, parser, vizualizér).

### Extraktor (Extractor)

Plugin extrahující surová data z dokumentu (text, čárové kódy, tabulky, grafika).

### Parser

Plugin transformující extrahovaná data na sémantické entity (např. SPAYD platba).

### Vizualizér (Visualizer)

Plugin generující výstupní reprezentaci VIM modelu (HTML, JSON).

### PF4J

Plugin Framework for Java — runtime pro dynamické načítání pluginů.

### ExtensionPoint

Bod rozšíření definovaný v API, který pluginy implementují.

### ServiceLoader

Standardní Java mechanismus pro objevení implementací rozhraní.

### Fat JAR (Uber JAR)

JAR obsahující všechny závislosti i runtime PF4J.

### Hot-reload

Schopnost přidat plugin za běhu bez restartu aplikace.

### Orchestrátor (Orchestrator)

Komponenta řídící spouštění pluginů v pořadí podle závislostí.

### Životní cyklus (Lifecycle)

Posloupnost stavů pluginu: load → start → stop → unload.

## Datový model

### Entita (Entity)

Sémantická jednotka v dokumentu (např. faktura, řádek faktury, adresa).

### Textový blok (TextBlock)

Souvislý úsek textu s pozicí na stránce.

### Bounding box (bbox)

Obdélníková oblast v dokumentu udaná souřadnicemi `[x1, y1, x2, y2]`.

### Stránka (Page)

Jednotlivá strana dokumentu (1-based číslování).

### Metadata

Strukturované informace o dokumentu (autor, datum, jazyk, schéma Schema.org).

### Plný text (Fulltext)

Surový obsah dokumentu jako prostý text.

### Čárový kód (Barcode)

QR Code, Data Matrix, PDF417, Aztec.

### SPAYD

Short Payment Descriptor — formát českých platebních QR kódů (`SPD*…`).

### TOC (Table of Contents)

Obsah dokumentu — hierarchie nadpisů.

## Původ a důvěra

### Původ (Provenance)

Sledování zdroje extrahovaného prvku (soubor, stránka, bbox).

### Důvěryhodnost (Confidence)

Číselné skóre `[0,1]` vyjadřující jistotu extrakce.

### Metoda extrakce (Extraction method)

Identifikátor algoritmu/verze, který data vytvořil.

### Alternativní interpretace (Alternative interpretation)

Konkurenční výklad téhož vstupu při nejistotě.

## Zpracování textu

### Stemming

Redukce slova na kmen (algoritmický, ztrátový).

### Lemmatizace (Lemmatization)

Převod slova na základní tvar (lemma) s využitím slovníku/morfologie.

### Backend

Implementace stemmingu/lemmatizace (Lucene, MorphoDiTa, TK).

### Lemma

Základní (slovníkový) tvar slova.

### Token

Jednotka textu po tokenizaci (slovo, číslo, interpunkce).

## Vrstvy VIM

### Vrstva 1 — Fyzická (Physical / raw)

Co je v souboru fyzicky přítomno (znaky, pozice).

### Vrstva 2 — Strukturální (Structural)

Jak je obsah uspořádán (tabulky, sloupce, buňky).

### Vrstva 3 — Sémantická (Semantic)

Co obsah znamená (entita, pole, jednotky).

### Vrstva 4 — Původ a důvěra (Provenance & Trust)

Odkud data pochází a jak moc jim věříme.

## CLI a běh

### Auto režim (Auto mode)

Automatické spuštění všech dostupných pluginů.

### Dry-run

Zobrazí plán provedení bez vlastního spuštění.

### Vyloučení pluginů (Plugin exclusion)

Volba `--exclude` pro vynechání konkrétních pluginů.

### Verbose

Podrobný výstup určený pro diagnostiku.

### Distribuce (Distribution)

Sestavený balíček `dist/` připravený k nasazení.

## Související standardy

### Schema.org

Slovník strukturovaných dat pro popis entit (Document, Person, Organization, …).

### JSON-LD

JSON for Linking Data — sémantický formát rozšiřující JSON o kontext.

### RDF

Resource Description Framework — model pro popis vztahů mezi entitami.

### ALTO XML

Analyzed Layout and Text Object — XML formát pro popis layoutu naskenovaných stránek.

### PAGE XML

Page Analysis and Ground-truth Elements — XML pro layout a ground-truth dokumentů.

### DSM (Document Semantic Model)

Sémantický model dokumentu — budoucí práce.

## Zkratky

### VIM

Vrstvený Informační Model (Layered Information Model).

### PF4J

Plugin Framework for Java — runtime pro pluginy.

### TOC

Table of Contents — obsah dokumentu.

### SPAYD

Short Payment Descriptor — formát českých platebních QR kódů.

### DPI

Dots Per Inch — rozlišení vykreslování stránek pro skenování čárových kódů.

### API

Application Programming Interface — programové rozhraní (modul `vim-parser-api`).

### CLI

Command-Line Interface — příkazová řádka.

### DSM

Document Semantic Model — sémantický model dokumentu.

## Konvence pojmenování

- **Anglicky** — kód, identifikátory, JSON klíče, commit message, technická dokumentace v `README.md`
- **Česky** — uživatelská dokumentace `*.cs.md`, koncepční texty, terminologie
- **Plugin ID** — krátký název bez prefixu `vim-parser-` (např. `barcode`, `fulltext`, `spayd`)
- **Modul Maven** — název s prefixem `vim-parser-` (např. `vim-parser-barcode`)
