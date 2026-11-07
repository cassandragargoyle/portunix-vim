# vim-parser-graphics

Graphics extraction plugin for VIM Parser using PDFBox. Locates and catalogs all graphic elements in PDF documents: images, paths, lines, rectangles, and shadings.

## Features

- Extracts embedded and inline images from PDF pages
- Detects vector paths, lines, and rectangles from content stream
- Captures shading fills (gradients)
- Computes bounding boxes for all graphic elements
- Reports pixel dimensions and format for images (JPEG, PNG, TIFF, JPEG2000)
- Classifies shapes: IMAGE, LINE, RECT, PATH, SHADING
- Shares extracted graphic data with other plugins via context

## Algorithm

1. Process each page using `PDFGraphicsStreamEngine`
2. **Path accumulation**: `moveTo/lineTo/curveTo/closePath/appendRectangle` accumulate points into `currentPath`
3. **Path emission**: `strokePath()`, `fillPath()`, `fillAndStrokePath()` emit a graphic element and reset path
4. **Shape classification**:
   - `appendRectangle` → RECT
   - 1 line segment → LINE
   - 4 line segments forming a rectangle → RECT
   - All other paths → PATH
5. `drawImage()` → emits IMAGE element immediately
6. `shadingFill()` → emits SHADING element immediately
7. `endPath()` → discards path (clip-only), resets
8. `clip()` → logged only, no element emitted

## Output

### IMAGE element

```json
{
  "id": "graphic_1",
  "page": 1,
  "element_type": "IMAGE",
  "image_type": "embedded",
  "format": "jpeg",
  "width_px": 800,
  "height_px": 600,
  "color_space": "DeviceRGB",
  "bits_per_component": 8,
  "bbox": {"x": 120, "y": 340, "width": 400, "height": 300}
}
```

### RECT element

```json
{
  "id": "graphic_2",
  "page": 1,
  "element_type": "RECT",
  "stroked": true,
  "filled": true,
  "stroke_width": 1.0,
  "bbox": {"x": 50, "y": 100, "width": 200, "height": 150},
  "points": [
    {"x": 50, "y": 100, "type": "move"},
    {"x": 250, "y": 100, "type": "line"},
    {"x": 250, "y": 250, "type": "line"},
    {"x": 50, "y": 250, "type": "line"}
  ]
}
```

### LINE element

```json
{
  "id": "graphic_3",
  "page": 1,
  "element_type": "LINE",
  "stroked": true,
  "stroke_width": 0.5,
  "bbox": {"x": 50, "y": 200, "width": 500, "height": 0},
  "points": [
    {"x": 50, "y": 200, "type": "move"},
    {"x": 550, "y": 200, "type": "line"}
  ]
}
```

## Configuration

- Priority: 25 (runs after barcode extraction)
- Predecessors: `barcode`
- Auto-enabled: yes
- Execution modes: SYNC, ASYNC_THREAD, ASYNC_GRPC

## Dependencies

- vim-parser-api
- PDFBox
