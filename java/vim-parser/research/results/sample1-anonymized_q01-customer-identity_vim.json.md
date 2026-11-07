# Result: sample1-anonymized / q01-customer-identity / vim.json
Date: 2026-01-31 14:20

## Question
Who is the customer on this document?

## Input Format
vim.json

## Answer
**Customer Name:** Ing. Jan Novák

**Address:** Hlavní 123/45, Vinohrady, 120 00 Praha 2

## Confidence
**high**

## Reasoning
VIM formát poskytuje EXPLICITNÍ mapování label→hodnota na dvou úrovních:

### 1. Structural Layer (řádky 3701-3705)
```json
{
  "id" : "block_1",
  "type" : "labeled_content",
  "header" : "Zákazník",
  "content" : [ "Ing. Jan Novák", "Hlavní 123/45", "Vinohrady", "120 00 Praha 2" ]
}
```
Header "Zákazník" je PŘÍMO spojen s content obsahujícím jméno a adresu.

### 2. Semantic Layer (řádky 4012-4020)
```json
{
  "type" : "customer",
  "source_block" : "block_1",
  "fields" : {
    "postal_address" : "120 00 Praha 2",
    "street" : "Hlavní 123/45",
    "name" : "Ing. Jan Novák"
  },
  "confidence" : 0.8
}
```
Extrahovaná entita typu "customer" explicitně identifikuje zákazníka.

### 3. Rozlišení od Adresáta
VIM formát JASNĚ odlišuje:
- `block_1` (header: "Zákazník") → customer entity
- `block_8` (header: "Adresát") → recipient entity

## What was found
- **Explicitní mapování:** header "Zákazník" → content ["Ing. Jan Novák", ...]
- **Sémantická entita:** type="customer", name="Ing. Jan Novák"
- **Oddělení rolí:** customer vs recipient jsou samostatné bloky a entity
- **Bounding boxy:** fyzická vrstva obsahuje souřadnice pro ověření pozice

## What was missing or confusing
- Customer confidence v sémantické vrstvě je 0.8 (ne 1.0), ale strukturální vrstva je jednoznačná
- Email zákazníka není v customer entitě, ale je v account entitě (block_2)

## Comparison with Fulltext
| Aspekt | Fulltext | VIM |
|--------|----------|-----|
| Explicitní mapování | ❌ Ztraceno | ✅ header → content |
| Rozlišení Zákazník/Adresát | ❌ Nelze | ✅ Samostatné bloky |
| Sémantická identifikace | ❌ Není | ✅ type="customer" |
| Confidence odpovědi | LOW | HIGH |
