/*
 * SPDX-License-Identifier: MIT
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.toc;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.cassandragargoyle.portunix.vimparser.model.TocEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for heading detection logic
 *
 * @author Zdenek
 * @since 2026-03-05
 */
class HeadingDetectorTest
{
	private HeadingDetector detector;

	@BeforeEach
	void setUp()
	{
		detector = new HeadingDetector();
	}

	@Test
	void detectEmptyBlocks()
	{
		List<TocEntry> entries = detector.detect(new ArrayList<>());
		assertTrue(entries.isEmpty());
	}

	@Test
	void detectNullBlocks()
	{
		List<TocEntry> entries = detector.detect(null);
		assertTrue(entries.isEmpty());
	}

	@Test
	void detectSingleHeading()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// Heading block with large font
		TextBlock headingBlock = createBlock("block_1", 1,
				new TextElement("Document Title", new BoundingBox(50, 50, 200, 20), 1, 18.0f, "Arial-Bold"));

		// Analyze structure to set header
		headingBlock.analyzeStructure(10.0f);

		// Body block with normal font
		TextBlock bodyBlock = createBlock("block_2", 1,
				new TextElement("Some regular text content that is longer than a heading would be.", new BoundingBox(50, 100, 400, 12), 1, 10.0f, "Arial"));
		bodyBlock.analyzeStructure(10.0f);

