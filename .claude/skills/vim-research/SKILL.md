---
name: vim-research
description: 'Ovládání VIM Parser Research Lab — výzkum efektivity zpracování dokumentů (fulltext/markdown/vim.json) a odpovídání na otázky pomocí LLM, s důrazem na to, kdy formát NEUMOŽŇUJE korektně odpovědět. Použij když chce uživatel pracovat s vim-parser research labem: přidat vzorek, generovat formáty, položit otázku nad formátem, porovnat formáty nebo vytvořit novou otázku. Příklady: "/vim-research list", "vim-research ask sample1 q01 fulltext", "porovnej formáty pro otázku q01".'
argument-hint: [list|add|generate|ask|compare|new-question ...]
allowed-tools: Read, Glob, Grep, Bash, Write, Edit, AskUserQuestion
user-invocable: true
---

# VIM Parser Research Lab

Skill pro ovládání VIM Parser Research Lab - výzkum efektivity zpracování dokumentů a odpovídání na otázky pomocí LLM.

**Argument:** $ARGUMENTS

## KRITICKÉ PRAVIDLO: NEKECÁM, NEHÁDÁM, NEVYMÝŠLÍM

**Účel tohoto výzkumu je OPAČNÝ než běžné odpovídání na otázky!**

Cílem NENÍ za každou cenu najít odpověď. Cílem JE zjistit:

- Kdy daný formát NEUMOŽŇUJE korektně odpovědět
- Kde dochází ke ztrátě informací (layout, struktura, kontext)
- Které otázky jsou pro daný formát nezodpověditelné

**Pravidla:**

1. **NIKDY nehádat** - pokud odpověď není explicitně v datech, řekni "nelze určit"
2. **NIKDY nepoužívat vnější znalosti** - nepředpokládat "logicky by to mělo být..."
3. **NIKDY nedoplňovat** - pokud chybí mapování label→hodnota, tak chybí
4. **Pokud si nejsem 100% jistý z dat, confidence = low a odpověď = "nelze s jistotou určit"**

**Příklad správného chování:**

- Fulltext obsahuje "Zákazník Adresát" a "Jan Novák Jan Novák"
- ŠPATNĚ: "Zákazník je Jan Novák" (hádám na základě pozice)
- SPRÁVNĚ: "Nelze určit - chybí explicitní mapování, layout ztracen"

Výsledek "nelze odpovědět" je CENNÝ výsledek - ukazuje limit formátu!

## Pracovní adresář

`java/vim-parser/research/`

## Dostupné příkazy

Uživatel může zadat jeden z těchto příkazů:

### `list` nebo `status`

Zobraz přehled:

- Dostupné vzorky (samples) a jejich stav (které formáty jsou vygenerované)
- Dostupné otázky (questions)
- Poslední výsledky (results)

### `add <pdf-path>`

Přidej nový vzorek:

1. Vytvoř adresář v `research/samples/<name>/`
2. Zkopíruj PDF jako `source.pdf`
3. Spusť generování všech formátů

### `generate <sample-name>`

Vygeneruj všechny formáty pro daný vzorek:

- fulltext.txt (portunix text-extractor extract text)
- markdown.md (portunix text-extractor convert)
- vim.json (portunix vim-parser auto)

### `ask <sample-name> <question-id> <format>`

Proveď test:

1. Načti otázku z `questions/<question-id>.md`
2. Načti obsah souboru daného formátu (fulltext/markdown/vim.json)
3. Polož otázku POUZE na základě obsahu tohoto souboru
4. Zapiš strukturovanou odpověď

**DŮLEŽITÉ - NEKECÁM!** Při odpovídání:

- Pracuj POUZE s obsahem daného souboru
- Nepředpokládej nic z jiných formátů
- NEHÁDEJ - pokud není explicitní mapování (label: hodnota), řekni "nelze určit"
- Pokud layout ztratil strukturu a nemůžeš prokázat vztah, je to LIMIT formátu
- Odpověď "nelze s jistotou určit" je VALIDNÍ a CENNÁ odpověď
- Uveď confidence (jistotu) a reasoning (zdůvodnění)
- Confidence=high POUZE když je odpověď 100% prokazatelná z dat

### `compare <sample-name> <question-id>`

Porovnej odpovědi ze všech tří formátů pro danou otázku:

1. Spusť `ask` pro fulltext
2. Spusť `ask` pro markdown
3. Spusť `ask` pro vim.json
4. Vytvoř srovnávací tabulku

### `new-question`

Vytvoř novou otázku:

1. Zeptej se na ID a název otázky
2. Zeptej se na text otázky
3. Zeptej se na očekávaný typ odpovědi
4. Vytvoř soubor v `questions/`

## Formát výsledku testu

Při `ask` vytvoř záznam:

```markdown
# Result: <sample> / <question> / <format>
Date: <timestamp>

## Question
<text otázky>

## Input Format
<fulltext|markdown|vim.json>

## Answer
<odpověď>

## Confidence
<high|medium|low>

## Reasoning
<zdůvodnění jak jsi k odpovědi došel>

## What was found
<co soubor obsahoval relevantního>

## What was missing or confusing
<co chybělo nebo bylo matoucí>
```

## Příklady použití

```text
/vim-research list
/vim-research add ../samples/sample1-anonymized.pdf
/vim-research generate sample1-anonymized
/vim-research ask sample1-anonymized q01-customer-identity fulltext
/vim-research compare sample1-anonymized q01-customer-identity
/vim-research new-question
```
