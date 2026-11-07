# VIM Parser Plugin Makefile
# Multi-module Maven project

.PHONY: build clean test install dist help all compile plugins

# Plugin info
PLUGIN_NAME := vim-parser
MAVEN_VERSION := $(shell grep -o '<version>[^<]*</version>' pom.xml | head -1 | sed 's/<[^>]*>//g')
JAR_NAME := vim-parser-$(MAVEN_VERSION).jar
CORE_TARGET := vim-parser-core/target

# Plugin ZIP locations
BARCODE_ZIP := vim-parser-barcode/target/barcode-plugin-$(MAVEN_VERSION).zip
FULLTEXT_ZIP := vim-parser-fulltext/target/fulltext-plugin-$(MAVEN_VERSION).zip
GRAPHICS_ZIP := vim-parser-graphics/target/graphics-plugin-$(MAVEN_VERSION).zip
LEMMATIZER_ZIP := vim-parser-lemmatizer/target/lemmatizer-plugin-$(MAVEN_VERSION).zip
LEMMATIZER_TK_ZIP := vim-parser-lemmatizer-tk/target/tk-backend-plugin-$(MAVEN_VERSION).zip
SPAYD_ZIP := vim-parser-spayd/target/spayd-plugin-$(MAVEN_VERSION).zip
TABLE_ZIP := vim-parser-table/target/table-plugin-$(MAVEN_VERSION).zip
TOC_ZIP := vim-parser-toc/target/toc-plugin-$(MAVEN_VERSION).zip
VISUALIZER_ZIP := vim-parser-visualizer/target/visualizer-plugin-$(MAVEN_VERSION).zip

# Default target
all: build

## build: Build all modules and create fat JAR
build:
	@echo "Building vim-parser (multi-module)..."
	mvn clean package -DskipTests
	@echo "Built: $(CORE_TARGET)/$(JAR_NAME)"

## compile: Compile only (no JAR packaging)
compile:
	mvn clean compile

## test: Run tests for all modules
test:
	mvn test

## clean: Clean build artifacts
clean:
	mvn clean
	rm -rf dist/

## install: Install all modules to local Maven repository
install:
	mvn install -DskipTests

## dist: Create distribution package for deployment
dist: build
	@echo "Creating distribution package..."
	@rm -rf dist/
	@mkdir -p dist/plugins/
	@cp $(CORE_TARGET)/$(JAR_NAME) dist/
	@cp plugin.json dist/
	@cp README.md dist/ 2>/dev/null || true
	@cp vim-parser.json.example dist/ 2>/dev/null || true
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

## help: Show this help message
help:
	@echo "VIM Parser Plugin - Build Commands (Multi-Module + PF4J)"
	@echo ""
	@echo "Modules:"
	@echo "  vim-parser-api        - Plugin interfaces and models (+ PF4J)"
	@echo "  vim-parser-core       - Orchestrator and CLI (fat JAR)"
	@echo "  vim-parser-barcode    - Barcode extraction (ZXing) [plugin ZIP]"
	@echo "  vim-parser-fulltext   - Text extraction (PDFBox) [plugin ZIP]"
	@echo "  vim-parser-lemmatizer - Stemming/lemmatization (Lucene) [plugin ZIP]"
	@echo "  vim-parser-spayd      - SPAYD payment parser [plugin ZIP]"
	@echo "  vim-parser-table      - Table extraction (PDFBox) [plugin ZIP]"
	@echo "  vim-parser-toc        - TOC extraction (heading detection) [plugin ZIP]"
	@echo "  vim-parser-visualizer - HTML visualizer [plugin ZIP]"
	@echo ""
	@echo "Distribution structure (after 'make dist'):"
	@echo "  dist/"
	@echo "  ├── vim-parser-*.jar     (launcher + core)"
	@echo "  ├── plugin.json          (Portunix metadata)"
	@echo "  └── plugins/             (PF4J plugins)"
	@echo "      ├── barcode-plugin-*.zip"
	@echo "      ├── fulltext-plugin-*.zip"
	@echo "      ├── lemmatizer-plugin-*.zip"
	@echo "      ├── spayd-plugin-*.zip"
	@echo "      ├── table-plugin-*.zip"
	@echo "      ├── toc-plugin-*.zip"
	@echo "      └── visualizer-plugin-*.zip"
	@echo ""
	@echo "Commands:"
	@sed -n 's/^##//p' $(MAKEFILE_LIST) | column -t -s ':' | sed -e 's/^/  /'
