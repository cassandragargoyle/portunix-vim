/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.table;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Generate simple PDF with bordered table for testing
 */
public class GenerateSamplePdf
{
	public static void main(String[] args) throws IOException
	{
		String outputPath = args.length > 0 ? args[0] : "samples/simple-table.pdf";

		System.out.println("Generating sample PDF with bordered table...");

		try (PDDocument document = new PDDocument())
		{
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			try (PDPageContentStream content = new PDPageContentStream(document, page))
			{
				PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
				PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

				// Title
				content.beginText();
				content.setFont(fontBold, 16);
				content.newLineAtOffset(50, 780);
				content.showText("Sample Table Document");
				content.endText();

				// Table data
				String[][] data = {
					{"Name", "Age", "City", "Country"},
					{"Alice", "25", "Prague", "Czech Republic"},
					{"Bob", "30", "Berlin", "Germany"},
					{"Charlie", "35", "Paris", "France"},
					{"Diana", "28", "Vienna", "Austria"},
					{"Eve", "32", "Warsaw", "Poland"}
				};

				float tableX = 50;
				float tableY = 720;
				float rowHeight = 25;
				float[] colWidths = {120, 60, 100, 120};
				float tableWidth = 0;
				for (float w : colWidths) tableWidth += w;

				// Draw table with borders
				content.setLineWidth(1f);

				// Draw horizontal lines
				for (int i = 0; i <= data.length; i++)
				{
					float y = tableY - i * rowHeight;
					content.moveTo(tableX, y);
					content.lineTo(tableX + tableWidth, y);
					content.stroke();
				}

				// Draw vertical lines
				float x = tableX;
				for (int i = 0; i <= colWidths.length; i++)
				{
					content.moveTo(x, tableY);
					content.lineTo(x, tableY - data.length * rowHeight);
					content.stroke();
					if (i < colWidths.length) x += colWidths[i];
				}

				// Draw cell content
				for (int row = 0; row < data.length; row++)
				{
					float cellX = tableX + 5;
					float cellY = tableY - row * rowHeight - 17;

					for (int col = 0; col < data[row].length; col++)
					{
						content.beginText();
						content.setFont(row == 0 ? fontBold : font, 11);
						content.newLineAtOffset(cellX, cellY);
						content.showText(data[row][col]);
						content.endText();

						cellX += colWidths[col];
					}
				}

				// Second table - numbers
				String[][] data2 = {
					{"Product", "Q1", "Q2", "Q3", "Q4", "Total"},
					{"Widgets", "100", "150", "120", "180", "550"},
					{"Gadgets", "80", "90", "110", "130", "410"},
					{"Gizmos", "50", "60", "70", "80", "260"},
					{"Total", "230", "300", "300", "390", "1220"}
				};

				float table2Y = tableY - data.length * rowHeight - 60;
				float[] colWidths2 = {80, 60, 60, 60, 60, 60};
				float tableWidth2 = 0;
				for (float w : colWidths2) tableWidth2 += w;

				// Draw second table
				for (int i = 0; i <= data2.length; i++)
				{
					float y = table2Y - i * rowHeight;
					content.moveTo(tableX, y);
					content.lineTo(tableX + tableWidth2, y);
					content.stroke();
				}

				x = tableX;
				for (int i = 0; i <= colWidths2.length; i++)
				{
					content.moveTo(x, table2Y);
					content.lineTo(x, table2Y - data2.length * rowHeight);
					content.stroke();
					if (i < colWidths2.length) x += colWidths2[i];
				}

				for (int row = 0; row < data2.length; row++)
				{
					float cellX = tableX + 5;
					float cellY = table2Y - row * rowHeight - 17;

					for (int col = 0; col < data2[row].length; col++)
					{
						content.beginText();
						content.setFont((row == 0 || row == data2.length - 1 || col == 0) ? fontBold : font, 10);
						content.newLineAtOffset(cellX, cellY);
						content.showText(data2[row][col]);
						content.endText();

						cellX += colWidths2[col];
					}
				}
			}

			document.save(outputPath);
			System.out.println("Generated: " + Path.of(outputPath).toAbsolutePath());
		}
	}
}
