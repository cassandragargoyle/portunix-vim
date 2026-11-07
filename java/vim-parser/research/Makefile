# VIM Parser Research Lab - Makefile

.PHONY: help generate list-samples list-questions clean

# Default target
help:
	@echo "VIM Parser Research Lab"
	@echo ""
	@echo "Commands:"
	@echo "  make generate SAMPLE=<name>  Generate all formats for a sample"
	@echo "  make list-samples            List available samples"
	@echo "  make list-questions          List available questions"
	@echo "  make clean SAMPLE=<name>     Clean generated files for a sample"
	@echo ""
	@echo "Example:"
	@echo "  make generate SAMPLE=sample1-anonymized"

# Research directory (absolute path)
RESEARCH_DIR := $(shell pwd)

# Generate all formats for a sample
generate:
ifndef SAMPLE
	$(error SAMPLE is required. Usage: make generate SAMPLE=<name>)
endif
	@echo "Generating formats for $(SAMPLE)..."
	@if [ ! -f "$(RESEARCH_DIR)/samples/$(SAMPLE)/source.pdf" ]; then \
		echo "Error: samples/$(SAMPLE)/source.pdf not found"; \
		exit 1; \
	fi
	@echo "  [1/3] Extracting fulltext..."
	@portunix text-extractor extract text "$(RESEARCH_DIR)/samples/$(SAMPLE)/source.pdf" > "$(RESEARCH_DIR)/samples/$(SAMPLE)/fulltext.txt" 2>/dev/null || \
		(echo "Error: fulltext extraction failed" && exit 1)
	@echo "  [2/3] Converting to markdown..."
	@portunix text-extractor convert "$(RESEARCH_DIR)/samples/$(SAMPLE)/source.pdf" -o "$(RESEARCH_DIR)/samples/$(SAMPLE)/markdown.md" 2>/dev/null || \
		(echo "Error: markdown conversion failed" && exit 1)
	@echo "  [3/3] Generating VIM JSON..."
	@portunix vim-parser auto "$(RESEARCH_DIR)/samples/$(SAMPLE)/source.pdf" 2>/dev/null | grep -v "^2[0-9]\{3\}-" | grep -v "^SLF4J" > "$(RESEARCH_DIR)/samples/$(SAMPLE)/vim.json" || \
		(echo "Error: VIM generation failed" && exit 1)
	@echo ""
	@echo "Generated files:"
	@ls -la "$(RESEARCH_DIR)/samples/$(SAMPLE)/"

# List available samples
list-samples:
	@echo "Available samples:"
	@for dir in samples/*/; do \
		name=$$(basename "$$dir"); \
		if [ -f "$$dir/source.pdf" ]; then \
			status="[source.pdf]"; \
			[ -f "$$dir/fulltext.txt" ] && status="$$status [fulltext]"; \
			[ -f "$$dir/markdown.md" ] && status="$$status [markdown]"; \
			[ -f "$$dir/vim.json" ] && status="$$status [vim.json]"; \
			echo "  $$name $$status"; \
		fi \
	done

# List available questions
list-questions:
	@echo "Available questions:"
	@for f in questions/*.md; do \
		if [ -f "$$f" ]; then \
			id=$$(basename "$$f" .md); \
			title=$$(head -1 "$$f" | sed 's/^# //'); \
			echo "  $$id: $$title"; \
		fi \
	done

# Clean generated files for a sample
clean:
ifndef SAMPLE
	$(error SAMPLE is required. Usage: make clean SAMPLE=<name>)
endif
	@echo "Cleaning generated files for $(SAMPLE)..."
	@rm -f "samples/$(SAMPLE)/fulltext.txt"
	@rm -f "samples/$(SAMPLE)/markdown.md"
	@rm -f "samples/$(SAMPLE)/vim.json"
	@echo "Done. Only source.pdf remains."
