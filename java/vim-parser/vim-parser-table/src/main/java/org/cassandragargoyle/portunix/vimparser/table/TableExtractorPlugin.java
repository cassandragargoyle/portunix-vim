/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.table;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.tabledetector.model.TableModel;
import org.cassandragargoyle.portunix.tabledetector.pdf.PdfTableDetector;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Table extractor plugin - thin adapter delegating to table-detector-pdf
 * Detects tabular data in PDF documents and extracts cell content
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@Extension
public class TableExtractorPlugin implements ExtractorPlugin, OrchestrationAware
{
	private static final Logger LOG = LogFactory.getLogger(TableExtractorPlugin.class);

	public static final String PLUGIN_ID = "table";

	public static final String RESULT_KEY_TABLES = "tables";

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "Table Extractor";
	}

	@Override
	public String getVersion()
	{
		return "0.2.0";
	}

	@Override
	public int getPriority()
	{
		return 30; // After barcode extraction
	}

	@Override
	public Set<String> getDependencies()
	{
		return Collections.emptySet();
	}

	@Override
	public Set<String> getPredecessors()
	{
		return Collections.emptySet();
	}

	@Override
	public boolean isAutoEnabled()
	{
		return true;
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.EXTRACT;
	}

	@Override
	public ExecutionMode[] getSupportedModes()
	{
		return new ExecutionMode[]
		{
			ExecutionMode.SYNC,
			ExecutionMode.ASYNC_THREAD
		};
	}

	@Override
	public void initialize()
	{
		LOG.log(Level.FINE, "Table extractor plugin initialized (delegates to table-detector-pdf)");
	}

	@Override
	public PluginResult extract(PluginContext context)
	{
		long startTime = System.currentTimeMillis();

		try
		{
			byte[] pdfContent = context.getPdfContent();
			if (pdfContent == null || pdfContent.length == 0)
			{
				return PluginResult.failure("No PDF content provided");
			}

			List<TableModel> allTables = new ArrayList<>();
			int tableCounter = 1;

			try (PDDocument document = Loader.loadPDF(pdfContent))
			{
				int pageCount = document.getNumberOfPages();

				int startPage = context.isAllPages() ? 1 : context.getPageNumber();
				int endPage = context.isAllPages() ? pageCount : context.getPageNumber();

				PdfTableDetector detector = new PdfTableDetector();

				for (int pageNum = startPage; pageNum <= endPage; pageNum++)
				{
					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Scanning page {0} for tables", pageNum);
					}

					List<TableModel> pageTables = detector.detectTables(document, pageNum);

					for (TableModel table : pageTables)
					{
						table.setId("table-" + tableCounter);
						table.setPage(pageNum);
						tableCounter++;
					}

					if (context.isVerbose() && !pageTables.isEmpty())
					{
						LOG.log(Level.INFO, "Page {0}: found {1} table(s)",
								LogFactory.args(pageNum, pageTables.size()));
					}

					allTables.addAll(pageTables);
				}
			}

			// Store results in context for other plugins
			context.putSharedData(RESULT_KEY_TABLES, allTables);

			PluginResult result = PluginResult.success(allTables);
			result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			result.addMetadata("table_count", String.valueOf(allTables.size()));

			int totalCells = allTables.stream().mapToInt(t -> t.getCells().size()).sum();
			result.addMetadata("cell_count", String.valueOf(totalCells));

			return result;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Error extracting tables: {0}", e.getMessage());
			return PluginResult.failure("Table extraction failed: " + e.getMessage());
		}
	}

	@Override
	public void shutdown()
	{
		LOG.log(Level.FINE, "Table extractor plugin shutdown");
	}
}
