# VIM Parser Research Lab

Research apparatus for investigating document processing effectiveness and LLM question answering.

## Structure

```text
research/
├── README.md           # This file
├── Makefile            # Automation for generating files
├── samples/            # Test documents
│   └── <sample-name>/
│       ├── source.pdf      # Original PDF
│       ├── fulltext.txt    # Plain text extraction
│       ├── markdown.md     # Markdown conversion
│       └── vim.json        # VIM structured model
├── questions/          # Question definitions
│   └── q<NN>-<name>.md
└── results/            # Test results
    └── <sample>-<question>-<format>.md
```

## Workflow

1. **Add sample**: Place PDF in `samples/<name>/source.pdf`
2. **Generate formats**: Run `make generate SAMPLE=<name>`
3. **Ask question**: Use `/cs:vim-research ask` skill
4. **Record result**: Results stored in `results/`

## Question Format

Each question file in `questions/` follows this format:

```markdown
# Question ID: q01-customer-identity

## Question
Who is the customer on this document?

## Expected Answer Type
- Name
- Address (optional)
- Customer ID (optional)

## Evaluation Criteria
- Correct name identification
- Distinguishing customer from recipient/supplier
```

## Commands

```bash
# Generate all formats for a sample
make generate SAMPLE=sample1

# List available samples
make list-samples

# List available questions
make list-questions
```
