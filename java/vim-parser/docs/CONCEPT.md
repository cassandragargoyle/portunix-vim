# Universal "perfect" format for storing information from any file

Design notes: how to store the result of file comprehension so that data can be easily extracted and computed on.

## 1) The real problem (not PDF)

PDF is just a symptom. The real problem is:

> **Loss of semantics during data transfer between systems**

Typical chain:

```text
DB → reporting tool → PDF → human → manual transcription
```

The customer doesn't want PDF. The customer wants:

* values
* relationships
* meaning (what is the column, unit, currency, period)
* proof of origin (that the data really comes from the given document)

---

## 2) Fundamental truth

> **There is no single universal format for all files.**
> **There is a universal inter-layer model for representing information.**

The question therefore is not: "What format stores everything?"
But: **"How to store the result of file comprehension?"**

---

## 3) Ideal goal: machine-readable reconstruction of reality

Using a table in PDF as an example:

The customer doesn't need:

* text
* graphics
* layout

The customer needs:

* **entity**: a table
* **structure**: columns, rows, cells
* **types**: number, currency, date
* **meaning**: e.g. `net_amount_eur`, `vat_rate`, `period`
* **origin**: where in the document it was (page, bounding box)

---

## 4) Principles of the "perfect" format

### 4.1 Separate content, structure and meaning

* content: `123 456`
* structure: cell → column → table
* meaning: `net_amount`, `currency=CZK`, `vat_included=false`

### 4.2 Be layered

A single layer is never enough (raw → layout → structure → semantics).

### 4.3 Preserve provenance

* from which file
* from which page
* from which region of the document

### 4.4 Be bidirectional

* you can run **computations** on the model
* you can (at least approximately) **reconstruct the document** from the model

### 4.5 Handle uncertainty

PDF parsing won't be 100% reliable, therefore:

* confidence score
* extraction method
* alternative interpretations

---

## 5) Architecture proposal: Layered Information Model (VIM)

The "perfect format" is not one thing — it is a **pipeline + data model**.

### Layer 1 — Physical representation (raw / layout)

*What is actually there*

```json
{
  "page": 3,
  "bbox": [120, 340, 560, 410],
  "text": "Total excl. VAT"
}
```

### Layer 2 — Structural model

*What it looks like*

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

### Layer 3 — Semantic model

*What it means*

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

### Layer 4 — Provenance & trust

*Why we believe it*

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

## 6) What format to actually use

| Level      | What it solves  | Typical formats        |
| ---------- | --------------- | ---------------------- |
| Raw file   | original        | PDF / DOCX / XLSX      |
| Layout     | text + position | ALTO / PAGE XML        |
| Structure  | tables          | JSON + Schema          |
| Semantics  | meaning         | JSON-LD / RDF          |
| Computation| aggregation     | tabular model          |
| Storage    | queries         | document DB / graph DB |

---

## 7) Ideal output for the customer

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

## 8) Summary

> **The perfect format does not exist as a single file.**
> What exists is a **layered model + pipeline** that preserves:

* layout
* structure
* semantics
* provenance and trust

---

## 9) Future work

* Document Semantic Model (DSM)
* JSON Schema for layers
* table ontology
* layout → meaning mapping
* handling uncertainty
