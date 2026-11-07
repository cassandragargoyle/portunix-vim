# vim-parser-spayd

Plugin pro parsování formátu SPAYD (Short Payment Descriptor) ve VIM Parseru.

## Co je SPAYD?

SPAYD je český standard pro kódování platebních údajů v QR kódech. Široce se používá na fakturách pro snadné bankovní převody.

Formát: `SPD*1.0*ACC:CZ6508000000192000145399*AM:1500.00*CC:CZK*X-VS:123456*MSG:Payment`

## Podporovaná pole

| Pole | Popis | Příklad |
| ---- | ----- | ------- |
| ACC | Bankovní účet (IBAN) | CZ6508000000192000145399 |
| AM | Částka | 1500.00 |
| CC | Měna | CZK |
| X-VS | Variabilní symbol | 123456 |
| X-SS | Specifický symbol | 0001 |
| X-KS | Konstantní symbol | 0308 |
| MSG | Zpráva pro příjemce | Payment for invoice |
| RN | Název příjemce | Company s.r.o. |

## Použití

Plugin zpracovává čárové kódy extrahované pluginem `vim-parser-barcode` a parsuje ty, které mají `contentType: "spayd"`.

## Výstup

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

## Závislosti

- vim-parser-api
- Závisí na: pluginu barcode (pro extrakci QR kódů)
