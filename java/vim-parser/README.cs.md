# vim-parser

Parser pro Vrstvený Informační Model (VIM) — sémantická extrakce z dokumentů.

## Co je VIM?

**VIM** (Vrstvený Informační Model / Layered Information Model) je strukturovaná reprezentace obsahu dokumentu extrahovaného v sémantických vrstvách. Místo aby byl dokument chápán jako plochý text, VIM jej rozkládá do nezávislých vrstev — textové bloky, tabulky, čárové kódy, grafika, metadata, obsah — kde každá vrstva je extrahována specializovaným pluginem. Výsledkem je bohatý JSON model (`vim.json`), který zachovává prostorové, strukturální a sémantické vztahy v původním dokumentu.

Klíčové koncepty:

- **Vrstvená extrakce** — každý plugin zpracovává jeden aspekt dokumentu nezávisle
- **Automatická orchestrace** — pluginy se spouští v pořadí podle závislostí bez nutnosti konfigurace
- **Sledování původu (provenance)** — každý extrahovaný prvek odkazuje na své umístění v dokumentu
- **Skladatelný pipeline** — pluginy lze přidávat, odebírat nebo nahrazovat bez ovlivnění ostatních

## Architektura

### Struktura modulů

```text
vim-parser/
├── vim-parser-api/          # rozhraní pluginů + PF4J ExtensionPoints
├── vim-parser-core/         # orchestrátor, CLI, fat JAR launcher
├── vim-parser-barcode/      # extrakce čárových kódů (ZXing) [plugin]
├── vim-parser-fulltext/     # extrakce textu (PDFBox) [plugin]
├── vim-parser-lemmatizer/   # stemming/lemmatizace (Lucene) [plugin]
├── vim-parser-spayd/        # parser SPAYD plateb [plugin]
├── vim-parser-visualizer/   # HTML vizualizér [plugin]
└── plugins/                 # runtime adresář pluginů (ZIP soubory)
```

### Plugin systém PF4J

