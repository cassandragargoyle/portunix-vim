/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.visualizer;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.VisualizerPlugin;
import org.cassandragargoyle.portunix.vimparser.model.DocumentMetadata;
import org.cassandragargoyle.portunix.vimparser.model.FulltextContent;
import org.cassandragargoyle.portunix.vimparser.model.GraphicElement;
import org.cassandragargoyle.portunix.vimparser.model.GraphicElementType;
import org.cassandragargoyle.portunix.vimparser.model.SpaydPayment;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.StemmedToken;
import org.cassandragargoyle.portunix.vimparser.model.TableOfContents;
import org.cassandragargoyle.portunix.vimparser.model.TocEntry;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.pf4j.Extension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HTML visualization plugin for VIM documents
 * Generates interactive HTML with tabbed layers view
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@Extension
public class VimHtmlVisualizerPlugin implements VisualizerPlugin, OrchestrationAware
{
	public static final String PLUGIN_ID = "html";

	private static final String[] BLOCK_COLORS =
	{
		"#e3f2fd", "#f3e5f5", "#e8f5e9", "#fff3e0",
		"#fce4ec", "#e0f7fa", "#f1f8e9", "#fbe9e7",
		"#ede7f6", "#e1f5fe", "#f9fbe7", "#efebe9"
	};

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "HTML Visualizer";
	}

	@Override
	public String getVersion()
	{
		return "0.1.0";
	}

	@Override
	public String getOutputFormat()
	{
		return "HTML";
	}

	@Override
	public String getFileExtension()
	{
		return ".vim.html";
	}

	@Override
	public String visualize(VimDocument document)
	{
		StringBuilder html = new StringBuilder();
		String sourceName = document.getDocument().getSource();

		// HTML header
		html.append("<!DOCTYPE html>\n");
		html.append("<html lang=\"cs\">\n");
		html.append("<head>\n");
		html.append("  <meta charset=\"UTF-8\">\n");
		html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("  <title>VIM Visualization - ").append(_escapeHtml(sourceName)).append("</title>\n");
		html.append("  <style>\n");
		html.append(_getStyles());
		html.append("  </style>\n");
		html.append("</head>\n");
		html.append("<body>\n");

		// Header
		html.append("<div class=\"header\">\n");
		html.append("  <h1>VIM Block Visualization</h1>\n");
		html.append("  <div class=\"source-info\">\n");
		html.append("    <span>Source: <strong>").append(_escapeHtml(sourceName)).append("</strong></span>\n");
		html.append("    <span>Pages: <strong>").append(document.getDocument().getPages()).append("</strong></span>\n");
		html.append("  </div>\n");
		_renderMetadataHeader(html, document);
		html.append("</div>\n");

		// Layers tabs
		html.append("<div class=\"tabs\">\n");
		html.append("  <button class=\"tab active\" onclick=\"showLayer('structural')\">Structural</button>\n");
		html.append("  <button class=\"tab\" onclick=\"showLayer('physical')\">Physical</button>\n");
		html.append("  <button class=\"tab\" onclick=\"showLayer('semantic')\">Semantic</button>\n");
		if (document.getLayers().getToc() != null && !document.getLayers().getToc().isEmpty())
		{
			html.append("  <button class=\"tab\" onclick=\"showLayer('toc')\">TOC</button>\n");
		}
		html.append("</div>\n");

		// Structural layer content
		html.append("<div id=\"structural\" class=\"layer-content active\">\n");
		_renderStructuralLayer(html, document);
		html.append("</div>\n");

		// Physical layer content
		html.append("<div id=\"physical\" class=\"layer-content\">\n");
		_renderPhysicalLayer(html, document);
		html.append("</div>\n");

		// Semantic layer content
		html.append("<div id=\"semantic\" class=\"layer-content\">\n");
		_renderSemanticLayer(html, document);
		html.append("</div>\n");

		// TOC layer content
		if (document.getLayers().getToc() != null && !document.getLayers().getToc().isEmpty())
		{
			html.append("<div id=\"toc\" class=\"layer-content\">\n");
			_renderTocLayer(html, document);
			html.append("</div>\n");
		}

		// JavaScript for tabs
		html.append("<script>\n");
		html.append(_getScript());
		html.append("</script>\n");

		html.append("</body>\n");
		html.append("</html>\n");

		return html.toString();
	}

	private void _renderStructuralLayer(StringBuilder html, VimDocument document)
	{
		html.append("<h2>Structural Blocks</h2>\n");

		List<VimDocument.StructuralBlock> blocks = document.getLayers().getStructural().getBlocks();

		if (blocks == null || blocks.isEmpty())
		{
			html.append("<p class=\"no-data\">No structural blocks found</p>\n");
		}
		else
		{
			// Count primary vs auxiliary blocks
			int totalBlocks = blocks.size();
			long auxiliaryCount = blocks.stream()
				.filter(b -> "auxiliary".equals(b.getSignificance()))
				.count();
			long primaryCount = totalBlocks - auxiliaryCount;

			// Toggle button and block count indicator
			if (auxiliaryCount > 0)
			{
				html.append("<div class=\"significance-controls\">\n");
				html.append("  <button class=\"toggle-auxiliary\" onclick=\"toggleAuxiliary()\">");
				html.append("Show auxiliary blocks</button>\n");
				html.append("  <span class=\"block-count\" id=\"blockCount\">");
				html.append("Showing ").append(primaryCount).append("/").append(totalBlocks);
				html.append(" blocks (").append(auxiliaryCount).append(" auxiliary hidden)");
				html.append("</span>\n");
				html.append("</div>\n");
			}

			// Group blocks by page (preserving insertion order)
			Map<Integer, java.util.List<VimDocument.StructuralBlock>> blocksByPage = new LinkedHashMap<>();
			for (VimDocument.StructuralBlock block : blocks)
			{
				blocksByPage.computeIfAbsent(block.getPage(), k -> new java.util.ArrayList<>()).add(block);
			}

			int colorIndex = 0;

			for (Map.Entry<Integer, java.util.List<VimDocument.StructuralBlock>> entry : blocksByPage.entrySet())
			{
				int pageNum = entry.getKey();
				String pageLabel = pageNum > 0 ? "Page " + pageNum : "Unknown Page";
				html.append("<div class=\"page-section\">\n");
				html.append("  <div class=\"page-section-header\">").append(pageLabel).append("</div>\n");
				html.append("  <div class=\"blocks-grid\">\n");

				for (VimDocument.StructuralBlock block : entry.getValue())
				{
					boolean isAuxiliary = "auxiliary".equals(block.getSignificance());
					String color = isAuxiliary ? "#f5f5f5" : BLOCK_COLORS[colorIndex % BLOCK_COLORS.length];
					colorIndex++;

					String blockClass = isAuxiliary ? "block auxiliary-block" : "block";
					String displayStyle = isAuxiliary ? " style=\"background-color: " + color + "; display: none;\"" : " style=\"background-color: " + color + "\"";

					html.append("  <div class=\"").append(blockClass).append("\"").append(displayStyle).append(">\n");
					html.append("    <div class=\"block-header\">\n");
					html.append("      <span class=\"block-id\">").append(_escapeHtml(block.getId())).append("</span>\n");
					if (block.getType() != null && !block.getType().isEmpty())
					{
						html.append("      <span class=\"block-type\">").append(_escapeHtml(block.getType())).append("</span>\n");
					}
					if (block.getPage() > 0)
					{
						html.append("      <span class=\"block-page\">").append(block.getPage()).append("</span>\n");
					}
					html.append("    </div>\n");

					if (block.getHeader() != null && !block.getHeader().isEmpty())
					{
						html.append("    <div class=\"block-title\">").append(_escapeHtml(block.getHeader())).append("</div>\n");
					}

					List<String> content = block.getContent();
					if (content != null && !content.isEmpty())
					{
						html.append("    <div class=\"block-content\">\n");
						for (String line : content)
						{
							html.append("      <div class=\"content-line\">").append(_escapeHtml(line)).append("</div>\n");
						}
						html.append("    </div>\n");
					}

					html.append("  </div>\n");
				}

				html.append("  </div>\n");
				html.append("</div>\n");
			}
		}

		// Structural barcodes
		List<VimDocument.StructuralBarcode> barcodes = document.getLayers().getStructural().getBarcodes();
		if (barcodes != null && !barcodes.isEmpty())
		{
			html.append("<h2>Structural Barcodes</h2>\n");
			html.append("<div class=\"blocks-grid\">\n");

			for (VimDocument.StructuralBarcode barcode : barcodes)
			{
				html.append("<div class=\"block barcode-block\">\n");
				html.append("  <div class=\"block-header\">\n");
				html.append("    <span class=\"block-id\">").append(_escapeHtml(barcode.getId())).append("</span>\n");
				html.append("    <span class=\"barcode-format\">").append(_escapeHtml(barcode.getFormat())).append("</span>\n");
				html.append("  </div>\n");
				html.append("  <div class=\"block-title\">").append(_escapeHtml(barcode.getContentType().toUpperCase())).append("</div>\n");
				html.append("  <div class=\"block-content\">\n");
				html.append("    <div class=\"content-line\">Confidence: ").append(String.format("%.0f%%", barcode.getConfidence() * 100)).append("</div>\n");

				List<String> nearbyBlocks = barcode.getNearbyBlocks();
				if (nearbyBlocks != null && !nearbyBlocks.isEmpty())
				{
					html.append("    <div class=\"content-line\">Nearby: ");
					html.append(String.join(", ", nearbyBlocks));
					html.append("</div>\n");
				}

				html.append("  </div>\n");
				html.append("</div>\n");
			}

			html.append("</div>\n");
		}
	}

	private void _renderPhysicalLayer(StringBuilder html, VimDocument document)
	{
		html.append("<h2>Physical Blocks (Bounding Boxes)</h2>\n");

		List<VimDocument.PhysicalBlock> blocks = document.getLayers().getPhysical().getBlocks();

		if (blocks == null || blocks.isEmpty())
		{
			html.append("<p class=\"no-data\">No physical blocks found</p>\n");
		}
		else
		{
			html.append("<table class=\"data-table\">\n");
			html.append("<thead><tr><th>ID</th><th>Page</th><th>X</th><th>Y</th><th>Width</th><th>Height</th><th>Elements</th></tr></thead>\n");
			html.append("<tbody>\n");

			for (VimDocument.PhysicalBlock block : blocks)
			{
				html.append("<tr>\n");
				html.append("  <td>").append(_escapeHtml(block.getId())).append("</td>\n");
				html.append("  <td>").append(block.getPage()).append("</td>\n");
				if (block.getBbox() != null)
				{
					html.append("  <td>").append(String.format("%.1f", block.getBbox().getX())).append("</td>\n");
					html.append("  <td>").append(String.format("%.1f", block.getBbox().getY())).append("</td>\n");
					html.append("  <td>").append(String.format("%.1f", block.getBbox().getWidth())).append("</td>\n");
					html.append("  <td>").append(String.format("%.1f", block.getBbox().getHeight())).append("</td>\n");
				}
				else
				{
					html.append("  <td>-</td><td>-</td><td>-</td><td>-</td>\n");
				}
				int elemCount = block.getTextElements() != null ? block.getTextElements().size() : 0;
				html.append("  <td>").append(elemCount).append("</td>\n");
				html.append("</tr>\n");
			}

			html.append("</tbody>\n");
			html.append("</table>\n");
		}

		// Physical barcodes
		List<VimDocument.PhysicalBarcode> barcodes = document.getLayers().getPhysical().getBarcodes();
		if (barcodes != null && !barcodes.isEmpty())
		{
			html.append("<h2>Physical Barcodes</h2>\n");
			html.append("<table class=\"data-table\">\n");
			html.append("<thead><tr><th>ID</th><th>Page</th><th>Format</th><th>X</th><th>Y</th><th>Size</th><th>Content</th></tr></thead>\n");
			html.append("<tbody>\n");

			for (VimDocument.PhysicalBarcode barcode : barcodes)
			{
				html.append("<tr>\n");
				html.append("  <td>").append(_escapeHtml(barcode.getId())).append("</td>\n");
				html.append("  <td>").append(barcode.getPage()).append("</td>\n");
				html.append("  <td><span class=\"barcode-format\">").append(_escapeHtml(barcode.getFormat())).append("</span></td>\n");
				if (barcode.getBbox() != null)
				{
					html.append("  <td>").append(String.format("%.1f", barcode.getBbox().getX())).append("</td>\n");
					html.append("  <td>").append(String.format("%.1f", barcode.getBbox().getY())).append("</td>\n");
					html.append("  <td>").append(String.format("%.0fx%.0f", barcode.getBbox().getWidth(), barcode.getBbox().getHeight())).append("</td>\n");
				}
				else
				{
					html.append("  <td>-</td><td>-</td><td>-</td>\n");
				}
				html.append("  <td class=\"barcode-content\">").append(_escapeHtml(barcode.getRawContent())).append("</td>\n");
				html.append("</tr>\n");
			}

			html.append("</tbody>\n");
			html.append("</table>\n");
		}

		// Physical tables
		List<VimDocument.PhysicalTable> tables = document.getLayers().getPhysical().getTables();
		if (tables != null && !tables.isEmpty())
		{
			html.append("<h2>Extracted Tables</h2>\n");

			for (VimDocument.PhysicalTable table : tables)
			{
				html.append("<div class=\"extracted-table\">\n");
				html.append("  <div class=\"table-header\">\n");
				html.append("    <span class=\"table-id\">").append(_escapeHtml(table.getId())).append("</span>\n");
				html.append("    <span class=\"table-info\">Page ").append(table.getPage()).append("</span>\n");
				html.append("    <span class=\"table-info\">").append(table.getRows()).append(" × ").append(table.getColumns()).append("</span>\n");
				html.append("    <span class=\"table-confidence\">").append(String.format("%.0f%%", table.getConfidence() * 100)).append("</span>\n");
				html.append("  </div>\n");

				// Render table content
				html.append("  <table class=\"extracted-data-table\">\n");

				for (int row = 0; row < table.getRows(); row++)
				{
					html.append("    <tr>\n");
					for (int col = 0; col < table.getColumns(); col++)
					{
						VimDocument.PhysicalTableCell cell = table.getCell(row, col);
						String cellText = cell != null ? cell.getText() : "";
						boolean isHeader = cell != null && cell.isHeader();

						String tag = isHeader ? "th" : "td";
						html.append("      <").append(tag).append(">");
						html.append(_escapeHtml(cellText));
						html.append("</").append(tag).append(">\n");
					}
					html.append("    </tr>\n");
				}

				html.append("  </table>\n");
				html.append("</div>\n");
			}
		}

		// Graphic elements
		List<GraphicElement> graphics = document.getLayers().getPhysical().getGraphics();
		if (graphics != null && !graphics.isEmpty())
		{
			html.append("<h2>Graphic Elements</h2>\n");
			html.append("<table class=\"data-table\">\n");
			html.append("<thead><tr><th>ID</th><th>Page</th><th>Element Type</th><th>Details</th><th>Position</th></tr></thead>\n");
			html.append("<tbody>\n");

			for (GraphicElement graphic : graphics)
			{
				html.append("<tr>\n");
				html.append("  <td>").append(_escapeHtml(graphic.getId())).append("</td>\n");
				html.append("  <td>").append(graphic.getPage()).append("</td>\n");
				html.append("  <td><span class=\"image-type\">").append(graphic.getElementType()).append("</span></td>\n");

				// Details column depends on element type
				html.append("  <td>");
				if (graphic.getElementType() == GraphicElementType.IMAGE)
				{
					String imageType = graphic.getImageType() != null ? graphic.getImageType() : "";
					String format = graphic.getFormat() != null ? graphic.getFormat().toUpperCase() : "";
					html.append(imageType).append(" ").append(format);
					html.append(" ").append(graphic.getWidthPx()).append("×").append(graphic.getHeightPx()).append("px");
					if (graphic.getColorSpace() != null)
					{
						html.append(" ").append(_escapeHtml(graphic.getColorSpace()));
						if (graphic.getBitsPerComponent() > 0)
						{
							html.append(" (").append(graphic.getBitsPerComponent()).append("-bit)");
						}
					}
				}
				else if (graphic.getElementType() == GraphicElementType.SHADING)
				{
					html.append("shading: ").append(_escapeHtml(graphic.getShadingName() != null ? graphic.getShadingName() : ""));
				}
				else
				{
					// LINE, RECT, PATH
					if (graphic.isStroked()) html.append("stroked ");
					if (graphic.isFilled()) html.append("filled ");
					if (graphic.getStrokeWidth() > 0)
					{
						html.append(String.format("(%.1fpt)", graphic.getStrokeWidth()));
					}
					if (graphic.getPoints() != null)
					{
						html.append(" [").append(graphic.getPoints().size()).append(" points]");
					}
				}
				html.append("</td>\n");

				// Position column
				if (graphic.getBbox() != null)
				{
					html.append("  <td>").append(String.format("(%.1f, %.1f) %.0f×%.0f",
															   graphic.getBbox().getX(), graphic.getBbox().getY(),
															   graphic.getBbox().getWidth(), graphic.getBbox().getHeight())).append("</td>\n");
				}
				else
				{
					html.append("  <td>-</td>\n");
				}
				html.append("</tr>\n");
			}

			html.append("</tbody>\n");
			html.append("</table>\n");
		}

		// Fulltext content
		_renderFulltextSection(html, document);
	}

	private void _renderSemanticLayer(StringBuilder html, VimDocument document)
	{
		html.append("<h2>Semantic Entities</h2>\n");

		List<VimDocument.SemanticEntity> entities = document.getLayers().getSemantic().getEntities();

		if (entities == null || entities.isEmpty())
		{
			html.append("<p class=\"no-data\">No semantic entities found</p>\n");
		}
		else
		{
			html.append("<div class=\"entities-list\">\n");

			for (VimDocument.SemanticEntity entity : entities)
			{
				html.append("<div class=\"entity\">\n");
				html.append("  <div class=\"entity-header\">\n");
				html.append("    <span class=\"entity-type\">").append(_escapeHtml(entity.getType().toUpperCase())).append("</span>\n");
				html.append("    <span class=\"entity-source\">from ").append(_escapeHtml(entity.getSourceBlock())).append("</span>\n");
				html.append("    <span class=\"entity-confidence\">").append(String.format("%.0f%%", entity.getConfidence() * 100)).append("</span>\n");
				html.append("  </div>\n");

				Map<String, String> fields = entity.getFields();
				if (fields != null && !fields.isEmpty())
				{
					html.append("  <div class=\"entity-fields\">\n");
					for (Map.Entry<String, String> field : fields.entrySet())
					{
						html.append("    <div class=\"field\"><span class=\"field-name\">").append(_escapeHtml(field.getKey())).append(":</span> ");
						html.append("<span class=\"field-value\">").append(_escapeHtml(field.getValue())).append("</span></div>\n");
					}
					html.append("  </div>\n");
				}

				html.append("</div>\n");
			}

			html.append("</div>\n");
		}

		// Payments
		List<SpaydPayment> payments = document.getLayers().getSemantic().getPayments();
		if (payments != null && !payments.isEmpty())
		{
			html.append("<h2>Payment QR Codes</h2>\n");
			html.append("<div class=\"entities-list\">\n");

			for (SpaydPayment payment : payments)
			{
				html.append("<div class=\"entity payment\">\n");
				html.append("  <div class=\"entity-header\">\n");
				html.append("    <span class=\"entity-type payment-type\">PAYMENT</span>\n");
				html.append("    <span class=\"payment-format\">").append(_escapeHtml(payment.getFormat().toUpperCase())).append("</span>\n");
				html.append("    <span class=\"entity-source\">from ").append(_escapeHtml(payment.getSourceBarcode())).append("</span>\n");
				html.append("    <span class=\"entity-confidence\">").append(String.format("%.0f%%", payment.getConfidence() * 100)).append("</span>\n");
				html.append("  </div>\n");

				SpaydPayment.SpaydFields fields = payment.getFields();
				if (fields != null)
				{
					html.append("  <div class=\"payment-details\">\n");

					if (fields.getAccount() != null && !fields.getAccount().isEmpty())
					{
						html.append("    <div class=\"payment-field\"><span class=\"field-name\">Account:</span> ");
						html.append("<span class=\"field-value account\">").append(_escapeHtml(fields.getAccount())).append("</span></div>\n");
					}

					if (fields.getAmount() != null && fields.getAmount() > 0)
					{
						html.append("    <div class=\"payment-field amount-field\"><span class=\"field-name\">Amount:</span> ");
						html.append("<span class=\"field-value amount\">").append(String.format("%,.2f", fields.getAmount()));
						html.append(" ").append(_escapeHtml(fields.getCurrency())).append("</span></div>\n");
					}

					if (fields.getVariableSymbol() != null && !fields.getVariableSymbol().isEmpty())
					{
						html.append("    <div class=\"payment-field\"><span class=\"field-name\">Variable Symbol:</span> ");
						html.append("<span class=\"field-value\">").append(_escapeHtml(fields.getVariableSymbol())).append("</span></div>\n");
					}

					if (fields.getMessage() != null && !fields.getMessage().isEmpty())
					{
						html.append("    <div class=\"payment-field\"><span class=\"field-name\">Message:</span> ");
						html.append("<span class=\"field-value\">").append(_escapeHtml(fields.getMessage())).append("</span></div>\n");
					}

					if (fields.getRecipientName() != null && !fields.getRecipientName().isEmpty())
					{
						html.append("    <div class=\"payment-field\"><span class=\"field-name\">Recipient:</span> ");
						html.append("<span class=\"field-value\">").append(_escapeHtml(fields.getRecipientName())).append("</span></div>\n");
					}

					html.append("  </div>\n");
				}

				html.append("</div>\n");
			}

			html.append("</div>\n");
		}

		// Stemming/Lemmatization results
		_renderStemmingSection(html, document);
	}

	private void _renderTocLayer(StringBuilder html, VimDocument document)
	{
		TableOfContents toc = document.getLayers().getToc();

		html.append("<h2>Table of Contents</h2>\n");
		html.append("<div class=\"toc-container\">\n");
		html.append("  <div class=\"toc-stats\">\n");
		html.append("    <span>Entries: <strong>").append(toc.size()).append("</strong></span>\n");
		if (toc.getTocPage() != null)
		{
			html.append("    <span>TOC Page: <strong>").append(toc.getTocPage()).append("</strong></span>\n");
		}
		long explicitCount = toc.getEntries().stream()
				.filter(e -> "explicit".equals(e.getSource())).count();
		long heuristicCount = toc.size() - explicitCount;
		if (explicitCount > 0)
		{
			html.append("    <span>Explicit: <strong>").append(explicitCount).append("</strong></span>\n");
		}
		if (heuristicCount > 0)
		{
			html.append("    <span>Heuristic: <strong>").append(heuristicCount).append("</strong></span>\n");
		}
		html.append("  </div>\n");
		html.append("  <div class=\"toc-entries\">\n");

		for (TocEntry entry : toc.getEntries())
		{
			String levelClass = "toc-level-" + Math.min(entry.getLevel(), 3);
			html.append("    <div class=\"toc-entry ").append(levelClass).append("\">\n");
			html.append("      <span class=\"toc-level\">L").append(entry.getLevel()).append("</span>\n");
			html.append("      <span class=\"toc-title\">").append(_escapeHtml(entry.getTitle())).append("</span>\n");
			html.append("      <span class=\"toc-dots\"></span>\n");
			html.append("      <span class=\"toc-page\">").append(entry.getPage()).append("</span>\n");
			html.append("      <span class=\"toc-meta\">\n");
			if (entry.getSource() != null)
			{
				String sourceClass = "explicit".equals(entry.getSource()) ? "toc-source-explicit" : "toc-source-heuristic";
				html.append("        <span class=\"toc-source ").append(sourceClass).append("\">").append(entry.getSource()).append("</span>\n");
			}
			html.append("        <span class=\"toc-block-ref\">").append(_escapeHtml(entry.getBlockId())).append("</span>\n");
			html.append("        <span class=\"toc-confidence\">").append(String.format("%.0f%%", entry.getConfidence() * 100)).append("</span>\n");
			html.append("      </span>\n");
			html.append("    </div>\n");
		}

		html.append("  </div>\n");
		html.append("</div>\n");
	}

	private void _renderStemmingSection(StringBuilder html, VimDocument document)
	{
		StemmedResult stemming = document.getLayers().getSemantic().getStemming();
		if (stemming == null || stemming.getTokens() == null || stemming.getTokens().isEmpty())
		{
			return;
		}

		html.append("<h2>Stemming / Lemmatization</h2>\n");
		html.append("<div class=\"stemming-container\">\n");

		// Statistics header
		html.append("  <div class=\"stemming-header\">\n");
		html.append("    <span class=\"stemming-info\"><span class=\"stemming-label\">Language:</span> ");
		html.append("<strong>").append(_escapeHtml(stemming.getLanguage())).append("</strong></span>\n");
		html.append("    <span class=\"stemming-info\"><span class=\"stemming-label\">Backend:</span> ");
		html.append("<strong>").append(_escapeHtml(stemming.getBackend())).append("</strong></span>\n");

		if (stemming.getStatistics() != null)
		{
			StemmedResult.Statistics stats = stemming.getStatistics();
			html.append("    <span class=\"stemming-info\"><span class=\"stemming-label\">Tokens:</span> ");
			html.append("<strong>").append(stats.getTotalTokens()).append("</strong></span>\n");
			html.append("    <span class=\"stemming-info\"><span class=\"stemming-label\">Unique stems:</span> ");
			html.append("<strong>").append(stats.getUniqueStems()).append("</strong></span>\n");
			html.append("    <span class=\"stemming-info\"><span class=\"stemming-label\">Time:</span> ");
			html.append("<strong>").append(stats.getProcessingTimeMs()).append("ms</strong></span>\n");
		}

		html.append("  </div>\n");

		// Token table with limited display
		List<StemmedToken> tokens = stemming.getTokens();
		int displayLimit = 100;
		int totalTokens = tokens.size();

		html.append("  <div class=\"stemming-tokens\">\n");

		if (totalTokens > displayLimit)
		{
			html.append("    <p class=\"token-notice\">Showing first ").append(displayLimit);
			html.append(" of ").append(totalTokens).append(" tokens</p>\n");
		}

		html.append("    <table class=\"data-table stemming-table\">\n");
		html.append("    <thead><tr><th>#</th><th>Surface</th><th>Stem</th><th>Offset</th></tr></thead>\n");
		html.append("    <tbody>\n");

		int count = 0;
		for (StemmedToken token : tokens)
		{
			if (count >= displayLimit)
			{
				break;
			}

			html.append("      <tr>\n");
			html.append("        <td class=\"token-num\">").append(count + 1).append("</td>\n");
			html.append("        <td class=\"token-surface\">").append(_escapeHtml(token.getSurface())).append("</td>\n");
			html.append("        <td class=\"token-stem\">").append(_escapeHtml(token.getStem())).append("</td>\n");

			if (token.getOffset() != null)
			{
				html.append("        <td class=\"token-offset\">");
				html.append(token.getOffset().getStart()).append("-").append(token.getOffset().getEnd());
				html.append("</td>\n");
			}
			else
			{
				html.append("        <td class=\"token-offset\">-</td>\n");
			}

			html.append("      </tr>\n");
			count++;
		}

		html.append("    </tbody>\n");
		html.append("    </table>\n");
		html.append("  </div>\n");
		html.append("</div>\n");
	}

	private void _renderMetadataHeader(StringBuilder html, VimDocument document)
	{
		DocumentMetadata metadata = document.getDocument().getMetadata();
		if (metadata == null)
		{
			return;
		}

		html.append("  <div class=\"metadata-info\">\n");

		if (metadata.getEncodingFormat() != null)
		{
			html.append("    <span class=\"meta-item\"><span class=\"meta-label\">Format:</span> ");
			html.append(_escapeHtml(metadata.getEncodingFormat())).append("</span>\n");
		}

		if (metadata.getContentSize() != null)
		{
			html.append("    <span class=\"meta-item\"><span class=\"meta-label\">Size:</span> ");
			html.append(_formatFileSize(metadata.getContentSize())).append("</span>\n");
		}

		if (metadata.getInLanguage() != null)
		{
			html.append("    <span class=\"meta-item\"><span class=\"meta-label\">Language:</span> ");
			html.append(_escapeHtml(metadata.getInLanguage())).append("</span>\n");
		}

		if (metadata.getCreator() != null)
		{
			html.append("    <span class=\"meta-item\"><span class=\"meta-label\">Creator:</span> ");
			html.append(_escapeHtml(metadata.getCreator())).append("</span>\n");
		}

		if (metadata.getTitle() != null)
		{
			html.append("    <span class=\"meta-item\"><span class=\"meta-label\">Title:</span> ");
			html.append(_escapeHtml(metadata.getTitle())).append("</span>\n");
		}

		if (metadata.getDateCreated() != null)
		{
			html.append("    <span class=\"meta-item\"><span class=\"meta-label\">Created:</span> ");
			html.append(_escapeHtml(metadata.getDateCreated())).append("</span>\n");
		}

		if (metadata.getSha256() != null)
		{
			html.append("    <span class=\"meta-item hash\"><span class=\"meta-label\">SHA-256:</span> ");
			html.append("<code>").append(_escapeHtml(metadata.getSha256())).append("</code></span>\n");
		}

		html.append("  </div>\n");
	}

	private void _renderFulltextSection(StringBuilder html, VimDocument document)
	{
		FulltextContent fulltext = document.getLayers().getPhysical().getFulltext();
		if (fulltext == null || fulltext.getContent() == null)
		{
			return;
		}

		html.append("<h2>Fulltext Content</h2>\n");
		html.append("<div class=\"fulltext-container\">\n");
		html.append("  <div class=\"fulltext-stats\">\n");
		html.append("    <span>Words: <strong>").append(fulltext.getWordCount()).append("</strong></span>\n");
		html.append("    <span>Characters: <strong>").append(fulltext.getCharacterCount()).append("</strong></span>\n");
		html.append("  </div>\n");
		html.append("  <div class=\"fulltext-content\">\n");
		html.append("    <pre>").append(_escapeHtml(fulltext.getContent())).append("</pre>\n");
		html.append("  </div>\n");
		html.append("</div>\n");
	}

	private String _formatFileSize(long bytes)
	{
		if (bytes < 1024)
		{
			return bytes + " B";
		}
		if (bytes < 1024 * 1024)
		{
			return String.format("%.1f KB", bytes / 1024.0);
		}
		if (bytes < 1024 * 1024 * 1024)
		{
			return String.format("%.1f MB", bytes / (1024.0 * 1024));
		}
		return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
	}

	private String _getStyles()
	{
		return """
			* { box-sizing: border-box; margin: 0; padding: 0; }
			body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; color: #333; line-height: 1.6; }
			.header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 2rem; text-align: center; }
			.header h1 { font-size: 1.8rem; margin-bottom: 0.5rem; }
			.source-info { font-size: 0.9rem; opacity: 0.9; }
			.source-info span { margin: 0 1rem; }
			.metadata-info { margin-top: 1rem; padding-top: 1rem; border-top: 1px solid rgba(255,255,255,0.3); display: flex; flex-wrap: wrap; justify-content: center; gap: 0.5rem 1.5rem; font-size: 0.85rem; opacity: 0.95; }
			.meta-item { display: inline-flex; align-items: center; gap: 0.3rem; }
			.meta-label { opacity: 0.8; }
			.meta-item.hash { width: 100%; justify-content: center; margin-top: 0.5rem; }
			.meta-item.hash code { font-family: monospace; font-size: 0.75rem; background: rgba(0,0,0,0.2); padding: 0.2rem 0.5rem; border-radius: 4px; }
			.tabs { display: flex; justify-content: center; background: #fff; border-bottom: 2px solid #e0e0e0; padding: 0.5rem; }
			.tab { background: none; border: none; padding: 0.75rem 1.5rem; cursor: pointer; font-size: 1rem; color: #666; transition: all 0.3s; border-radius: 4px 4px 0 0; }
			.tab:hover { background: #f0f0f0; }
			.tab.active { color: #667eea; border-bottom: 3px solid #667eea; font-weight: 600; }
			.layer-content { display: none; padding: 2rem; max-width: 1200px; margin: 0 auto; }
			.layer-content.active { display: block; }
			h2 { color: #444; margin-bottom: 1.5rem; padding-bottom: 0.5rem; border-bottom: 2px solid #e0e0e0; margin-top: 2rem; }
			h2:first-child { margin-top: 0; }
			.blocks-grid { display: flex; flex-direction: column; gap: 1rem; }
			.block { border-radius: 8px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
			.block-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; font-size: 0.8rem; }
			.block-id { font-weight: 600; color: #555; }
			.block-type { background: #667eea; color: white; padding: 0.1rem 0.5rem; border-radius: 4px; font-size: 0.7rem; }
			.block-page { background: #43a047; color: white; padding: 0.1rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 600; margin-left: auto; }
			.page-section { margin-bottom: 2rem; }
			.page-section-header { font-size: 1rem; font-weight: 600; color: #555; padding: 0.5rem 0; margin-bottom: 1rem; border-bottom: 2px solid #667eea; display: flex; align-items: center; gap: 0.5rem; }
			.block-title { font-weight: 700; font-size: 1.1rem; color: #333; margin-bottom: 0.75rem; padding-bottom: 0.5rem; border-bottom: 1px solid rgba(0,0,0,0.1); }
			.block-content { font-size: 0.9rem; }
			.content-line { padding: 0.2rem 0; color: #555; }
			.no-data { color: #888; font-style: italic; padding: 2rem; text-align: center; }
			.data-table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); margin-bottom: 1rem; }
			.data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e0e0e0; }
			.data-table th { background: #667eea; color: white; font-weight: 600; }
			.data-table tr:hover { background: #f9f9f9; }
			.entities-list { display: flex; flex-direction: column; gap: 1rem; }
			.entity { background: white; border-radius: 8px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
			.entity-header { display: flex; align-items: center; gap: 1rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
			.entity-type { background: #4caf50; color: white; padding: 0.25rem 0.75rem; border-radius: 4px; font-weight: 600; font-size: 0.85rem; }
			.entity-source { color: #888; font-size: 0.85rem; }
			.entity-confidence { margin-left: auto; color: #667eea; font-weight: 600; }
			.entity-fields { padding-left: 0.5rem; border-left: 3px solid #e0e0e0; }
			.field { padding: 0.25rem 0; }
			.field-name { color: #888; font-size: 0.85rem; }
			.field-value { color: #333; }
			.barcode-block { background: linear-gradient(135deg, #fff9c4 0%, #fff59d 100%); }
			.barcode-format { background: #ff9800; color: white; padding: 0.1rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }
			.barcode-content { font-family: monospace; font-size: 0.8rem; word-break: break-all; white-space: normal; max-width: 400px; }
			.image-type { background: #9c27b0; color: white; padding: 0.1rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }
			.image-format { background: #e91e63; color: white; padding: 0.1rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }
			.payment { background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%); border-left: 4px solid #4caf50; }
			.payment-type { background: #2e7d32; }
			.payment-format { background: #43a047; color: white; padding: 0.1rem 0.5rem; border-radius: 4px; font-size: 0.75rem; }
			.payment-details { padding: 0.5rem; background: rgba(255,255,255,0.7); border-radius: 4px; }
			.payment-field { padding: 0.3rem 0; display: flex; gap: 0.5rem; }
			.amount-field { font-size: 1.1rem; }
			.amount { font-weight: 700; color: #2e7d32; }
			.account { font-family: monospace; font-size: 0.9rem; }
			.extracted-table { background: white; border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
			.table-header { display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; padding-bottom: 0.75rem; border-bottom: 1px solid #e0e0e0; }
			.table-id { font-weight: 700; color: #333; font-size: 1.1rem; }
			.table-info { color: #666; font-size: 0.85rem; padding: 0.2rem 0.6rem; background: #f0f0f0; border-radius: 4px; }
			.table-confidence { margin-left: auto; color: #667eea; font-weight: 600; }
			.extracted-data-table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
			.extracted-data-table th, .extracted-data-table td { padding: 0.5rem 0.75rem; border: 1px solid #e0e0e0; text-align: left; }
			.extracted-data-table th { background: #f5f5f5; font-weight: 600; color: #444; }
			.extracted-data-table tr:hover td { background: #fafafa; }
			.extracted-data-table td:empty { background: #f9f9f9; }
			.fulltext-container { background: white; border-radius: 8px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
			.fulltext-stats { display: flex; gap: 2rem; margin-bottom: 1rem; padding-bottom: 0.75rem; border-bottom: 1px solid #e0e0e0; color: #666; font-size: 0.9rem; }
			.fulltext-content { max-height: 400px; overflow-y: auto; }
			.fulltext-content pre { white-space: pre-wrap; word-wrap: break-word; font-family: monospace; font-size: 0.85rem; line-height: 1.5; color: #444; background: #f9f9f9; padding: 1rem; border-radius: 4px; }
			.stemming-container { background: white; border-radius: 8px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); border-left: 4px solid #03a9f4; }
			.stemming-header { display: flex; flex-wrap: wrap; gap: 1rem 2rem; margin-bottom: 1rem; padding-bottom: 0.75rem; border-bottom: 1px solid #e0e0e0; }
			.stemming-info { color: #666; font-size: 0.9rem; }
			.stemming-label { color: #888; }
			.stemming-tokens { max-height: 500px; overflow-y: auto; }
			.stemming-table { font-size: 0.85rem; }
			.stemming-table th { background: #03a9f4; }
			.stemming-table .token-num { color: #999; font-size: 0.75rem; width: 50px; text-align: center; }
			.stemming-table .token-surface { font-weight: 500; }
			.stemming-table .token-stem { font-family: monospace; color: #03a9f4; font-weight: 600; }
			.stemming-table .token-offset { font-family: monospace; font-size: 0.75rem; color: #999; }
			.token-notice { color: #888; font-style: italic; font-size: 0.85rem; margin-bottom: 0.75rem; padding: 0.5rem; background: #f5f5f5; border-radius: 4px; }
			.toc-container { background: white; border-radius: 8px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); border-left: 4px solid #ff9800; }
			.toc-stats { display: flex; gap: 2rem; margin-bottom: 1rem; padding-bottom: 0.75rem; border-bottom: 1px solid #e0e0e0; color: #666; font-size: 0.9rem; }
			.toc-entries { display: flex; flex-direction: column; gap: 0.25rem; }
			.toc-entry { display: flex; align-items: center; gap: 0.75rem; padding: 0.5rem 0.75rem; border-radius: 4px; transition: background 0.2s; }
			.toc-entry:hover { background: #f5f5f5; }
			.toc-level { font-size: 0.7rem; font-weight: 600; color: white; background: #ff9800; padding: 0.15rem 0.4rem; border-radius: 3px; min-width: 28px; text-align: center; }
			.toc-level-1 { font-size: 1rem; }
			.toc-level-1 .toc-title { font-weight: 700; }
			.toc-level-2 { padding-left: 2rem; font-size: 0.95rem; }
			.toc-level-2 .toc-level { background: #ffa726; }
			.toc-level-3 { padding-left: 3.5rem; font-size: 0.9rem; color: #666; }
			.toc-level-3 .toc-level { background: #ffcc80; color: #333; }
			.toc-title { flex-shrink: 0; }
			.toc-dots { flex: 1; border-bottom: 1px dotted #ccc; min-width: 20px; margin-bottom: 0.3rem; }
			.toc-page { font-weight: 600; color: #667eea; font-size: 0.95rem; min-width: 24px; text-align: right; }
			.toc-meta { display: flex; gap: 0.5rem; align-items: center; font-size: 0.75rem; color: #999; margin-left: 0.5rem; }
			.toc-block-ref { font-family: monospace; }
			.toc-confidence { color: #667eea; font-weight: 600; }
			.toc-source { font-size: 0.65rem; padding: 0.1rem 0.35rem; border-radius: 3px; font-weight: 600; text-transform: uppercase; }
			.toc-source-explicit { background: #4caf50; color: white; }
			.toc-source-heuristic { background: #9e9e9e; color: white; }
			.significance-controls { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; padding: 0.75rem 1rem; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
			.toggle-auxiliary { background: #667eea; color: white; border: none; padding: 0.5rem 1rem; border-radius: 4px; cursor: pointer; font-size: 0.85rem; font-weight: 600; transition: background 0.2s; }
			.toggle-auxiliary:hover { background: #5a6fd6; }
			.toggle-auxiliary.active { background: #43a047; }
			.block-count { color: #888; font-size: 0.85rem; }
			.auxiliary-block { border: 2px dashed #ccc; opacity: 0.7; font-size: 0.85rem; }
			.auxiliary-block .block-type { background: #9e9e9e; }
			""";
	}

	private String _getScript()
	{
		return """
			function showLayer(layerId) {
				document.querySelectorAll('.layer-content').forEach(el => el.classList.remove('active'));
				document.querySelectorAll('.tab').forEach(el => el.classList.remove('active'));
				document.getElementById(layerId).classList.add('active');
				event.target.classList.add('active');
			}
			function toggleAuxiliary() {
				var btn = document.querySelector('.toggle-auxiliary');
				var blocks = document.querySelectorAll('.auxiliary-block');
				var countEl = document.getElementById('blockCount');
				var showing = btn.classList.toggle('active');
				blocks.forEach(function(el) {
					el.style.display = showing ? '' : 'none';
				});
				if (btn.classList.contains('active')) {
					btn.textContent = 'Hide auxiliary blocks';
					if (countEl) {
						var total = document.querySelectorAll('.block').length;
						countEl.textContent = 'Showing ' + total + '/' + total + ' blocks';
					}
				} else {
					btn.textContent = 'Show auxiliary blocks';
					if (countEl) {
						var total = document.querySelectorAll('.block').length;
						var aux = blocks.length;
						countEl.textContent = 'Showing ' + (total - aux) + '/' + total + ' blocks (' + aux + ' auxiliary hidden)';
					}
				}
			}
			""";
	}

	private String _escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

	// OrchestrationAware implementation
	@Override
	public Set<String> getPredecessors()
	{
		return Collections.emptySet();  // Depends on VimDocument, not specific plugins
	}

	@Override
	public boolean isAutoEnabled()
	{
		return false;  // Visualizers are not auto-enabled, use 'vim-parser visualize' separately
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.VISUALIZE;
	}
}
