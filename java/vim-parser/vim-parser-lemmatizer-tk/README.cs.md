# vim-parser-lemmatizer-tk

Backend TK Stemmer pro `vim-parser-lemmatizer`. Poskytuje stemming pro 33 jazyků prostřednictvím nativní C++ knihovny TK Stemmer napojené přes JNI.

## Vlastnosti

- **33 podporovaných jazyků** přes engine TK Stemmer / Snowball
- Implementuje rozhraní `LemmatizerBackend` z modulu `vim-parser-lemmatizer`
- Instance `TKStemmer` pro jednotlivé jazyky cachované v `ConcurrentHashMap`
- Tokenizace zohledňující Unicode (`\\p{L}+`) se sledováním offsetů
- Objevováno jako PF4J rozšíření (`@Extension`)

## Identifikace backendu

| Pole | Hodnota |
| ---- | ------- |
| ID backendu | `tk` |
| Název backendu | `TK Stemmer (33 languages)` |
| ID pluginu | `tk-backend-plugin` |

## Podporované jazyky

Volí se kódy ISO 639-1; interně se mapují na názvy jazyků v TKu:

`ar`, `hy`, `eu`, `ca`, `cs`, `da`, `nl`, `en`, `et`, `fi`, `fr`, `de`, `el`, `hi`, `hu`, `id`, `ga`, `it`, `lt`, `ne`, `no`, `pl`, `pt`, `ro`, `ru`, `sr`, `es`, `sv`, `ta`, `tr`, `uk`, `yi`

Pokud je požadován nemapovaný jazyk, backend se vrátí k `english`.

## Použití

```bash
# Explicitní volba backendu TK
portunix vim-parser auto invoice.pdf --stemming --backend=tk

# V kombinaci s explicitním jazykem
portunix vim-parser auto invoice.pdf --stemming --backend=tk --lang=cs
```

## Algoritmus

1. Převeď vstupní jazyk ISO 639-1 na název jazyka v TKu (`cs` → `czech`)
2. Získej (nebo líně vytvoř) instanci `TKStemmer` pro daný jazyk
3. Tokenizuj vstupní text vzorem pro Unicode písmena `\\p{L}+`
4. Pro každý token převeď na lowercase a zavolej `stemmer.stem(word)`
5. Vyšli `StemmedToken` s ID `t001`, `t002`, … plus povrchový tvar, kmen a offset `[start, end]`
6. Agreguj do `StemmedResult` a spočítej statistiky (počet tokenů, doba zpracování)

## Výstup

Backend produkuje stejnou strukturu `StemmedResult`, kterou konzumuje `vim-parser-lemmatizer`:

```json
{
  "language": "cs",
  "backend": "tk",
  "tokens": [
    {
      "id": "t001",
      "surface": "Faktura",
      "stem": "faktur",
      "offset": { "start": 0, "end": 7 }
    }
  ],
  "statistics": {
    "totalTokens": 156,
    "uniqueStems": 89,
    "processingTimeMs": 4
  }
}
```

## Dostupnost

`isAvailable()` vrací `true` pouze tehdy, lze-li načíst nativní knihovnu TK Stemmer (viz `vim-parser-stemmer-tk` pro pokyny k sestavení nativní knihovny). Pokud nativní knihovna chybí, backend se hlásí jako nedostupný a orchestrátor přepne na jiný backend (např. Lucene).

## Závislosti

- vim-parser-api (provided)
- vim-parser-lemmatizer (provided — definuje `LemmatizerBackend`)
- vim-parser-stemmer-tk (JNI obal nad nativní knihovnou TK Stemmer)
- PF4J (provided)
