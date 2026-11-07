# vim-parser-lemmatizer

Plugin pro stemming/lemmatizaci ve VIM Parseru s architekturou umožňující vyměnitelné backendy.

## Vlastnosti

- **Pluginovatelná architektura backendů** — snadné přidávání nových NLP backendů
- **Podpora více jazyků** — čeština, němčina, angličtina, španělština, francouzština, italština
- **Backend Apache Lucene** — čistá Java, bez externích závislostí
- **Sledování pozic tokenů** — mapování kmenů zpět na zdrojový text
- **Statistiky** — počet tokenů, unikátní kmeny, doba zpracování

## Podporované jazyky

| Jazyk | Kód | Backend | Poznámky |
| ----- | --- | ------- | -------- |
| Čeština | cs | Lucene CzechAnalyzer | Stemmer odstraňující koncovky |
| Němčina | de | Lucene GermanAnalyzer | |
| Angličtina | en | Lucene EnglishAnalyzer | |
| Španělština | es | Lucene SpanishAnalyzer | |
| Francouzština | fr | Lucene FrenchAnalyzer | |
| Italština | it | Lucene ItalianAnalyzer | |

## Architektura

```text
┌─────────────────────────────────────────────────┐
│         Lemmatizer Plugin (abstraktní)          │
├─────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │
│  │   Lucene    │  │ MorphoDiTa  │  │ Hunspell │ │
│  │  (v1.0)     │  │  (budoucí)  │  │ (budoucí)│ │
│  └─────────────┘  └─────────────┘  └──────────┘ │
└─────────────────────────────────────────────────┘
```

## Použití

```bash
# povolení stemmingu v auto režimu
portunix vim-parser auto invoice.pdf --stemming

# explicitní volba backendu
portunix vim-parser auto invoice.pdf --stemming --backend=lucene

# přepsání automatické detekce jazyka
portunix vim-parser auto invoice.pdf --stemming --lang=cs
```

## Výstup

Kmeny se ukládají do sémantické vrstvy VIM JSONu:

```json
{
  "layers": {
    "semantic": {
      "stemming": {
        "language": "cs",
        "backend": "lucene",
        "tokens": [
          {
            "id": "t001",
            "surface": "Faktura",
            "stem": "faktur",
            "offset": { "start": 0, "end": 7 }
          },
          {
            "id": "t002",
            "surface": "služby",
            "stem": "služb",
            "offset": { "start": 12, "end": 18 }
          }
        ],
        "statistics": {
          "totalTokens": 156,
          "uniqueStems": 89,
          "processingTimeMs": 12
        }
      }
    }
  }
}
```

## Známá omezení (backend Lucene)

Lucene používá stemming založený na **odstraňování koncovek**, což má známá omezení pro morfologicky bohaté jazyky jako čeština:

| Případ | Příklad | Omezení |
| ------ | ------- | ------- |
| Vkladné „e" | pes → psa | Odlišné kmeny |
| Alternace souhlásek | kniha → knize | Nezvládá h/z |
| Supletivní tvary | člověk → lidé | Zcela odlišné kmeny |
| Duálové tvary | oko → oči | Nerozpozná |
| Časování sloves | platit → platím | Nekonzistentní kmeny |

Pro vyšší přesnost lze využít budoucí backendy:

- **MorphoDiTa** — skutečná lemmatizace s morfologickou analýzou
- **Hunspell** — slovníkově založená lemmatizace

## Konfigurace

| Parametr | Výchozí | Popis |
| -------- | ------- | ----- |
| Priorita | 15 | Spouští se po fulltextu (10) a metadatech (5) |
| Automaticky povoleno | Ne | Musí být zapnuto přepínačem `--stemming` |
| Výchozí jazyk | cs | Čeština (detekce z metadat, je-li k dispozici) |

## Závislosti

- vim-parser-api
- Apache Lucene Analysis Common 9.10.0

## Pokrytí testy

- **15 unit testů**, které pokrývají:
  - Identitu backendu a podporu jazyků
  - Přesnost stemmingu českých podstatných a přídavných jmen (práh 70 %+)
  - Sledování offsetů tokenů
  - Rychlost zpracování (>10 000 tokenů/s)
  - Zpracování ukázkových textů

## Budoucí backendy

### MorphoDiTa (plánováno)

Skutečná lemmatizace s POS taggingem pro češtinu/slovenštinu:

- URL: <https://ufal.mff.cuni.cz/morphodita>
- Přesnost: ~98 % pro češtinu

### Hunspell (plánováno)

Slovníkově založená lemmatizace:

- URL: <https://hunspell.github.io/>
- Vyžaduje český slovník
