# Portunix VIM

## Overview

Tooling for **VIM** (Layered Information Model) — semantic extraction of structured information
from documents — for the [Portunix](https://github.com/cassandragargoyle/portunix) ecosystem.

The repository hosts all VIM tooling regardless of language:

| Directory                            | Language | Content                                           |
| ------------------------------------ | -------- | ------------------------------------------------- |
| [`java/vim-parser`](java/vim-parser) | Java 21  | VIM parser (Portunix plugin) and its PF4J modules |
| [`python`](python)                   | Python   | VIM-related tools (planned)                       |
| [`go`](go)                           | Go       | VIM-related CLI and helper tools (planned)        |
| [`docs`](docs)                       | -        | ADRs, issue write-ups and contributing guides     |

For the VIM parser itself (layers, plugins, usage) see
[java/vim-parser/README.md](java/vim-parser/README.md).

## Build

### Prerequisites

`vim-parser-table` depends on `table-detector-api` and `table-detector-pdf`, which live in
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) (`plugins/table-detector`).
Install them into the local Maven repository first:

```bash
git clone https://github.com/cassandragargoyle/portunix-plugins
mvn -f portunix-plugins/plugins/table-detector/pom.xml install -DskipTests
```

The required version is the `table-detector.version` property in
[`java/vim-parser/pom.xml`](java/vim-parser/pom.xml).

### Commands

Run from the repository root — the [`Makefile`](Makefile) wraps the Maven build of
`java/vim-parser`:

```bash
make verify   # clean build with unit and integration tests (same as CI)
make build    # build all modules and plugin ZIPs, skip tests
make dist     # Portunix distribution package in dist/
make help     # list all targets
```

## Related Projects

The repository is linked to [api](https://github.com/cassandragargoyle/api), the home of the
language-independent **contracts** (JSON Schema and gRPC definitions) shared by CassandraGargoyle
and Portunix tools. The VIM data model follows its schemas, e.g. `vim-document.schema.json`,
`vim-extraction.schema.json` and `vim-prescriptive.schema.json` in
[`contract/schemas`](https://github.com/cassandragargoyle/api/tree/main/contract/schemas).

`vim-parser-api` tests validate the model against copies of these schemas in
[`src/test/resources/contract/schemas`](java/vim-parser/vim-parser-api/src/test/resources/contract/schemas).
To test against the canonical schemas of a local api checkout, set `PTX_CONTRACT_SCHEMAS`:

```bash
PTX_CONTRACT_SCHEMAS=<api>/contract/schemas mvn -f java/vim-parser/pom.xml verify
```

## History

`vim-parser` was developed in
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) (`plugins/vim-parser`)
and moved here as a single initial commit (portunix-plugins issue #231); the earlier git history
was not carried over. Issue numbers that refer to portunix-plugins are written as
`portunix-plugins#NN` — see [docs/issues/README.md](docs/issues/README.md#history).

## License

MIT — see [LICENSE](LICENSE).
