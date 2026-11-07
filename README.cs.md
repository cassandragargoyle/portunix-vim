# Portunix VIM

## Přehled

Nástroje pro **VIM** (Vrstvený Informační Model / Layered Information Model) — sémantickou extrakci
strukturovaných informací z dokumentů — pro ekosystém
[Portunix](https://github.com/cassandragargoyle/portunix).

Repozitář obsahuje všechny nástroje pro VIM bez ohledu na jazyk:

| Adresář                              | Jazyk   | Obsah                                                 |
| ------------------------------------ | ------- | ----------------------------------------------------- |
| [`java/vim-parser`](java/vim-parser) | Java 21 | VIM parser (Portunix plugin) a jeho PF4J moduly       |
| [`python`](python)                   | Python  | Nástroje související s VIM (plánováno)                |
| [`go`](go)                           | Go      | CLI a pomocné nástroje související s VIM (plánováno)  |
| [`docs`](docs)                       | -       | ADR, popisy issues a průvodci pro přispěvatele        |

Dokumentaci samotného VIM parseru (vrstvy, pluginy, použití) najdete v
[java/vim-parser/README.cs.md](java/vim-parser/README.cs.md).

## Sestavení

### Předpoklady

`vim-parser-table` závisí na `table-detector-api` a `table-detector-pdf`, které jsou v
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) (`plugins/table-detector`).
Nejprve je nainstalujte do lokálního Maven repozitáře:

```bash
git clone https://github.com/cassandragargoyle/portunix-plugins
mvn -f portunix-plugins/plugins/table-detector/pom.xml install -DskipTests
```

Požadovaná verze je uvedena ve vlastnosti `table-detector.version` v
[`java/vim-parser/pom.xml`](java/vim-parser/pom.xml).

### Příkazy

Spouštějte z kořene repozitáře — [`Makefile`](Makefile) obaluje Maven build `java/vim-parser`:

```bash
make verify   # čisté sestavení s unit a integračními testy (stejně jako CI)
make build    # sestavení všech modulů a plugin ZIP balíčků bez testů
make dist     # distribuční balíček pro Portunix v dist/
make help     # výpis všech cílů
```

## Související projekty

Repozitář je navázaný na [api](https://github.com/cassandragargoyle/api), kde jsou jazykově
nezávislé **kontrakty** (JSON Schema a gRPC definice) sdílené nástroji CassandraGargoyle
a Portunix. Datový model VIM odpovídá jeho schématům, např. `vim-document.schema.json`,
`vim-extraction.schema.json` a `vim-prescriptive.schema.json` v
[`contract/schemas`](https://github.com/cassandragargoyle/api/tree/main/contract/schemas).

Testy `vim-parser-api` ověřují model proti kopiím těchto schémat v
[`src/test/resources/contract/schemas`](java/vim-parser/vim-parser-api/src/test/resources/contract/schemas).
Pro test proti kanonickým schématům z lokálního klonu api nastavte `PTX_CONTRACT_SCHEMAS`:

```bash
PTX_CONTRACT_SCHEMAS=<api>/contract/schemas mvn -f java/vim-parser/pom.xml verify
```

## Historie

`vim-parser` byl vyvíjen v
[portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins) (`plugins/vim-parser`)
a přesunut sem jako jeden úvodní commit (issue #231 v portunix-plugins); dřívější git historie
přenesena nebyla. Čísla issues z portunix-plugins se zapisují jako `portunix-plugins#NN` — viz
[docs/issues/README.md](docs/issues/README.md#history).

## Licence

MIT — viz [LICENSE](LICENSE).
