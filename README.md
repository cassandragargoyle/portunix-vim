# Portunix VIM

## Overview

Tooling for **VIM** (Layered Information Model) — semantic extraction of structured information
from documents — for the [Portunix](https://github.com/cassandragargoyle/portunix) ecosystem.

The repository hosts all VIM tooling regardless of language:

| Directory                          | Language | Content                                              |
| ---------------------------------- | -------- | ---------------------------------------------------- |
| [`java/vim-parser`](java/vim-parser) | Java 21  | VIM parser (Portunix plugin) and its PF4J modules    |
| [`python`](python)                 | Python   | VIM-related tools (planned)                          |
| [`go`](go)                         | Go       | VIM-related CLI and helper tools (planned)           |
| [`docs`](docs)                     | -        | ADRs, issue write-ups and contributing guides        |

## Build

```bash
cd java/vim-parser
mvn clean verify
```

`vim-parser-table` depends on `table-detector-api` and `table-detector-pdf`, which live in
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) (`plugins/table-detector`).
Install them into the local Maven repository first:

```bash
git clone https://github.com/cassandragargoyle/portunix-plugins
mvn -f portunix-plugins/plugins/table-detector/pom.xml install -DskipTests
```

The required version is the `table-detector.version` property in
[`java/vim-parser/pom.xml`](java/vim-parser/pom.xml).

## History

`vim-parser` was developed in
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) (`plugins/vim-parser`)
and moved here with its full git history (portunix-plugins issue #231). Issue numbers in commit
messages before the move (e.g. `feat(159): ...`) refer to portunix-plugins issues — see
[docs/issues/README.md](docs/issues/README.md#history).

## License

MIT — see [LICENSE](LICENSE).
