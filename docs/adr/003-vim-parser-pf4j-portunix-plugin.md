# ADR-003: VIM Parser - PF4J Plugin System and Portunix Integration

**Status**: Accepted
**Date**: 2026-01-31
**Origin**: ADR-013 in [portunix-plugins](https://github.com/cassandragargoyle/portunix-plugins/blob/main/docs/adr/013-vim-parser-pf4j-portunix-plugin.md)
**Deciders**: Zdenek
**Related**: ADR-001, ADR-002, portunix-plugins issue #27

## Context

VIM Parser byl vyvinut jako modulární Java aplikace s více submoduly (api, core, barcode, fulltext, spayd, visualizer). Aktuální stav:

1. **Chybějící Portunix integrace** - vim-parser původně pravděpodobně existoval jako Portunix plugin modul, ale tato integrace se ztratila nebo nebyla dokončena. Aktuálně není jasné, jak vim-parser spustit z Portunix CLI.

2. **Interní plugin systém** - vim-parser používá Java ServiceLoader pro discovery pluginů, což funguje, ale:
   - Žádný lifecycle management
   - Žádné verzování pluginů
   - Složité spouštění (classpath hell)
   - Uživatel nemůže snadno přidávat vlastní pluginy

3. **Distribuce** - aktuálně není jasné jak vim-parser distribuovat:
   - Není fat JAR
   - Není launcher
   - Nelze jednoduše spustit

## Decision

### 1. Použít PF4J pro interní plugin systém vim-parseru

[PF4J (Plugin Framework for Java)](https://github.com/pf4j/pf4j) nahradí přímé použití ServiceLoader.

**Struktura:**
```
vim-parser/
├── vim-parser.jar                    (core + launcher)
└── plugins/
    ├── barcode-plugin-0.1.0.zip
    ├── fulltext-plugin-0.1.0.zip
    ├── spayd-plugin-0.1.0.zip
    ├── visualizer-plugin-0.1.0.zip
    └── custom-plugin-x.y.z.zip       (uživatelské rozšíření)
```

**Výhody PF4J:**
- Lehký framework (~100 KB)
- Plugin lifecycle (load, start, stop, unload)
- Dependency management mezi pluginy
- ZIP/JAR plugin packaging
- Anotace `@Extension` pro discovery
- Kompatibilní s existující architekturou

### 2. Obnovit vim-parser jako Portunix plugin

Vytvořit Portunix plugin wrapper pro vim-parser:

```
portunix/
└── plugins/
    └── vim-parser/
        ├── plugin.json               (Portunix metadata)
        ├── vim-parser.jar            (launcher + core)
        └── plugins/                  (vim-parser interní pluginy)
            ├── barcode-plugin.zip
            └── ...
```

**plugin.json:**
```json
{
  "name": "vim-parser",
  "version": "0.1.0",
  "description": "Layered Information Model parser for semantic extraction",
  "runtime": "java",
  "minJavaVersion": "17",
  "main": "vim-parser.jar",
  "commands": {
    "auto": "Automatic extraction using all plugins",
    "extract": "Manual extraction with specific plugins",
    "visualize": "Generate HTML visualization",
    "plugins": "List available plugins"
  }
}
```

**Použití z Portunix:**
```bash
portunix vim-parser auto invoice.pdf
portunix vim-parser plugins --deps
```

### 3. Fat JAR jako launcher

vim-parser.jar bude fat JAR obsahující:
- vim-parser-core
- vim-parser-api
- PF4J runtime
- Všechny runtime závislosti (Jackson, Picocli)

Pluginy (barcode, fulltext, spayd, visualizer) budou separátní ZIP soubory v `plugins/` adresáři.

## Consequences

### Positive

1. **Jasná distribuce** - jeden JAR + plugins adresář
2. **Rozšiřitelnost** - uživatelé mohou přidávat vlastní pluginy bez rekompilace
3. **Integrace s Portunix** - vim-parser bude dostupný přes `portunix vim-parser`
4. **Hot-reload** - možnost načítat pluginy za běhu (budoucí enhancement)
5. **Verzování** - každý plugin má vlastní verzi, lze upgradovat nezávisle

### Negative

1. **Komplexnější build** - Maven musí vytvářet fat JAR + plugin ZIPy
2. **Větší celková velikost** - PF4J overhead + duplicitní API v každém pluginu
3. **Migrace** - nutné upravit všechny pluginy pro PF4J anotace

### Risks

| Risk | Pravděpodobnost | Dopad | Mitigace |
|------|-----------------|-------|----------|
| PDFBox/ZXing konflikty s PF4J classloaderem | Střední | Vysoký | Testování, parent-first loading |
| Složitý debugging plugin issues | Nízká | Střední | Logging, verbose mode |
| Performance overhead | Nízká | Nízký | PF4J je lightweight |

## Implementation Notes

### Fáze 1: PF4J integrace
1. Přidat PF4J závislost do vim-parser-api
2. Vytvořit `@ExtensionPoint` interface
3. Přidat `@Extension` anotace do pluginů
4. Upravit PluginManager pro PF4J

### Fáze 2: Packaging
1. Maven Shade Plugin pro fat JAR (core)
2. Maven Assembly Plugin pro plugin ZIPy
3. Plugin descriptor (plugin.properties)

### Fáze 3: Portunix integrace
1. Vytvořit plugin.json
2. Deployment skript
3. Dokumentace

## Alternatives Considered

| Alternativa | Důvod zamítnutí |
|-------------|-----------------|
| OSGi | Příliš komplexní pro tento use case |
| JPMS (Java Modules) | PDFBox nemá module-info, složitá migrace |
| Pouze ServiceLoader | Nedostatečný pro uživatelské rozšíření |
| gRPC daemon | Overkill, komplikuje deployment |

---

**References:**
- [PF4J GitHub](https://github.com/pf4j/pf4j)
- [PF4J Documentation](https://pf4j.org/)
- [ADR-002: Modular Architecture](002-vim-parser-modular-architecture.md)
