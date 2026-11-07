# vim-parser-visualizer

HTML visualization plugin for VIM Parser.

## Features

- Interactive HTML output with tabbed layers view
- Three layer views: Structural, Physical, Semantic
- Color-coded text blocks
- Payment QR code details
- Semantic entity display
- XSS-safe HTML escaping

## Output

Generates standalone HTML file with embedded CSS and JavaScript.

### Tabs

1. **Structural** - Logical text blocks with headers and content
2. **Physical** - Raw text elements with coordinates
3. **Semantic** - Extracted entities and payments

## Usage

```bash
vim-parser visualize input.vim.json -o output.html
```

Or programmatically:

```java
VimHtmlVisualizerPlugin visualizer = new VimHtmlVisualizerPlugin();
String html = visualizer.visualize(vimDocument);
```

## File Extension

Output files use `.vim.html` extension to indicate VIM visualization.

## Dependencies

- vim-parser-api
- No external dependencies (generates self-contained HTML)
