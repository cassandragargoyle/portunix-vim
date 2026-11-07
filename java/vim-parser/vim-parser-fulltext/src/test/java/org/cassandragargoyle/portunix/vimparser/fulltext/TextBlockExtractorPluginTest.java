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
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TextBlockExtractorPlugin
 */
class TextBlockExtractorPluginTest
{
	private TextBlockExtractorPlugin plugin;

	@BeforeEach
	void setUp()
	{
		plugin = new TextBlockExtractorPlugin();
	}

	@Test
	void testPluginMetadata()
	{
		assertEquals("fulltext", plugin.getId());
		assertEquals("Text Block Extractor", plugin.getName());
		assertEquals("0.1.0", plugin.getVersion());
		assertEquals(10, plugin.getPriority());
	}

	@Test
	void testPluginId_constant()
	{
		assertEquals("fulltext", TextBlockExtractorPlugin.PLUGIN_ID);
	}

	@Test
	void testResultKeyBlocks_constant()
	{
		assertEquals("textBlocks", TextBlockExtractorPlugin.RESULT_KEY_BLOCKS);
	}

	@Test
	void testResultKeyPageCount_constant()
	{
		assertEquals("pageCount", TextBlockExtractorPlugin.RESULT_KEY_PAGE_COUNT);
	}

	@Test
	void testDependencies_empty()
	{
		assertTrue(plugin.getDependencies().isEmpty());
	}

	@Test
	void testSupportedModes()
	{
		ExecutionMode[] modes = plugin.getSupportedModes();
		assertEquals(2, modes.length);
		assertEquals(ExecutionMode.SYNC, modes[0]);
		assertEquals(ExecutionMode.ASYNC_THREAD, modes[1]);
	}

