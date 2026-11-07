# Univerzální „dokonalý" formát pro ukládání informací z libovolného souboru

Dokument obsahuje poznámky k návrhu: jak uložit výsledek porozumění souboru tak, aby šla data snadno extrahovat a počítat.

## 1) Skutečný problém (ne PDF)

PDF je jen symptom. Skutečný problém je:

> **Ztráta sémantiky při přenosu dat mezi systémy**

Typický řetězec:

```text
DB → reportovací nástroj → PDF → člověk → ruční přepis
```

Zákazník nechce PDF. Zákazník chce:

* hodnoty
* vztahy
* význam (co je sloupec, jednotka, měna, období)
* důkaz původu (že data opravdu pochází z daného dokumentu)

---

## 2) Základní pravda

> **Neexistuje jeden univerzální formát pro všechny soubory.**
> **Existuje univerzální mezivrstvový model reprezentace informací.**

Otázka tedy není: „Jaký formát uloží všechno?"
Ale: **„Jak uložit výsledek porozumění souboru?"**

---

## 3) Ideální cíl: strojově čitelná rekonstrukce reality

Na příkladu tabulky v PDF:

Zákazník nepotřebuje:

* text
* grafiku
* layout

Zákazník potřebuje:

* **entitu**: tabulka
* **strukturu**: sloupce, řádky, buňky
* **typy**: číslo, měna, datum
* **význam**: např. `net_amount_eur`, `vat_rate`, `period`
* **původ**: kde v dokumentu to bylo (stránka, bounding box)

---

## 4) Principy „dokonalého" formátu

### 4.1 Oddělit obsah, strukturu a význam

* obsah: `123 456`
* struktura: buňka → sloupec → tabulka
* význam: `net_amount`, `currency=CZK`, `vat_included=false`

### 4.2 Být vrstvený

Jedna vrstva nikdy nestačí (raw → layout → struktura → sémantika).

### 4.3 Uchovat původ (provenance)

* z jakého souboru
* z jaké stránky
* z jakého regionu dokumentu

### 4.4 Být obousměrný

* z modelu jde dělat **výpočty**
* z modelu jde (aspoň přibližně) **zrekonstruovat dokument**

### 4.5 Umět nejistotu

PDF parsing nebude 100% spolehlivý, proto:

* confidence score
* metoda extrakce
* alternativní interpretace

---

## 5) Návrh architektury: vrstvený informační model (VIM)

„Dokonalý formát" není jedna věc, ale **pipeline + datový model**.

### Vrstva 1 – Fyzická reprezentace (raw/layout)

*Co tam opravdu je*

```json
{
  "page": 3,
  "bbox": [120, 340, 560, 410],
  "text": "Celkem bez DPH"
}
```

### Vrstva 2 – Strukturální model

*Jak to vypadá*

```json
{
  "type": "table",
  "columns": ["Položka", "Množství", "Cena", "Celkem"],
  "rows": [
    ["A", "2", "100", "200"],
    ["B", "1", "300", "300"]
  ]
}
```

### Vrstva 3 – Sémantický model

*Co to znamená*

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

### Vrstva 4 – Provenance & důvěra

*Proč tomu věříme*

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

## 6) Jaký formát reálně použít

| Úroveň     | Co řeší       | Typické formáty        |
| ---------- | ------------- | ---------------------- |
| Raw soubor | originál      | PDF / DOCX / XLSX      |
| Layout     | text + pozice | ALTO / PAGE XML        |
| Struktura  | tabulky       | JSON + Schema          |
| Sémantika  | význam        | JSON-LD / RDF          |
| Výpočty    | agregace      | tabulkový model        |
| Uložení    | dotazy        | document DB / graph DB |

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

> **Dokonalý formát neexistuje jako jeden soubor.**
> Existuje **vrstvený model + pipeline**, která uchová:

* layout
* strukturu
* sémantiku
* původ a důvěru

---

## 9) Další rozpracování

* Document Semantic Model (DSM)
* JSON Schema pro vrstvy
* ontologie tabulek
* mapování layout → význam
* práce s nejistotou
