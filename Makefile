# Portunix VIM Makefile
# vim-parser is a multi-module Maven project in java/vim-parser

.PHONY: build clean test verify container-verify install dist help all compile plugins run auto book book-figures

# Plugin info
PLUGIN_NAME := vim-parser
VIM_PARSER := java/vim-parser
MVN := mvn -f $(VIM_PARSER)/pom.xml
MAVEN_VERSION := $(shell grep -o '<version>[^<]*</version>' $(VIM_PARSER)/pom.xml | head -1 | sed 's/<[^>]*>//g')
JAR_NAME := vim-parser-$(MAVEN_VERSION).jar
CORE_TARGET := $(VIM_PARSER)/vim-parser-core/target

# Plugin ZIP locations
BARCODE_ZIP := $(VIM_PARSER)/vim-parser-barcode/target/barcode-plugin-$(MAVEN_VERSION).zip
FULLTEXT_ZIP := $(VIM_PARSER)/vim-parser-fulltext/target/fulltext-plugin-$(MAVEN_VERSION).zip
GRAPHICS_ZIP := $(VIM_PARSER)/vim-parser-graphics/target/graphics-plugin-$(MAVEN_VERSION).zip
LEMMATIZER_ZIP := $(VIM_PARSER)/vim-parser-lemmatizer/target/lemmatizer-plugin-$(MAVEN_VERSION).zip
LEMMATIZER_TK_ZIP := $(VIM_PARSER)/vim-parser-lemmatizer-tk/target/tk-backend-plugin-$(MAVEN_VERSION).zip
SPAYD_ZIP := $(VIM_PARSER)/vim-parser-spayd/target/spayd-plugin-$(MAVEN_VERSION).zip
TABLE_ZIP := $(VIM_PARSER)/vim-parser-table/target/table-plugin-$(MAVEN_VERSION).zip
TOC_ZIP := $(VIM_PARSER)/vim-parser-toc/target/toc-plugin-$(MAVEN_VERSION).zip
VISUALIZER_ZIP := $(VIM_PARSER)/vim-parser-visualizer/target/visualizer-plugin-$(MAVEN_VERSION).zip

# Default target
all: build

## build: Build all modules and create fat JAR
build:
	@echo "Building vim-parser (multi-module)..."
	$(MVN) clean package -DskipTests
	@echo "Built: $(CORE_TARGET)/$(JAR_NAME)"

## compile: Compile only (no JAR packaging)
compile:
	$(MVN) clean compile

## test: Run tests for all modules
test:
	$(MVN) test

## verify: Clean build with unit and integration tests (same as CI)
verify:
	$(MVN) clean verify

## container-verify: Same as CI, in a clean Portunix container (needs uv, portunix, gh)
container-verify:
	uv run scripts/container-build.py

## clean: Clean build artifacts
clean:
	$(MVN) clean
	rm -rf dist/

## install: Install all modules to local Maven repository
install:
	$(MVN) install -DskipTests

## dist: Create distribution package for deployment
dist: build
	@echo "Creating distribution package..."
	@rm -rf dist/
	@mkdir -p dist/plugins/
	@cp $(CORE_TARGET)/$(JAR_NAME) dist/
	@cp $(VIM_PARSER)/plugin.json dist/
	@cp $(VIM_PARSER)/README.md dist/ 2>/dev/null || true
	@cp $(VIM_PARSER)/vim-parser.json.example dist/ 2>/dev/null || true
	@echo "Copying plugin ZIPs..."
	@cp $(BARCODE_ZIP) dist/plugins/
	@cp $(FULLTEXT_ZIP) dist/plugins/
	@cp $(GRAPHICS_ZIP) dist/plugins/
	@cp $(LEMMATIZER_ZIP) dist/plugins/
	@cp $(LEMMATIZER_TK_ZIP) dist/plugins/ 2>/dev/null || echo "  (tk-backend-plugin skipped - native libs required)"
	@cp $(SPAYD_ZIP) dist/plugins/
	@cp $(TABLE_ZIP) dist/plugins/
	@cp $(TOC_ZIP) dist/plugins/
	@cp $(VISUALIZER_ZIP) dist/plugins/
	@echo ""
	@echo "Distribution created in dist/"
	@echo "Structure:"
	@echo "  dist/"
	@echo "  ├── $(JAR_NAME)"
	@echo "  ├── plugin.json"
	@echo "  └── plugins/"
	@ls dist/plugins/

## plugins: List available plugins (runs the JAR)
plugins:
	@java -jar $(CORE_TARGET)/$(JAR_NAME) plugins

## run: Run extraction on a test file (usage: make run FILE=input.pdf)
run:
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make run FILE=input.pdf"; \
		exit 1; \
	fi
	java -jar $(CORE_TARGET)/$(JAR_NAME) extract $(FILE)

## auto: Run auto extraction from dist (usage: make auto FILE=input.pdf)
auto: dist
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make auto FILE=input.pdf"; \
		exit 1; \
	fi
	cd dist && java -jar $(JAR_NAME) auto -v $(FILE)

## book: Build the PDF books in docs/book/ in all languages (needs uv)
book:
	uv run scripts/build-book-pdf.py

## book-figures: Generate the SVG figures of the VIM book
book-figures:
	python3 scripts/generate-book-vim-figures.py

## help: Show this help message
help:
	@echo "Portunix VIM - Build Commands"
	@echo ""
	@echo "Projects:"
	@echo "  java/vim-parser - VIM parser, Portunix plugin (Maven multi-module + PF4J)"
	@echo "  python          - Python VIM tools (planned, no make targets yet)"
	@echo "  go              - Go VIM CLI and helper tools (planned, no make targets yet)"
	@echo ""
	@echo "vim-parser modules:"
	@echo "  vim-parser-api        - Plugin interfaces and models (+ PF4J)"
	@echo "  vim-parser-core       - Orchestrator and CLI (fat JAR)"
	@echo "  vim-parser-barcode    - Barcode extraction (ZXing) [plugin ZIP]"
	@echo "  vim-parser-fulltext   - Text extraction (PDFBox) [plugin ZIP]"
	@echo "  vim-parser-graphics   - Graphics extraction [plugin ZIP]"
	@echo "  vim-parser-lemmatizer - Stemming/lemmatization (Lucene) [plugin ZIP]"
	@echo "  vim-parser-spayd      - SPAYD payment parser [plugin ZIP]"
	@echo "  vim-parser-table      - Table extraction (PDFBox) [plugin ZIP]"
	@echo "  vim-parser-toc        - TOC extraction (heading detection) [plugin ZIP]"
	@echo "  vim-parser-visualizer - HTML visualizer [plugin ZIP]"
	@echo ""
	@echo "vim-parser distribution structure (after 'make dist'):"
	@echo "  dist/"
	@echo "  ├── vim-parser-*.jar     (launcher + core)"
	@echo "  ├── plugin.json          (Portunix metadata)"
	@echo "  └── plugins/             (PF4J plugins)"
	@echo "      ├── barcode-plugin-*.zip"
	@echo "      ├── fulltext-plugin-*.zip"
	@echo "      ├── graphics-plugin-*.zip"
	@echo "      ├── lemmatizer-plugin-*.zip"
	@echo "      ├── spayd-plugin-*.zip"
	@echo "      ├── table-plugin-*.zip"
	@echo "      ├── toc-plugin-*.zip"
	@echo "      └── visualizer-plugin-*.zip"
	@echo ""
	@echo "Commands (vim-parser):"
	@sed -n 's/^## \([a-z-]*\): /\1|/p' $(MAKEFILE_LIST) | column -t -s '|' | sed -e 's/^/  /'
