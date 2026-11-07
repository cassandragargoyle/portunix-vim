/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.table;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.cassandragargoyle.portunix.tabledetector.model.TableCell;
import org.cassandragargoyle.portunix.tabledetector.model.TableModel;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Demo program to test table extraction on sample PDF
 */
public class TableExtractionDemo
{
	public static void main(String[] args) throws Exception
	{
		// Default to sample file if no argument provided
		String pdfPath = args.length > 0 ? args[0] :
			"samples/sample1-anonymized.pdf";

		Path path = Path.of(pdfPath);
		if (!Files.exists(path))
		{
			System.err.println("File not found: " + path.toAbsolutePath());
			System.exit(1);
		}

		System.out.println("Testing table extraction on: " + path.toAbsolutePath());
		System.out.println("=".repeat(60));

		// Read PDF
		byte[] pdfContent = Files.readAllBytes(path);
		System.out.println("PDF size: " + pdfContent.length + " bytes");

		// Create plugin and context
		TableExtractorPlugin plugin = new TableExtractorPlugin();
		plugin.initialize();
		// plugin.setDebug(true);  // Uncomment for debugging

		PluginContext context = new PluginContext();
		context.setPdfContent(pdfContent);
		context.setVerbose(true);

		// Extract tables
		System.out.println("\nExtracting tables...");
		PluginResult result = plugin.extract(context);

		System.out.println("\n" + "=".repeat(60));
		System.out.println("Result: " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
		System.out.println("Processing time: " + result.getProcessingTimeMs() + " ms");

		if (!result.isSuccess())
		{
			System.err.println("Error: " + result.getErrorMessage());
			System.exit(1);
		}

		// Print results
		@SuppressWarnings("unchecked")
		List<TableModel> tables = (List<TableModel>) result.getData();
		System.out.println("Tables found: " + tables.size());

		if (tables.isEmpty())
		{
			System.out.println("\nNo tables detected in this document.");
			System.out.println("This could mean:");
			System.out.println("  - Document has no tabular data");
			System.out.println("  - Tables use complex layout not detected by heuristics");
			System.out.println("  - Text positions don't form clear column structure");
		}
		else
		{
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			for (TableModel table : tables)
			{
				System.out.println("\n" + "-".repeat(60));
				System.out.println("Table: " + table.getId());
				System.out.println("Page: " + table.getPage());
				System.out.println("Size: " + table.getRowCount() + " rows x " + table.getColumnCount() + " columns");
				System.out.println("Confidence: " + String.format("%.0f%%", table.getConfidence() * 100));
				System.out.println("Cells: " + table.getCells().size());

				// Print table content as grid
				System.out.println("\nContent:");
				for (int row = 0; row < table.getRowCount(); row++)
				{
					StringBuilder line = new StringBuilder("| ");
					for (int col = 0; col < table.getColumnCount(); col++)
					{
						TableCell cell = table.getCell(row, col);
						String text = cell != null ? cell.getText() : "";
						// Truncate long text
						if (text.length() > 20)
						{
							text = text.substring(0, 17) + "...";
						}
						line.append(String.format("%-20s | ", text));
					}
					System.out.println(line);
				}
			}

			// Print full JSON
			System.out.println("\n" + "=".repeat(60));
			System.out.println("Full JSON output:");
			System.out.println(mapper.writeValueAsString(tables));
		}

		plugin.shutdown();
	}
}