		blocks.add(headingBlock);
		blocks.add(bodyBlock);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(1, entries.size());
		assertEquals("Document Title", entries.get(0).getTitle());
		assertEquals(1, entries.get(0).getPage());
		assertEquals(1, entries.get(0).getLevel());
		assertEquals("block_1", entries.get(0).getBlockId());
		assertTrue(entries.get(0).getConfidence() > 0.7);
	}

	@Test
	void detectMultipleLevels()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// Level 1 heading (largest font)
		TextBlock h1 = createBlock("block_1", 1,
				new TextElement("Main Title", new BoundingBox(50, 50, 200, 24), 1, 24.0f, "Arial-Bold"));
		h1.analyzeStructure(10.0f);

		// Level 2 heading (medium font)
		TextBlock h2 = createBlock("block_2", 1,
				new TextElement("Section Title", new BoundingBox(50, 150, 200, 16), 1, 16.0f, "Arial-Bold"));
		h2.analyzeStructure(10.0f);

		// Body
		TextBlock body = createBlock("block_3", 1,
				new TextElement("Body text that is a normal paragraph with regular font size.", new BoundingBox(50, 200, 400, 10), 1, 10.0f, "Arial"));
		body.analyzeStructure(10.0f);

		blocks.add(h1);
		blocks.add(h2);
		blocks.add(body);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(2, entries.size());
		assertEquals(1, entries.get(0).getLevel());
		assertEquals(2, entries.get(1).getLevel());
	}

	@Test
	void detectHeadingsAcrossPages()
	{
		List<TextBlock> blocks = new ArrayList<>();

		TextBlock h1Page1 = createBlock("block_1", 1,
				new TextElement("Chapter 1", new BoundingBox(50, 50, 200, 18), 1, 18.0f, "Arial-Bold"));
		h1Page1.analyzeStructure(10.0f);

		TextBlock bodyPage1 = createBlock("block_2", 1,
				new TextElement("Content on page 1 with more text to fill the block properly.", new BoundingBox(50, 100, 400, 10), 1, 10.0f, "Arial"));
		bodyPage1.analyzeStructure(10.0f);

		TextBlock h1Page2 = createBlock("block_3", 2,
				new TextElement("Chapter 2", new BoundingBox(50, 50, 200, 18), 2, 18.0f, "Arial-Bold"));
		h1Page2.analyzeStructure(10.0f);

		blocks.add(h1Page1);
		blocks.add(bodyPage1);
		blocks.add(h1Page2);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(2, entries.size());
		assertEquals(1, entries.get(0).getPage());
		assertEquals("Chapter 1", entries.get(0).getTitle());
		assertEquals(2, entries.get(1).getPage());
		assertEquals("Chapter 2", entries.get(1).getTitle());
	}

	@Test
	void noHeadingsInUniformDocument()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// All blocks with same font size, no headers
		for (int i = 1; i <= 3; i++)
		{
			TextBlock block = createBlock("block_" + i, 1,
					new TextElement("Regular text paragraph number " + i + " with enough content.",
							new BoundingBox(50, 50 + i * 50, 400, 10), 1, 10.0f, "Arial"));
			block.analyzeStructure(10.0f);
			blocks.add(block);
		}

		List<TocEntry> entries = detector.detect(blocks);
		assertTrue(entries.isEmpty());
	}

	@Test
	void boldFontDetection()
	{
		assertTrue(detector.isBoldFont("Arial-Bold"));
		assertTrue(detector.isBoldFont("TimesNewRoman-ExtraBold"));
		assertTrue(detector.isBoldFont("Helvetica-SemiBold"));
		assertTrue(detector.isBoldFont("Roboto-Heavy"));
		assertTrue(detector.isBoldFont("OpenSans-Black"));
		assertFalse(detector.isBoldFont("Arial"));
		assertFalse(detector.isBoldFont("TimesNewRoman-Italic"));
		assertFalse(detector.isBoldFont(null));
	}

	@Test
	void levelAssignmentWithThresholds()
	{
		List<Float> thresholds = List.of(24.0f, 16.0f, 12.0f);

		assertEquals(1, detector.assignLevel(24.0f, thresholds));
		assertEquals(2, detector.assignLevel(16.0f, thresholds));
		assertEquals(3, detector.assignLevel(12.0f, thresholds));
		assertEquals(1, detector.assignLevel(30.0f, thresholds)); // above largest
	}

	@Test
	void levelAssignmentEmptyThresholds()
	{
		assertEquals(1, detector.assignLevel(18.0f, new ArrayList<>()));
	}

	@Test
	void baseFontSizeCalculation()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// Block with mostly 10pt text
		TextBlock block = new TextBlock("block_1", 1);
		block.addTextElement(new TextElement("Heading", new BoundingBox(0, 0, 100, 18), 1, 18.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("Text line 1", new BoundingBox(0, 20, 100, 10), 1, 10.0f, "Arial"));
		block.addTextElement(new TextElement("Text line 2", new BoundingBox(0, 32, 100, 10), 1, 10.0f, "Arial"));
		block.addTextElement(new TextElement("Text line 3", new BoundingBox(0, 44, 100, 10), 1, 10.0f, "Arial"));
		block.addTextElement(new TextElement("Text line 4", new BoundingBox(0, 56, 100, 10), 1, 10.0f, "Arial"));
		blocks.add(block);

		float baseFontSize = detector.calculateDocumentBaseFontSize(blocks);
		assertEquals(10.0f, baseFontSize, 0.1f);
	}

	@Test
	void confidenceIncludesBoldBonus()
	{
		List<TextBlock> blocks = new ArrayList<>();

		TextBlock boldHeading = createBlock("block_1", 1,
				new TextElement("Bold Title", new BoundingBox(50, 50, 200, 16), 1, 16.0f, "Arial-Bold"));
		boldHeading.analyzeStructure(10.0f);

		TextBlock regularHeading = createBlock("block_2", 1,
				new TextElement("Regular Title", new BoundingBox(50, 150, 200, 16), 1, 16.0f, "Arial"));
		regularHeading.analyzeStructure(10.0f);

		TextBlock body1 = createBlock("block_3", 1,
				new TextElement("Body text with normal font size for the document baseline.", new BoundingBox(50, 250, 400, 10), 1, 10.0f, "Arial"));
		body1.analyzeStructure(10.0f);

		TextBlock body2 = createBlock("block_4", 1,
				new TextElement("Another paragraph of body text to ensure base font is 10pt.", new BoundingBox(50, 300, 400, 10), 1, 10.0f, "Arial"));
		body2.analyzeStructure(10.0f);

		TextBlock body3 = createBlock("block_5", 1,
				new TextElement("Third paragraph to make 10pt the most common font size.", new BoundingBox(50, 350, 400, 10), 1, 10.0f, "Arial"));
		body3.analyzeStructure(10.0f);

		blocks.add(boldHeading);
		blocks.add(regularHeading);
		blocks.add(body1);
		blocks.add(body2);
		blocks.add(body3);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(2, entries.size());
		// Bold heading should have higher confidence
		assertTrue(entries.get(0).getConfidence() > entries.get(1).getConfidence());
	}

	@Test
	void formLabelsOnCoverPageAreNotHeadings()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// Label and value in separate blocks on the same row
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Investor:", new BoundingBox(50, 100, 80, 14), 1, 14.0f, "Arial-Bold"))));
		blocks.add(analyzed(createBlock("block_2", 1,
				new TextElement("Example Company Ltd.", new BoundingBox(150, 100, 200, 12), 1, 10.0f, "Arial"))));

		// Label and value in the same block
		blocks.add(analyzed(createBlock("block_3", 1,
				new TextElement("Project:", new BoundingBox(50, 140, 80, 14), 1, 14.0f, "Arial-Bold"),
				new TextElement("Family house", new BoundingBox(150, 140, 120, 12), 1, 10.0f, "Arial"))));

		blocks.add(analyzed(createBlock("block_4", 1,
				new TextElement("Technical Report", new BoundingBox(50, 40, 300, 24), 1, 24.0f, "Arial-Bold"))));
		addBodyText(blocks, 1, 300);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(1, entries.size());
		assertEquals("Technical Report", entries.get(0).getTitle());
	}

	@Test
	void standaloneColonHeadingIsKept()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Conclusion:", new BoundingBox(50, 100, 120, 16), 1, 16.0f, "Arial-Bold"))));
		addBodyText(blocks, 1, 130);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(1, entries.size());
		assertEquals("Conclusion:", entries.get(0).getTitle());
	}

	@Test
	void numbersDimensionsAndMarksAreNotHeadings()
	{
		List<TextBlock> blocks = new ArrayList<>();
		String[] noise = {"1.05", "A101", "D12", "\u00b10,000", "+3,250", "A", "7"};
		for (int i = 0; i < noise.length; i++)
		{
			blocks.add(analyzed(createBlock("block_" + i, 1,
					new TextElement(noise[i], new BoundingBox(50, 50 + i * 40, 60, 16), 1, 16.0f, "Arial-Bold"))));
		}
		addBodyText(blocks, 1, 400);

		assertTrue(detector.detect(blocks).isEmpty());
	}

	@Test
	void markOrNumberClassification()
	{
		assertTrue(detector.isMarkOrNumber("\u00b10,000"));
		assertTrue(detector.isMarkOrNumber("1.05"));
		assertTrue(detector.isMarkOrNumber("WC"));
		assertTrue(detector.isMarkOrNumber("A"));
		assertTrue(detector.isMarkOrNumber(null));
		assertFalse(detector.isMarkOrNumber("1 \u00davod"));
		assertFalse(detector.isMarkOrNumber("4.2 Scope"));
	}

	@Test
	void labelRepeatedAtSamePositionOnSeveralPagesIsNotHeading()
	{
		List<TextBlock> blocks = new ArrayList<>();
		for (int page = 1; page <= 4; page++)
		{
			blocks.add(analyzed(createBlock("block_h" + page, page,
					new TextElement("Measured value", new BoundingBox(300, 80 + page, 120, 14), page, 14.0f, "Arial-Bold"))));
			addBodyText(blocks, page, 200);
		}
		blocks.add(analyzed(createBlock("block_c", 2,
				new TextElement("Results", new BoundingBox(50, 40, 200, 20), 2, 20.0f, "Arial-Bold"))));

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(1, entries.size());
		assertEquals("Results", entries.get(0).getTitle());
	}

	@Test
	void repeatedLabelWithDriftingPositionIsNotHeading()
	{
		List<TextBlock> blocks = new ArrayList<>();
		// Each page shifts by 8 pt: page 1 and 3 are 16 pt apart, linked through page 2
		for (int page = 1; page <= 3; page++)
		{
			blocks.add(analyzed(createBlock("block_h" + page, page,
					new TextElement("Running header", new BoundingBox(50, 20 + (page - 1) * 8, 200, 14), page, 14.0f, "Arial-Bold"))));
			addBodyText(blocks, page, 200);
		}

		assertTrue(detector.detect(blocks).isEmpty());
	}

	@Test
	void sameTitleOnTwoPagesIsKept()
	{
		List<TextBlock> blocks = new ArrayList<>();
		for (int page = 1; page <= 2; page++)
		{
			blocks.add(analyzed(createBlock("block_h" + page, page,
					new TextElement("Appendix", new BoundingBox(50, 40, 120, 18), page, 18.0f, "Arial-Bold"))));
			addBodyText(blocks, page, 200);
		}

		assertEquals(2, detector.detect(blocks).size());
	}

	@Test
	void drawingPageIsSkipped()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// Large sheet with room names, title block labels and no running text
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Living room", new BoundingBox(400, 600, 120, 16), 1, 16.0f, "Arial-Bold"))));
		blocks.add(analyzed(createBlock("block_2", 1,
				new TextElement("Ground floor", new BoundingBox(2000, 1500, 200, 20), 1, 20.0f, "Arial-Bold"))));
		for (int i = 0; i < 10; i++)
		{
			blocks.add(analyzed(createBlock("block_m" + i, 1,
					new TextElement("D" + i, new BoundingBox(100 + i * 150, 300, 20, 8), 1, 8.0f, "Arial"))));
		}

		// Text report page in the same document
		blocks.add(analyzed(createBlock("block_r", 2,
				new TextElement("Introduction", new BoundingBox(50, 40, 200, 20), 2, 20.0f, "Arial-Bold"))));
		addBodyText(blocks, 2, 100);

		assertEquals(Set.of(1), detector.detectDrawingPages(blocks));

		List<TocEntry> entries = detector.detect(blocks);
		assertEquals(1, entries.size());
		assertEquals("Introduction", entries.get(0).getTitle());
	}

	@Test
	void largeTextPageIsNotDrawing()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Overview", new BoundingBox(50, 40, 200, 20), 1, 20.0f, "Arial-Bold"))));
		addBodyText(blocks, 1, 1500);

		assertTrue(detector.detectDrawingPages(blocks).isEmpty());
		assertEquals(1, detector.detect(blocks).size());
	}

	@Test
	void formLabelInsideBlockAfterSectionCaptionIsNotHeading()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("CUSTOMER", new BoundingBox(280, 120, 40, 5), 1, 7.0f, "Arial-Bold"),
				new TextElement("DELIVERY ADDRESS:", new BoundingBox(316, 141, 116, 7), 1, 14.0f, "Arial-Bold"),
				new TextElement("Primary school Luhacovice, public organization",
						new BoundingBox(316, 158, 183, 5), 1, 10.0f, "Arial"))));
		addBodyText(blocks, 1, 300);

		assertTrue(detector.detect(blocks).isEmpty());
	}

	@Test
	void labelColumnWithValuesInWideBlockIsNotHeading()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// Column of labels in one block, values in a block to the right joined with other fields
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Invoice:", new BoundingBox(308, 44, 120, 14), 1, 14.0f, "Arial-Bold"),
				new TextElement("PARTNER NUMBER:", new BoundingBox(308, 60, 90, 5), 1, 7.0f, "Arial"),
				new TextElement("CONTRACT ACCOUNT:", new BoundingBox(308, 70, 100, 5), 1, 7.0f, "Arial"))));
		blocks.add(analyzed(createBlock("block_2", 1,
				new TextElement("Part A", new BoundingBox(458, 46, 40, 10), 1, 10.0f, "Arial"),
				new TextElement("1121007312P", new BoundingBox(458, 60, 60, 5), 1, 7.0f, "Arial"),
				new TextElement("PLYN STREDNI ODBER CATEGORY LONG", new BoundingBox(458, 70, 90, 5), 1, 7.0f, "Arial"))));
		addBodyText(blocks, 1, 300);

		assertTrue(detector.detect(blocks).isEmpty());
	}

	@Test
	void labelAboveValueIsNotHeading()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Investor:", new BoundingBox(50, 100, 80, 14), 1, 14.0f, "Arial-Bold"))));
		blocks.add(analyzed(createBlock("block_2", 1,
				new TextElement("Example Company Ltd.", new BoundingBox(50, 118, 200, 10), 1, 10.0f, "Arial"))));
		addBodyText(blocks, 1, 300);

		assertTrue(detector.detect(blocks).isEmpty());
	}

	@Test
	void colonHeadingFollowedByParagraphIsKept()
	{
		List<TextBlock> blocks = new ArrayList<>();

		// Paragraph in the same block
		TextBlock sameBlock = createBlock("block_1", 1,
				new TextElement("Conclusion:", new BoundingBox(50, 100, 120, 16), 1, 16.0f, "Arial-Bold"),
				new TextElement("We recommend the second variant because it is cheaper to build.",
						new BoundingBox(50, 120, 400, 10), 1, 10.0f, "Arial"));
		blocks.add(analyzed(sameBlock));

		// Two-column layout: running text in the right column on the same row
		blocks.add(analyzed(createBlock("block_2", 2,
				new TextElement("Summary:", new BoundingBox(50, 100, 100, 16), 2, 16.0f, "Arial-Bold"))));
		addBodyText(blocks, 2, 95);
		blocks.add(analyzed(createBlock("block_3", 2,
				new TextElement("Running paragraph text in the right column of the same page layout.",
						new BoundingBox(320, 98, 250, 10), 2, 10.0f, "Arial"))));
		addBodyText(blocks, 1, 300);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(2, entries.size());
		assertEquals("Conclusion:", entries.get(0).getTitle());
		assertEquals("Summary:", entries.get(1).getTitle());
	}

	@Test
	void cjkShortTitleIsKept()
	{
		assertFalse(detector.isMarkOrNumber("目录"));
		assertFalse(detector.isMarkOrNumber("概要"));
		assertTrue(detector.isMarkOrNumber("序"));
	}

	@Test
	void largeCoverPageWithFewBlocksIsNotDrawing()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Design Report", new BoundingBox(500, 600, 400, 28), 1, 28.0f, "Arial-Bold"))));
		blocks.add(analyzed(createBlock("block_2", 1,
				new TextElement("Prague 2026", new BoundingBox(1500, 1150, 100, 10), 1, 10.0f, "Arial"))));
		addBodyText(blocks, 2, 100);

		assertTrue(detector.detectDrawingPages(blocks).isEmpty());
		assertEquals(1, detector.detect(blocks).size());
	}

	@Test
	void blockWithoutElementsDoesNotFail()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(new TextBlock("block_empty", 1));
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Title:", new BoundingBox(50, 50, 100, 18), 1, 18.0f, "Arial-Bold"))));
		addBodyText(blocks, 1, 100);

		assertEquals(1, detector.detect(blocks).size());
	}

	@Test
	void largeDocumentIsFast()
	{
		// 1000 pages x 40 blocks: 5 headings, 1 form label and 34 paragraphs per page
		List<TextBlock> blocks = new ArrayList<>();
		for (int page = 1; page <= 1000; page++)
		{
			for (int c = 0; c < 5; c++)
			{
				blocks.add(analyzed(createBlock("block_h" + page + "_" + c, page,
						new TextElement("Heading number " + page + " " + c,
								new BoundingBox(50, 50 + c * 120, 200, 16), page, 16.0f, "Arial-Bold"))));
			}
			blocks.add(analyzed(createBlock("block_l" + page, page,
					new TextElement("Label:", new BoundingBox(50, 700, 60, 14), page, 14.0f, "Arial-Bold"))));
			for (int i = 0; i < 34; i++)
			{
				blocks.add(analyzed(createBlock("block_b" + page + "_" + i, page,
						new TextElement("Running paragraph text with enough words to look like body content.",
								new BoundingBox(50, 60 + i * 20, 400, 10), page, 10.0f, "Arial"))));
			}
		}

		long start = System.nanoTime();
		List<TocEntry> entries = detector.detect(blocks);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		assertEquals(5000, entries.size());
		assertTrue(elapsedMs < 2000, "detect took " + elapsedMs + " ms");
	}

	@Test
	void drawingPageNeedsMinimumBlockCount()
	{
		for (int blockCount : new int[] {9, 10})
		{
			List<TextBlock> blocks = new ArrayList<>();
			blocks.add(analyzed(createBlock("block_t", 1,
					new TextElement("Ground floor", new BoundingBox(2000, 1500, 200, 20), 1, 20.0f, "Arial-Bold"))));
			for (int i = 1; i < blockCount; i++)
			{
				blocks.add(analyzed(createBlock("block_m" + i, 1,
						new TextElement("Room", new BoundingBox(100 + i * 150, 300, 30, 8), 1, 8.0f, "Arial"))));
			}

			Set<Integer> expected = blockCount >= 10 ? Set.of(1) : Set.of();
			assertEquals(expected, detector.detectDrawingPages(blocks), blockCount + " blocks");
		}
	}

	@Test
	void duplicateBlockIdsAcrossPagesKeepOrder()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(analyzed(createBlock("block_1", 2,
				new TextElement("Later", new BoundingBox(50, 500, 100, 16), 2, 16.0f, "Arial-Bold"))));
		blocks.add(analyzed(createBlock("block_2", 2,
				new TextElement("Earlier", new BoundingBox(50, 100, 100, 16), 2, 16.0f, "Arial-Bold"))));
		blocks.add(analyzed(createBlock("block_1", 1,
				new TextElement("Page one", new BoundingBox(50, 100, 100, 16), 1, 16.0f, "Arial-Bold"))));
		addBodyText(blocks, 1, 400);
		addBodyText(blocks, 2, 600);

		List<TocEntry> entries = detector.detect(blocks);

		assertEquals(3, entries.size());
		assertEquals("Page one", entries.get(0).getTitle());
		assertEquals("Earlier", entries.get(1).getTitle());
		assertEquals("Later", entries.get(2).getTitle());
	}

	@Test
	void largeClusterOfIdenticalHeadersIsFast()
	{
		// 5000 pages with the same running header at the same position
		List<TextBlock> blocks = new ArrayList<>();
		for (int page = 1; page <= 5000; page++)
		{
			blocks.add(analyzed(createBlock("block_h" + page, page,
					new TextElement("MEDITATIONS.", new BoundingBox(50, 20, 100, 14), page, 14.0f, "Arial-Bold"))));
			blocks.add(analyzed(createBlock("block_b" + page, page,
					new TextElement("Running paragraph text with enough words to look like body content.",
							new BoundingBox(50, 100, 400, 10), page, 10.0f, "Arial"))));
		}

		long start = System.nanoTime();
		List<TocEntry> entries = detector.detect(blocks);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		assertTrue(entries.isEmpty());
		assertTrue(elapsedMs < 2000, "detect took " + elapsedMs + " ms");
	}

	private TextBlock analyzed(TextBlock block)
	{
		block.analyzeStructure(10.0f);
		return block;
	}

	private void addBodyText(List<TextBlock> blocks, int page, float y)
	{
		for (int i = 0; i < 3; i++)
		{
			blocks.add(analyzed(createBlock("block_body_" + page + "_" + i, page,
					new TextElement("Running paragraph text with enough words to look like body content.",
							new BoundingBox(50, y + i * 20, 400, 10), page, 10.0f, "Arial"))));
		}
	}

	private TextBlock createBlock(String id, int page, TextElement... elements)
	{
		TextBlock block = new TextBlock(id, page);
		for (TextElement element : elements)
		{
			block.addTextElement(element);
		}
		return block;
	}
}
