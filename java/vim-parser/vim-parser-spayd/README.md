# vim-parser-spayd

SPAYD (Short Payment Descriptor) parser plugin for VIM Parser.

## What is SPAYD?

SPAYD is a Czech standard for encoding payment information in QR codes. It's widely used on invoices for easy bank transfers.

Format: `SPD*1.0*ACC:CZ6508000000192000145399*AM:1500.00*CC:CZK*X-VS:123456*MSG:Payment`

## Supported Fields

| Field | Description | Example |
| ----- | ----------- | ------- |
| ACC | Bank account (IBAN) | CZ6508000000192000145399 |
| AM | Amount | 1500.00 |
| CC | Currency | CZK |
| X-VS | Variable symbol | 123456 |
| X-SS | Specific symbol | 0001 |
| X-KS | Constant symbol | 0308 |
| MSG | Message | Payment for invoice |
| RN | Recipient name | Company s.r.o. |

## Usage

The plugin processes barcodes extracted by `vim-parser-barcode` and parses those with `contentType: "spayd"`.

## Output

```json
{
  "type": "payment",
  "sourceBarcode": "barcode_1",
  "format": "spayd",
  "fields": {
    "account": "CZ6508000000192000145399",
    "amount": 1500.00,
    "currency": "CZK",
    "variableSymbol": "123456",
    "message": "Payment for invoice"
  },
  "confidence": 0.95
}
```

## Dependencies

- vim-parser-api
- Depends on: barcode plugin (for QR code extraction)
