# Contract Schemas (Copy)

Copies of the VIM contract schemas for `ExtractionLayerSchemaTest`, `PrescriptiveLayerSchemaTest`
and `PrescriptiveLayerContractTest`. Do not edit them here.

- **Source**: repository `api`, directory `contract/schemas/`
- **Source commit**: `f5809e5` (2026-10-26)

The test reads the directory named by the environment variable `PTX_CONTRACT_SCHEMAS` when it
is set, and these copies otherwise. To refresh the copies, copy `vim-*.schema.json` from the
source directory and update the commit above.

The contract example `../examples/drawing-definition-file.vim.json` (source directory
`contract/examples/`, same commit) is the round-trip input of `PrescriptiveLayerSchemaTest`.
Refresh it together with the schemas.
