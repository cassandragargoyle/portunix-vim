# vim-parser-lemmatizer-morphodita

Backend MorphoDiTa pro `vim-parser-lemmatizer`. Poskytuje skutečnou lemmatizaci do slovníkového tvaru (nejen stemming) pro češtinu, slovenštinu a angličtinu pomocí morfologického analyzátoru [MorphoDiTa](https://github.com/ufal/morphodita) z ÚFAL (Univerzita Karlova) napojeného přes JNI.

## Vlastnosti

- **Skutečná lemmatizace** — vrací slovníkové tvary, nikoli algoritmické kmeny (`programování` → `programování`, `běželi` → `běžet`)
- **Morfologické značkování** — pro každý token plný POS / morfologický tag
- **Vysoká přesnost** — ~98 % pro češtinu (oproti ~70 % u stemmerů odsekávajících přípony)
- Implementuje rozhraní `LemmatizerBackend` z modulu `vim-parser-lemmatizer`
- Objevováno jako PF4J rozšíření (`@Extension`)

## Identifikace backendu

| Pole | Hodnota |
| ---- | ------- |
| ID backendu | `morphodita` |
| Název backendu | MorphoDiTa Lemmatizer |
| ID pluginu | `morphodita-backend-plugin` |

## Podporované jazyky

| ISO 639-1 | Jazyk | Poznámky |
| --------- | ----- | -------- |
| `cs` | čeština | výchozí model |
| `sk` | slovenština | |
| `en` | angličtina | |

Sada dostupných jazyků závisí na tom, které modely MorphoDiTa jsou nainstalovány.

## Předpoklady

1. **Nativní knihovny** — poskytuje `vim-parser-morpho-morphodita` (sestavení přes `scripts/fetch-natives.sh`)
2. **Jazykové modely** — instalují se přes Portunix:

```bash
portunix install morphodita-model-czech
portunix install morphodita-model-slovak
portunix install morphodita-model-english
```

Bez nainstalovaného modelu backend hlásí daný jazyk jako nepodporovaný a orchestrátor přepne na jiný backend (např. TK nebo Lucene).

## Použití

```bash
# Explicitní volba backendu MorphoDiTa
portunix vim-parser auto invoice.pdf --stemming --backend=morphodita

# V kombinaci s explicitním jazykem
portunix vim-parser auto invoice.pdf --stemming --backend=morphodita --lang=cs
```

## Výstup

Backend produkuje stejnou strukturu `StemmedResult`, kterou konzumuje `vim-parser-lemmatizer`, ale pole `stem` nese skutečné lemmata (a v metadatech může obsahovat morfologický tag):

```json
{
  "language": "cs",
  "backend": "morphodita",
  "tokens": [
    {
      "id": "t001",
      "surface": "Faktury",
      "stem": "faktura",
      "tag": "NNFP1-----A----",
      "offset": { "start": 0, "end": 7 }
    },
    {
      "id": "t002",
      "surface": "běželi",
      "stem": "běžet",
      "tag": "VpMP----R-AA---",
      "offset": { "start": 12, "end": 18 }
    }
  ],
  "statistics": {
    "totalTokens": 156,
    "uniqueStems": 89,
    "processingTimeMs": 5
  }
}
```

## Kdy zvolit MorphoDiTa před jinými backendy

| Scénář | Doporučený backend |
| ------ | ------------------ |
| Nejvyšší přesnost pro češtinu / slovenštinu / angličtinu | **morphodita** |
| 33 jazyků bez nutnosti instalovat model pro každý jazyk | tk |
| Čistě javový fallback bez nativního kódu | lucene |

## Dostupnost

`isAvailable()` vrací `true` pouze tehdy, když:

1. Lze načíst nativní knihovnu MorphoDiTa (`MorphoDiTa.isAvailable()`)
2. Je nainstalován a načtitelný alespoň jeden jazykový model

## Závislosti

- vim-parser-api (provided)
- vim-parser-lemmatizer (provided — definuje `LemmatizerBackend`)
- vim-parser-morpho-morphodita (JNI obal nad nativní knihovnou MorphoDiTa)
- PF4J (provided)

## Licence

- Tento adaptér: MIT License
- Knihovna MorphoDiTa: MPL-2.0
- Jazykové modely: CC BY-NC-SA 4.0

## Zdroje

- [MorphoDiTa GitHub](https://github.com/ufal/morphodita)
- [Dokumentace MorphoDiTa](https://ufal.mff.cuni.cz/morphodita)
- [Jazykové modely (LINDAT/CLARIAH-CZ)](https://lindat.mff.cuni.cz/repository/xmlui/handle/11234/1-5457)
