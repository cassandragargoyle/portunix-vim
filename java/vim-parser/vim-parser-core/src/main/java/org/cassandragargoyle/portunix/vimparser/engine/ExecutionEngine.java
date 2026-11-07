/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.engine;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.api.telemetry.TelemetryProvider;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.ParserPlugin;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.core.PluginManager;
import org.cassandragargoyle.portunix.vimparser.model.Barcode;
import org.cassandragargoyle.portunix.vimparser.model.DocumentMetadata;
import org.cassandragargoyle.portunix.vimparser.model.FulltextContent;
import org.cassandragargoyle.portunix.vimparser.model.GraphicElement;
import org.cassandragargoyle.portunix.vimparser.model.SpaydPayment;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.TableOfContents;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Execution engine for running plugins in different modes
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class ExecutionEngine
{
	private static final Logger LOG = LogFactory.getLogger(ExecutionEngine.class);

	private final PluginManager pluginManager;

	private ExecutionMode defaultMode = ExecutionMode.SYNC;

	private int threadPoolSize = 4;

	private ExecutorService executor;

	private TelemetryProvider telemetryProvider = TelemetryProvider.noOp();

	public ExecutionEngine(PluginManager pluginManager)
	{
		this.pluginManager = pluginManager;
	}

	public void setTelemetryProvider(TelemetryProvider telemetryProvider)
	{
		this.telemetryProvider = telemetryProvider;
	}

	public void setDefaultMode(ExecutionMode mode)
	{
		this.defaultMode = mode;
	}

	public void setThreadPoolSize(int size)
	{
		this.threadPoolSize = size;
	}

	/**
	 * Process PDF content and build VimDocument using configured plugins
	 */
	public VimDocument process(byte[] pdfContent, String sourceName, boolean verbose, Set<String> enabledPlugins)
	{
		Tracer tracer = telemetryProvider.getTracer("vim-parser-engine");

		Span processSpan = tracer.spanBuilder("process-pdf")
			.setAttribute("document.path", sourceName)
			.setAttribute("document.size", (long) pdfContent.length)
			.setAttribute("vim.command", "extract")
			.setAttribute("vim.mode", defaultMode.name())
			.startSpan();

		try (var scope = processSpan.makeCurrent())
		{
			PluginContext context = new PluginContext();
			context.setPdfContent(pdfContent);
			context.setVerbose(verbose);
			context.setTracer(tracer);

			VimDocument vimDoc = new VimDocument();
			vimDoc.getDocument().setSource(sourceName);

			// Get plugins to run
			List<ExtractorPlugin> extractors = pluginManager.getExtractorPlugins(enabledPlugins);
			List<ParserPlugin> parsers = pluginManager.getParserPlugins(enabledPlugins);

			if (extractors.isEmpty())
			{
				LOG.log(Level.WARNING, "No extractor plugins available");
				return vimDoc;
			}

			processSpan.setAttribute("vim.plugin.count",
				(long) (extractors.size() + parsers.size()));

			// Run extractors based on mode
			Span extractorsSpan = tracer.spanBuilder("run-extractors")
				.setAttribute("vim.plugin.count", (long) extractors.size())
				.setAttribute("vim.mode", defaultMode.name())
				.startSpan();

			try (var exScope = extractorsSpan.makeCurrent())
			{
				switch (defaultMode)
				{
					case SYNC:
						runExtractorsSync(extractors, context);
						break;
					case ASYNC_THREAD:
						runExtractorsAsync(extractors, context);
						break;
					case ASYNC_GRPC:
						LOG.log(Level.INFO, "gRPC mode not yet implemented, using thread mode");
						runExtractorsAsync(extractors, context);
						break;
				}
			}
			finally
			{
				extractorsSpan.end();
			}

			// Run parsers (always sync, they depend on extractor results)
			Span parsersSpan = tracer.spanBuilder("run-parsers")
				.setAttribute("vim.plugin.count", (long) parsers.size())
				.startSpan();

			try (var pScope = parsersSpan.makeCurrent())
			{
				runParsersSync(parsers, context);
			}
			finally
			{
				parsersSpan.end();
			}

			// Build VimDocument from results
			Span buildSpan = tracer.spanBuilder("build-vim-document").startSpan();
			try (var bScope = buildSpan.makeCurrent())
			{
				buildVimDocument(vimDoc, context);
			}
			finally
			{
				buildSpan.end();
			}

			return vimDoc;
		}
		finally
		{
			processSpan.end();
		}
	}

	/**
	 * Run extractors sequentially
	 */
	private void runExtractorsSync(List<ExtractorPlugin> extractors, PluginContext context)
	{
		Tracer tracer = context.getTracer();

		for (ExtractorPlugin extractor : extractors)
		{
			if (context.isVerbose())
			{
				LOG.log(Level.INFO, "Running extractor: {0}", extractor.getName());
			}

			Span pluginSpan = tracer.spanBuilder("plugin:" + extractor.getId())
				.setAttribute("vim.plugin.name", extractor.getId())
				.startSpan();

			try (var scope = pluginSpan.makeCurrent())
			{
				PluginResult result = extractor.extract(context);
				context.addPreviousResult(extractor.getId(), result);

				if (!result.isSuccess())
				{
					pluginSpan.setStatus(StatusCode.ERROR, result.getErrorMessage());
					LOG.log(Level.WARNING, "Extractor {0} failed: {1}",
							LogFactory.args(extractor.getId(), result.getErrorMessage()));
				}
				else if (context.isVerbose())
				{
					LOG.log(Level.INFO, "Extractor {0} completed in {1}ms",
							LogFactory.args(extractor.getId(), result.getProcessingTimeMs()));
				}
			}
			catch (Exception e)
			{
				pluginSpan.setStatus(StatusCode.ERROR, e.getMessage());
				pluginSpan.recordException(e);
				LOG.log(Level.SEVERE, "Error running extractor {0}: {1}", LogFactory.args(extractor.getId(), e.getMessage()));
				context.addPreviousResult(extractor.getId(), PluginResult.failure("Exception: " + e.getMessage()));
			}
			finally
			{
				pluginSpan.end();
			}
		}
	}

	/**
	 * Run independent extractors in parallel using thread pool
	 */
	private void runExtractorsAsync(List<ExtractorPlugin> extractors, PluginContext context)
	{
		if (executor == null)
		{
			executor = Executors.newFixedThreadPool(threadPoolSize);
		}

		// Group extractors by dependencies
		List<ExtractorPlugin> independentExtractors = new ArrayList<>();
		List<ExtractorPlugin> dependentExtractors = new ArrayList<>();

		for (ExtractorPlugin extractor : extractors)
		{
			if (extractor.getDependencies().isEmpty())
			{
				independentExtractors.add(extractor);
			}
			else
			{
				dependentExtractors.add(extractor);
			}
		}

		// Run independent extractors in parallel
		if (!independentExtractors.isEmpty())
		{
			List<CompletableFuture<Void>> futures = new ArrayList<>();

			for (ExtractorPlugin extractor : independentExtractors)
			{
				CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
				{
					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Running extractor (async): {0}", extractor.getName());
					}

					try
					{
						PluginResult result = extractor.extract(context);
						synchronized (context)
						{
							context.addPreviousResult(extractor.getId(), result);
						}

						if (context.isVerbose())
						{
							LOG.log(Level.INFO, "Extractor {0} completed in {1}ms", LogFactory.args(extractor.getId(), result.getProcessingTimeMs()));
						}
					}
					catch (Exception e)
					{
						LOG.log(Level.SEVERE, "Error running extractor {0}: {1}", LogFactory.args(extractor.getId(), e.getMessage()));
						synchronized (context)
						{
							context.addPreviousResult(extractor.getId(), PluginResult.failure("Exception: " + e.getMessage()));
						}
					}
				}, executor);

				futures.add(future);
			}

			// Wait for all independent extractors to complete
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		}

		// Run dependent extractors sequentially
		for (ExtractorPlugin extractor : dependentExtractors)
		{
			// Check if dependencies are satisfied
			Set<String> deps = extractor.getDependencies();
			boolean depsOk = true;

			for (String dep : deps)
			{
				PluginResult depResult = context.getPreviousResult(dep);
				if (depResult == null || !depResult.isSuccess())
				{
					LOG.log(Level.WARNING, "Skipping {0}: dependency {1} not satisfied", LogFactory.args(extractor.getId(), dep));
					depsOk = false;
					break;
				}
			}

			if (!depsOk)
			{
				continue;
			}

			if (context.isVerbose())
			{
				LOG.log(Level.INFO, "Running extractor: {0}", extractor.getName());
			}

			try
			{
				PluginResult result = extractor.extract(context);
				context.addPreviousResult(extractor.getId(), result);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, "Error running extractor {0}: {1}", LogFactory.args(extractor.getId(), e.getMessage()));
				context.addPreviousResult(extractor.getId(), PluginResult.failure("Exception: " + e.getMessage()));
			}
		}
	}

	/**
	 * Run parsers sequentially
	 */
	private void runParsersSync(List<ParserPlugin> parsers, PluginContext context)
	{
		Tracer tracer = context.getTracer();

		for (ParserPlugin parser : parsers)
		{
			// Check dependencies
			Set<String> deps = parser.getDependencies();
			boolean depsOk = true;

			for (String dep : deps)
			{
				PluginResult depResult = context.getPreviousResult(dep);
				if (depResult == null || !depResult.isSuccess())
				{
					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Skipping parser {0}: dependency {1} not available",
								LogFactory.args(parser.getId(), dep));
					}
					depsOk = false;
					break;
				}
			}

			if (!depsOk)
			{
				continue;
			}

			if (context.isVerbose())
			{
				LOG.log(Level.INFO, "Running parser: {0}", parser.getName());
			}

			Span pluginSpan = tracer.spanBuilder("plugin:" + parser.getId())
				.setAttribute("vim.plugin.name", parser.getId())
				.startSpan();

			try (var scope = pluginSpan.makeCurrent())
			{
				PluginResult result = parser.parse(context);
				context.addPreviousResult(parser.getId(), result);

				if (context.isVerbose() && result.isSuccess())
				{
					LOG.log(Level.INFO, "Parser {0} completed in {1}ms", LogFactory.args(parser.getId(), result.getProcessingTimeMs()));
				}
			}
			catch (Exception e)
			{
				pluginSpan.setStatus(StatusCode.ERROR, e.getMessage());
				pluginSpan.recordException(e);
				LOG.log(Level.SEVERE, "Error running parser {0}: {1}", LogFactory.args(parser.getId(), e.getMessage()));
			}
			finally
			{
				pluginSpan.end();
			}
		}
	}

	/**
	 * Build VimDocument from plugin results (public for AutoOrchestrator)
	 */
	public void buildVimDocumentFromContext(VimDocument vimDoc, PluginContext context)
	{
		buildVimDocument(vimDoc, context);
	}

	/**
	 * Build VimDocument from plugin results stored in PluginContext
	 *
	 * This method aggregates results from all executed plugins and populates the VimDocument's
	 * three-layer architecture (physical, structural, semantic):
	 *
	 * 1. TEXT BLOCKS ("textBlocks"): Extracted text regions from fulltext extractor
	 *    - Added to physical layer as PhysicalBlock (raw position/content)
	 *    - Added to structural layer as StructuralBlock (logical grouping)
	 *
	 * 2. PAGE COUNT ("pageCount"): Document page metadata from PDF extraction
	 *
	 * 3. BARCODES ("barcodes"): QR codes, barcodes from barcode plugin
	 *    - Added to physical layer with coordinates and raw value
	 *    - Added to structural layer with proximity analysis to nearby text blocks
	 *    - Proximity threshold: 100 PDF units for block association
	 *
	 * 4. SPAYD PAYMENTS ("spaydPayments"): Parsed payment data from SPAYD parser
	 *    - Added to semantic layer as payment entities
	 *
	 * 5. TABLES ("tables"): Detected tables from table extraction plugin
	 *    - Uses reflection to avoid compile-time dependency on vim-parser-table module
	 *    - Extracts table structure (rows, columns, cells, headers)
	 *    - Added to physical layer with bounding boxes
	 *
	 * 6. SEMANTIC ENTITIES: Extracted from text blocks via header recognition
	 *    - Identifies customer, supplier, invoice sections by header keywords
	 *    - Parses address components (name, street, postal code, city)
	 *
	 * 7. DOCUMENT METADATA ("documentMetadata"): PDF metadata from metadata extractor
	 *    - Title, author, subject, keywords, creation/modification dates
	 *    - PDF version, producer, page count
	 *
	 * 8. FULLTEXT CONTENT ("fulltextContent"): Full document text from metadata extractor
	 *    - Complete text content for search indexing
	 *    - Page-by-page text breakdown
	 *
	 * @param vimDoc target document to populate with extracted data
	 * @param context plugin execution context containing shared data from all plugins
	 */
	@SuppressWarnings("unchecked")
	private void buildVimDocument(VimDocument vimDoc, PluginContext context)
	{
		// Get text blocks from fulltext extractor
		List<TextBlock> blocks = context.getSharedData("textBlocks", List.class);
		if (blocks != null)
		{
			for (TextBlock block : blocks)
			{
				vimDoc.getLayers().getPhysical().addBlock(
					VimDocument.PhysicalBlock.fromTextBlock(block));
				vimDoc.getLayers().getStructural().addBlock(
					VimDocument.StructuralBlock.fromTextBlock(block));
			}
		}

		// Get page count
		Integer pageCount = context.getSharedData("pageCount", Integer.class);
		if (pageCount != null)
		{
			vimDoc.getDocument().setPages(pageCount);
		}

		// Get barcodes from barcode extractor
		List<Barcode> barcodes = context.getSharedData("barcodes", List.class);
		if (barcodes != null && !barcodes.isEmpty())
		{
			vimDoc.setVimVersion("1.1");  // Update version to indicate barcode support

			for (Barcode barcode : barcodes)
			{
				vimDoc.getLayers().getPhysical().addBarcode(
					VimDocument.PhysicalBarcode.fromBarcode(barcode));

				VimDocument.StructuralBarcode structBarcode
					= VimDocument.StructuralBarcode.fromBarcode(barcode);

				// Find nearby blocks
				if (blocks != null && barcode.getBbox() != null)
				{
					float proximityThreshold = 100.0f;
					for (TextBlock block : blocks)
					{
						if (block.getPage() == barcode.getPage() && block.getBbox() != null)
						{
							float distance = calculateDistance(barcode.getBbox(), block.getBbox());
							if (distance < proximityThreshold)
							{
								structBarcode.addNearbyBlock(block.getId());
							}
						}
					}
				}

				vimDoc.getLayers().getStructural().addBarcode(structBarcode);
			}
		}

		// Get graphic elements from graphics extractor
		List<GraphicElement> graphicElements = context.getSharedData("graphicElements", List.class);
		if (graphicElements != null && !graphicElements.isEmpty())
		{
			for (GraphicElement graphic : graphicElements)
			{
				vimDoc.getLayers().getPhysical().addGraphic(graphic);
			}
		}

		// Get SPAYD payments from parser
		List<SpaydPayment> payments = context.getSharedData("spaydPayments", List.class);
		if (payments != null)
		{
			for (SpaydPayment payment : payments)
			{
				vimDoc.getLayers().getSemantic().addPayment(payment);
			}
		}

		// Get tables from table extractor
		List<?> tables = context.getSharedData("tables", List.class);
		if (tables != null && !tables.isEmpty())
		{
			for (Object tableObj : tables)
			{
				// Use reflection to avoid direct dependency on vim-parser-table
				try
				{
					VimDocument.PhysicalTable physTable = new VimDocument.PhysicalTable();

					// Get table properties via reflection
					Class<?> tableClass = tableObj.getClass();
					physTable.setId((String) tableClass.getMethod("getId").invoke(tableObj));
					physTable.setPage((Integer) tableClass.getMethod("getPage").invoke(tableObj));
					physTable.setRows((Integer) tableClass.getMethod("getRowCount").invoke(tableObj));
					physTable.setColumns((Integer) tableClass.getMethod("getColumnCount").invoke(tableObj));
					physTable.setConfidence((Float) tableClass.getMethod("getConfidence").invoke(tableObj));

					Object bbox = tableClass.getMethod("getBoundingBox").invoke(tableObj);
					if (bbox != null)
					{
						physTable.setBbox(toBoundingBox(bbox));
					}

					// Get cells
					List<?> cells = (List<?>) tableClass.getMethod("getCells").invoke(tableObj);
					if (cells != null)
					{
						for (Object cellObj : cells)
						{
							VimDocument.PhysicalTableCell physCell = new VimDocument.PhysicalTableCell();
							Class<?> cellClass = cellObj.getClass();

							physCell.setRow((Integer) cellClass.getMethod("getRow").invoke(cellObj));
							physCell.setColumn((Integer) cellClass.getMethod("getColumn").invoke(cellObj));
							physCell.setText((String) cellClass.getMethod("getText").invoke(cellObj));
							physCell.setHeader((Boolean) cellClass.getMethod("isHeader").invoke(cellObj));

							Object cellBbox = cellClass.getMethod("getBoundingBox").invoke(cellObj);
							if (cellBbox != null)
							{
								physCell.setBbox(toBoundingBox(cellBbox));
							}

							physTable.addCell(physCell);
						}
					}

					vimDoc.getLayers().getPhysical().addTable(physTable);
				}
				catch (Exception e)
				{
					LOG.log(Level.WARNING, "Error converting table: {0}", e.getMessage());
				}
			}
		}

		// Extract semantic entities from blocks
		if (blocks != null)
		{
			extractSemanticEntities(vimDoc, blocks);
		}

		// Get document metadata from metadata extractor
		DocumentMetadata metadata = context.getSharedData("documentMetadata", DocumentMetadata.class);
		if (metadata != null)
		{
			vimDoc.getDocument().setMetadata(metadata);
			vimDoc.setVimVersion("1.2");  // Update version to indicate metadata support
		}

		// Get fulltext content from metadata extractor
		FulltextContent fulltext = context.getSharedData("fulltextContent", FulltextContent.class);
		if (fulltext != null)
		{
			vimDoc.getLayers().getPhysical().setFulltext(fulltext);
			vimDoc.setVimVersion("1.2");  // Update version to indicate fulltext support
		}

		// Get stemming results from lemmatizer plugin
		StemmedResult stemming = context.getSharedData("stemmingResult", StemmedResult.class);
		if (stemming != null)
		{
			vimDoc.getLayers().getSemantic().setStemming(stemming);
			vimDoc.setVimVersion("1.3");  // Update version to indicate stemming support
		}

		// Get TOC from toc extractor plugin
		TableOfContents toc = context.getSharedData("toc", TableOfContents.class);
		if (toc != null && !toc.isEmpty())
		{
			vimDoc.getLayers().setToc(toc);
			vimDoc.setVimVersion("1.4");

			// Mark structural blocks on the TOC page
			List<String> tocBlockIds = context.getSharedData("tocBlockIds", List.class);
			if (tocBlockIds != null && toc.getTocPage() != null)
			{
				int tocPage = toc.getTocPage();
				for (VimDocument.StructuralBlock sBlock : vimDoc.getLayers().getStructural().getBlocks())
				{
					if (sBlock.getPage() == tocPage && tocBlockIds.contains(sBlock.getId()))
					{
						sBlock.setType("toc");
					}
				}
			}
		}
	}

	/**
	 * Extract semantic entities from text blocks based on header recognition
	 */
	private void extractSemanticEntities(VimDocument vimDoc, List<TextBlock> blocks)
	{
		// Header to semantic type mapping
		java.util.Map<String, String> headerTypeMap = new java.util.HashMap<>();
		headerTypeMap.put("zákazník", "customer");
		headerTypeMap.put("adresát", "recipient");
		headerTypeMap.put("dodavatel", "supplier");
		headerTypeMap.put("odběratel", "customer");
		headerTypeMap.put("váš účet", "account");
		headerTypeMap.put("faktura", "invoice");
		headerTypeMap.put("customer", "customer");
		headerTypeMap.put("recipient", "recipient");
		headerTypeMap.put("supplier", "supplier");
		headerTypeMap.put("invoice", "invoice");
		headerTypeMap.put("bill to", "customer");
		headerTypeMap.put("ship to", "recipient");

		for (TextBlock block : blocks)
		{
			if (block.getHeader() == null || block.getHeader().isEmpty())
			{
				continue;
			}

			String headerLower = block.getHeader().toLowerCase().trim();
			String semanticType = null;

			for (java.util.Map.Entry<String, String> entry : headerTypeMap.entrySet())
			{
				if (headerLower.contains(entry.getKey()))
				{
					semanticType = entry.getValue();
					break;
				}
			}

			if (semanticType == null)
			{
				continue;
			}

			VimDocument.SemanticEntity entity = new VimDocument.SemanticEntity();
			entity.setType(semanticType);
			entity.setSourceBlock(block.getId());
			entity.setConfidence(0.8);

			List<String> content = block.getContent();
			if (!content.isEmpty())
			{
				entity.addField("name", content.get(0));
				for (int i = 1; i < content.size(); i++)
				{
					String line = content.get(i);
					if (line.matches(".*\\d{3}\\s*\\d{2}.*"))
					{
						entity.addField("postal_address", line);
					}
					else if (i == 1)
					{
						entity.addField("street", line);
					}
					else if (i == content.size() - 1)
					{
						entity.addField("city", line);
					}
				}
			}

			vimDoc.getLayers().getSemantic().addEntity(entity);
		}
	}

	/**
	 * Convert a bounding box of another module to the core model via reflection
	 *
	 * The table plugin returns its own BoundingBox class with the same fields,
	 * so the value has to be copied and cannot be cast
	 */
	private org.cassandragargoyle.portunix.vimparser.model.BoundingBox toBoundingBox(Object bbox)
		throws ReflectiveOperationException
	{
		if (bbox instanceof org.cassandragargoyle.portunix.vimparser.model.BoundingBox)
		{
			return (org.cassandragargoyle.portunix.vimparser.model.BoundingBox) bbox;
		}

		Class<?> bboxClass = bbox.getClass();
		return new org.cassandragargoyle.portunix.vimparser.model.BoundingBox(
			((Number) bboxClass.getMethod("getX").invoke(bbox)).floatValue(),
			((Number) bboxClass.getMethod("getY").invoke(bbox)).floatValue(),
			((Number) bboxClass.getMethod("getWidth").invoke(bbox)).floatValue(),
			((Number) bboxClass.getMethod("getHeight").invoke(bbox)).floatValue());
	}

	/**
	 * Calculate minimum distance between two bounding boxes
	 */
	private float calculateDistance(org.cassandragargoyle.portunix.vimparser.model.BoundingBox a,
									org.cassandragargoyle.portunix.vimparser.model.BoundingBox b)
	{
		float dx = 0;
		float dy = 0;

		if (a.getRight() < b.getX())
		{
			dx = b.getX() - a.getRight();
		}
		else if (b.getRight() < a.getX())
		{
			dx = a.getX() - b.getRight();
		}

		if (a.getBottom() < b.getY())
		{
			dy = b.getY() - a.getBottom();
		}
		else if (b.getBottom() < a.getY())
		{
			dy = a.getY() - b.getBottom();
		}

		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Shutdown the execution engine
	 */
	public void shutdown()
	{
		if (executor != null)
		{
			executor.shutdown();
			try
			{
				if (!executor.awaitTermination(5, TimeUnit.SECONDS))
				{
					executor.shutdownNow();
				}
			}
			catch (InterruptedException e)
			{
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}
}