	@Test
	void testExtract_nullPdfContent()
	{
		PluginContext context = new PluginContext();
		context.setPdfContent(null);

		PluginResult result = plugin.extract(context);

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorMessage().contains("No PDF content"));
	}

	@Test
	void testExtract_emptyPdfContent()
	{
		PluginContext context = new PluginContext();
		context.setPdfContent(new byte[0]);

		PluginResult result = plugin.extract(context);

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorMessage().contains("No PDF content"));
	}

	@Test
	void testExtract_invalidPdfContent()
	{
		PluginContext context = new PluginContext();
		context.setPdfContent("not a pdf".getBytes());

		PluginResult result = plugin.extract(context);

		assertFalse(result.isSuccess());
		assertNotNull(result.getErrorMessage());
	}

	@Test
	void testExtract_doesNotStoreDataOnError()
	{
		PluginContext context = new PluginContext();
		context.setPdfContent("invalid pdf content".getBytes());

		plugin.extract(context);

		// On invalid PDF, text blocks and page count won't be stored
		assertFalse(context.getSharedData().containsKey(TextBlockExtractorPlugin.RESULT_KEY_BLOCKS));
		assertFalse(context.getSharedData().containsKey(TextBlockExtractorPlugin.RESULT_KEY_PAGE_COUNT));
	}

	@Test
	void testPriority_isFirst()
	{
		// Text block extractor should run first (lowest priority number)
		assertEquals(10, plugin.getPriority());
	}

	@Test
	void testNoGrpcSupport()
	{
		ExecutionMode[] modes = plugin.getSupportedModes();

		for (ExecutionMode mode : modes)
		{
			assertNotEquals(ExecutionMode.ASYNC_GRPC, mode);
		}
	}

	@Test
	void testLargeSheet_isolatedLabelsGiveOneBlockEach() throws Exception
	{
		byte[] pdf = createPdf(DRAWING_SHEET,
			new Label("Room 101", 100, 1000, 3),
			new Label("Kitchen", 1200, 1000, 3),
			new Label("Rev A", 2400, 1000, 3),
			new Label("EI 30", 1200, 600, 3),
			new Label("Scale 1:50", 2400, 100, 3));

		List<TextBlock> blocks = extractBlocks(pdf);

		assertEquals(List.of("Room 101", "Kitchen", "Rev A", "EI 30", "Scale 1:50"), blockTexts(blocks));
		for (TextBlock block : blocks)
		{
			assertTrue(block.getBbox().getWidth() < 30,
				"Block bbox should cover its label only: " + block.getFullText() + " " + block.getBbox().getWidth());
		}
	}

	@Test
	void testLargeSheet_smallGapRelativeToFontSplitsLabels() throws Exception
	{
		// 40 pt gap is below the absolute 50 pt threshold but more than 3 em of a 3 pt font
		byte[] pdf = createPdf(DRAWING_SHEET,
			new Label("D01", 500, 700, 3),
			new Label("W12", 545, 700, 3));

		List<TextBlock> blocks = extractBlocks(pdf);

		assertEquals(List.of("D01", "W12"), blockTexts(blocks));
	}

	@Test
	void testSameBaselineLargeGap_twoBlocksWithOwnText() throws Exception
	{
		byte[] pdf = createPdf(PDRectangle.A4,
			new Label("ALPHA", 72, 700, 10),
			new Label("omega", 400, 700, 10));

		List<TextBlock> blocks = extractBlocks(pdf);

		assertEquals(List.of("ALPHA", "omega"), blockTexts(blocks));
	}

	@Test
	void testOverlappingLabels_lettersNotInterleaved() throws Exception
	{
		// Two-line label with tight leading: glyph heights overlap, PDFBox treats them as one line
		byte[] pdf = createPdf(DRAWING_SHEET,
			new Label("Kuchyne", 300, 500, 10),
			new Label("Kitchen", 302, 494, 10));

		List<TextBlock> blocks = extractBlocks(pdf);

		List<String> elementTexts = blocks.stream()
			.flatMap(b -> b.getTextElements().stream())
			.map(TextElement::getText)
			.toList();
		assertEquals(List.of("Kuchyne", "Kitchen"), elementTexts);
	}

	@Test
	void testA4Paragraphs_regression() throws Exception
	{
		String[] first = {
			"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do",
			"eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim",
			"ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut",
			"aliquip ex ea commodo consequat."
		};
		String[] second = {
			"Duis aute irure dolor in reprehenderit in voluptate velit esse",
			"cillum dolore eu fugiat nulla pariatur."
		};

		Label[] labels = new Label[first.length + second.length + 1];
		int i = 0;
		labels[i++] = new Label("Introduction", 72, 760, 16);
		for (int line = 0; line < first.length; line++)
		{
			labels[i++] = new Label(first[line], 72, 730 - line * 14, 11);
		}
		for (int line = 0; line < second.length; line++)
		{
			labels[i++] = new Label(second[line], 72, 630 - line * 14, 11);
		}

		List<TextBlock> blocks = extractBlocks(createPdf(PDRectangle.A4, labels));

		assertEquals(2, blocks.size(), "Heading with paragraph and the second paragraph: " + blockTexts(blocks));
		assertEquals("Introduction " + String.join(" ", first), blocks.get(0).getFullText());
		assertEquals(String.join(" ", second), blocks.get(1).getFullText());
	}

	private static final PDRectangle DRAWING_SHEET = new PDRectangle(2580, 1276);

	private record Label(String text, float x, float y, float fontSize)
	{
	}

	private static byte[] createPdf(PDRectangle size, Label... labels) throws Exception
	{
		try (PDDocument doc = new PDDocument())
		{
			PDPage page = new PDPage(size);
			doc.addPage(page);
			PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

			try (PDPageContentStream cs = new PDPageContentStream(doc, page))
			{
				for (Label label : labels)
				{
					cs.beginText();
					cs.setFont(font, label.fontSize());
					cs.newLineAtOffset(label.x(), label.y());
					cs.showText(label.text());
					cs.endText();
				}
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			doc.save(out);
			return out.toByteArray();
		}
	}

	private List<TextBlock> extractBlocks(byte[] pdf)
	{
		PluginContext context = new PluginContext();
		context.setPdfContent(pdf);

		PluginResult result = plugin.extract(context);
		assertTrue(result.isSuccess(), "Extraction should succeed");

		@SuppressWarnings("unchecked")
		List<TextBlock> blocks = context.getSharedData(TextBlockExtractorPlugin.RESULT_KEY_BLOCKS, List.class);
		assertNotNull(blocks);
		return blocks;
	}

	private static List<String> blockTexts(List<TextBlock> blocks)
	{
		return blocks.stream().map(TextBlock::getFullText).toList();
	}
}