vim-parser používá [PF4J](https://pf4j.org/) (Plugin Framework for Java) pro správu pluginů:

- **Dynamické načítání** — pluginy jsou načítány za běhu z adresáře `plugins/` vedle JARu
  (záloha: `plugins/` v pracovním adresáři); `auto --plan` vypíše použitý adresář.
  Když se nenajde žádný plugin, `auto` a `extract` skončí s kódem 1 a nic nezapíšou
- **Správa životního cyklu** — load, start, stop, unload
- **Verzování** — každý plugin má nezávislou verzi
- **Hot-reload** — přidávání nových pluginů bez nutnosti restartu

### Struktura distribuce

```text
dist/
├── vim-parser-0.1.0.jar     # fat JAR (core + API + PF4J runtime)
├── plugin.json              # Portunix metadata
└── plugins/
    ├── barcode-plugin-0.1.0.zip
    ├── fulltext-plugin-0.1.0.zip
    ├── lemmatizer-plugin-0.1.0.zip
    ├── spayd-plugin-0.1.0.zip
    └── visualizer-plugin-0.1.0.zip
```

### Sestavení

Spouštějte z kořene repozitáře (je tam `Makefile`):

```bash
# sestavení všech modulů + plugin ZIP balíčků
make build

# vytvoření distribučního balíčku
make dist

# spuštění testů
make test

# výpis dostupných pluginů
make plugins
```

### Použití s Portunixem

```bash
# automatická extrakce pomocí všech pluginů
portunix vim-parser auto invoice.pdf

# zahrnutí metadat dokumentu (výchozí: zapnuto)
portunix vim-parser auto --metadata invoice.pdf

# zahrnutí surového obsahu plného textu (výchozí: vypnuto)
portunix vim-parser auto --fulltext invoice.pdf

# zahrnutí SHA-256 hashe v metadatech (výchozí: vypnuto)
portunix vim-parser auto --hash invoice.pdf

# kombinace voleb
portunix vim-parser auto --fulltext --hash invoice.pdf

# povolení stemmingu (pro vyhledávání/NLP)
portunix vim-parser auto --stemming invoice.pdf

# stemming s konkrétním backendem a jazykem
portunix vim-parser auto --stemming --backend=lucene --lang=cs invoice.pdf

# výpis nainstalovaných pluginů
portunix vim-parser plugins --deps

# generování HTML vizualizace
portunix vim-parser visualize output.vim.json
```

`visualize` čte VIM dokument striktně: skalár špatného JSON typu (například číslo v textovém poli,
řetězec `"0.9"` nebo `"NaN"` v číselném poli nebo `2.5` v celočíselném poli) je chyba načtení,
která jmenuje pole. Neznámá pole se ignorují.

### CLI volby (příkaz auto)

| Volba | Výchozí | Popis |
| ----- | ------- | ----- |
| `--metadata` / `--no-metadata` | zapnuto | zahrnout metadata dokumentu (Schema.org) |
| `--fulltext` | vypnuto | zahrnout surový obsah plného textu |
| `--hash` | vypnuto | zahrnout SHA-256 hash v metadatech |
| `--stemming` | vypnuto | povolit stemming/lemmatizaci |
| `--backend <id>` | lucene | použitý stemming backend |
| `--lang <kód>` | auto | jazyk pro stemming (cs, de, en, es, fr, it) |
| `--exclude <pluginy>` | - | seznam pluginů k vyloučení (oddělený čárkou) |
| `--dry-run` | vypnuto | zobrazí plán provedení bez vlastního spuštění |
| `-v, --verbose` | vypnuto | podrobný výstup |

---

## Koncept

**Univerzální „dokonalý" formát pro ukládání informací z libovolného souboru** — designové poznámky:
jak ukládat výsledek pochopení souboru tak, aby z něj bylo možné snadno extrahovat data a počítat.

## 1) Skutečný problém (není to PDF)

PDF je jen symptom. Skutečným problémem je:

> **Ztráta sémantiky při přenosu dat mezi systémy**

Typický řetězec:

```text
DB → reportovací nástroj → PDF → člověk → ruční přepis
```

Zákazník nechce PDF. Zákazník chce:

- hodnoty
- vztahy
- význam (co je sloupec, jednotka, měna, období)
- doklad o původu (že data skutečně pocházejí z daného dokumentu)

---

## 2) Základní pravda

> **Neexistuje jediný univerzální formát pro všechny soubory.**
> **Existuje univerzální mezivrstvý model pro reprezentaci informací.**

Otázka nestojí: „Jaký formát uloží všechno?"
Ale: **„Jak uložit výsledek pochopení souboru?"**

---

## 3) Ideální cíl: strojově čitelná rekonstrukce reality

Na příkladu tabulky v PDF:

Zákazník nepotřebuje:

- text
- grafiku
- layout

Zákazník potřebuje:

- **entitu**: tabulku
- **strukturu**: sloupce, řádky, buňky
- **typy**: číslo, měna, datum
- **význam**: např. `net_amount_eur`, `vat_rate`, `period`
- **původ**: kde v dokumentu se nacházela (stránka, bounding box)

---

## 4) Principy „dokonalého" formátu

### 4.1 Oddělit obsah, strukturu a význam

- obsah: `123 456`
- struktura: buňka → sloupec → tabulka
- význam: `net_amount`, `currency=CZK`, `vat_included=false`

### 4.2 Být vrstvený

Jedna vrstva nikdy nestačí (raw → layout → struktura → sémantika).

### 4.3 Zachovat původ (Provenance)

- z jakého souboru
- z jaké stránky
- z jaké oblasti dokumentu

### 4.4 Být obousměrný

- z modelu lze provádět **výpočty**
- z modelu lze (alespoň přibližně) **rekonstruovat** dokument

### 4.5 Zvládnout nejistotu

Parsování PDF nebude 100% spolehlivé, proto:

- confidence score
- metoda extrakce
- alternativní interpretace

---

## 5) Návrh architektury: Vrstvený Informační Model (VIM)

„Dokonalý formát" není jedna věc, ale **pipeline + datový model**.

### Vrstva 1 – Fyzická reprezentace (raw/layout)

Odpovídá na: *co tam skutečně je*

```json
{
  "page": 3,
  "bbox": [120, 340, 560, 410],
  "text": "Total excl. VAT"
}
```

### Vrstva 2 – Strukturální model

Odpovídá na: *jak to vypadá*

```json
{
  "type": "table",
  "columns": ["Item", "Quantity", "Price", "Total"],
  "rows": [
    ["A", "2", "100", "200"],
    ["B", "1", "300", "300"]
  ]
}
```

### Vrstva 3 – Sémantický model

Odpovídá na: *co to znamená*

```json
{
  "entity": "invoice_line",
  "fields": {
    "item_name": "A",
    "quantity": 2,
    "unit_price": { "value": 100, "currency": "CZK" },
    "line_total": { "value": 200, "currency": "CZK", "vat_included": false }
  }
}
```

### Vrstva 4 – Původ a důvěra (Provenance & Trust)

Odpovídá na: *proč tomu věříme*

```json
{
  "source": {
    "file": "invoice.pdf",
    "page": 3,
    "bbox": [120, 340, 560, 410]
  },
  "confidence": 0.87,
  "method": "table-detection-v2"
}
```

---

## 6) Jaký formát skutečně použít

| Úroveň     | Co řeší         | Typické formáty        |
| ---------- | --------------- | ---------------------- |
| Raw soubor | originál        | PDF / DOCX / XLSX      |
| Layout     | text + pozice   | ALTO / PAGE XML        |
| Struktura  | tabulky         | JSON + Schema          |
| Sémantika  | význam          | JSON-LD / RDF          |
| Výpočty    | agregace        | tabulkový model        |
| Úložiště   | dotazy          | document DB / graph DB |

---

## 7) Ideální výstup pro zákazníka

```json
{
  "document_type": "invoice",
  "totals": {
    "net": 123456,
    "vat": 25925,
    "gross": 149381,
    "currency": "CZK"
  }
}
```

```sql
SELECT SUM(net) FROM invoices;
```

---

## 8) Shrnutí

> **Dokonalý formát neexistuje jako jediný soubor.**
> Existuje **vrstvený model + pipeline**, který zachovává:

- layout
- strukturu
- sémantiku
- původ a důvěru

---

## 9) Budoucí práce

- Document Semantic Model (DSM)
- JSON Schema pro vrstvy
- ontologie tabulek
- mapování layout → význam
- zpracování nejistoty
