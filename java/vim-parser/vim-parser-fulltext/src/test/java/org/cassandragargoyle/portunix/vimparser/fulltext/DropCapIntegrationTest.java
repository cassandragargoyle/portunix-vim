/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: TextBlockExtractorPlugin with drop cap detection
 *
 * Creates a synthetic PDF with drop cap layout and verifies the full
 * extraction + reconstruction pipeline produces merged text.
 */
class DropCapIntegrationTest
{
	@Test
	void testDropCapDetectedInSyntheticPdf() throws Exception
	{
		byte[] pdf = createPdfWithDropCap();

		TextBlockExtractorPlugin plugin = new TextBlockExtractorPlugin();
		PluginContext context = new PluginContext();
		context.setPdfContent(pdf);
		context.setVerbose(true);

		PluginResult result = plugin.extract(context);

		assertTrue(result.isSuccess(), "Extraction should succeed");

		@SuppressWarnings("unchecked")
		List<TextBlock> blocks = context.getSharedData("textBlocks", List.class);
		assertNotNull(blocks, "Text blocks should be stored in shared data");
		assertFalse(blocks.isEmpty(), "Should have at least one text block");

		// Find merged element containing "The" (drop cap "T" + "he")
		boolean foundMerged = false;
		for (TextBlock block : blocks)
		{
			for (TextElement el : block.getTextElements())
			{
				if (el.isDropCapMerged())
				{
					foundMerged = true;
					assertNotNull(el.getDropCapBbox(), "Merged element should have drop cap bbox");
					assertTrue(el.getText().startsWith("T"),
						"Merged text should start with drop cap letter");
				}
			}
		}

		assertTrue(foundMerged, "Should detect and merge at least one drop cap");
	}

	@Test
	void testDropCapDisabledViOption() throws Exception
	{
		byte[] pdf = createPdfWithDropCap();

		TextBlockExtractorPlugin plugin = new TextBlockExtractorPlugin();
		PluginContext context = new PluginContext();
		context.setPdfContent(pdf);
		context.setOption("dropCapEnabled", false);

		PluginResult result = plugin.extract(context);
		assertTrue(result.isSuccess());

		@SuppressWarnings("unchecked")
		List<TextBlock> blocks = context.getSharedData("textBlocks", List.class);
		assertNotNull(blocks);

		// No element should be marked as drop cap merged
		for (TextBlock block : blocks)
		{
			for (TextElement el : block.getTextElements())
			{
				assertFalse(el.isDropCapMerged(),
					"No drop cap merging should happen when disabled");
			}
		}
	}

	@Test
	void testNormalPdfNoDropCaps() throws Exception
	{
		byte[] pdf = createNormalPdf();

		TextBlockExtractorPlugin plugin = new TextBlockExtractorPlugin();
		PluginContext context = new PluginContext();
		context.setPdfContent(pdf);

		PluginResult result = plugin.extract(context);
		assertTrue(result.isSuccess());

		@SuppressWarnings("unchecked")
		List<TextBlock> blocks = context.getSharedData("textBlocks", List.class);
		assertNotNull(blocks);

		// No drop cap merges in normal document
		for (TextBlock block : blocks)
		{
			for (TextElement el : block.getTextElements())
			{
				assertFalse(el.isDropCapMerged(),
					"Normal PDF should not trigger drop cap merging");
			}
		}
	}

	/**
	 * Create a PDF with a large initial "T" followed by "he quick brown fox"
	 * simulating a drop cap layout. Uses 60pt for the drop cap to ensure
	 * the bbox height spans >= 2 body line heights (matching real-world PDFs).
	 */
	private byte[] createPdfWithDropCap() throws Exception
	{
		try (PDDocument doc = new PDDocument())
		{
			PDPage page = new PDPage(PDRectangle.A4);
			doc.addPage(page);

			PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
			PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

			try (PDPageContentStream cs = new PDPageContentStream(doc, page))
			{
				// Drop cap "T" - 60pt, baseline at y=680
				cs.beginText();
				cs.setFont(fontBold, 60);
				cs.newLineAtOffset(72, 680);
				cs.showText("T");
				cs.endText();

				// Body text line 1 - next to drop cap, at top
				cs.beginText();
				cs.setFont(fontRegular, 10);
				cs.newLineAtOffset(100, 710);
				cs.showText("he quick brown fox jumps over");
				cs.endText();

				// Body text line 2
				cs.beginText();
				cs.setFont(fontRegular, 10);
				cs.newLineAtOffset(100, 698);
				cs.showText("the lazy dog. This paragraph");
				cs.endText();

				// Body text line 3
				cs.beginText();
				cs.setFont(fontRegular, 10);
				cs.newLineAtOffset(100, 686);
				cs.showText("continues here and wraps across.");
				cs.endText();

				// Body text line 4 - full width below drop cap
				cs.beginText();
				cs.setFont(fontRegular, 10);
				cs.newLineAtOffset(72, 668);
				cs.showText("More text that flows normally after the drop cap area.");
				cs.endText();

				// Second paragraph - no drop cap
				cs.beginText();
				cs.setFont(fontRegular, 10);
				cs.newLineAtOffset(72, 640);
				cs.showText("Lorem ipsum dolor sit amet, consectetur adipiscing.");
				cs.endText();
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);
			return baos.toByteArray();
		}
	}

	/**
	 * Create a normal PDF with uniform font size - no drop caps
	 */
	private byte[] createNormalPdf() throws Exception
	{
		try (PDDocument doc = new PDDocument())
		{
			PDPage page = new PDPage(PDRectangle.A4);
			doc.addPage(page);

			PDType1Font font = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

			try (PDPageContentStream cs = new PDPageContentStream(doc, page))
			{
				cs.beginText();
				cs.setFont(font, 12);
				cs.newLineAtOffset(72, 700);
				cs.showText("The quick brown fox jumps over the lazy dog.");
				cs.endText();

				cs.beginText();
				cs.setFont(font, 12);
				cs.newLineAtOffset(72, 686);
				cs.showText("Lorem ipsum dolor sit amet, consectetur adipiscing.");
				cs.endText();
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);
			return baos.toByteArray();
		}
	}
}
