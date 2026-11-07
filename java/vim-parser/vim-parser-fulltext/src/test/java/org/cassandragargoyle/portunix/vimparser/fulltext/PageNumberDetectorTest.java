/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PageNumberDetector
 */
class PageNumberDetectorTest
{
	private PageNumberDetector detector;

	@BeforeEach
	void setUp()
	{
		detector = new PageNumberDetector();
	}

	@Test
	void testDetect_simplePageNumber_bottomOfPage()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "Main content", 10, 100, 1));
		blocks.add(createBlock("block_2", "42", 10, 800, 1));

		int detected = detector.detect(blocks);

		assertEquals(1, detected);
		assertEquals("page_number", blocks.get(1).getType());
		assertEquals("auxiliary", blocks.get(1).getSignificance());
	}

	@Test
	void testDetect_simplePageNumber_topOfPage()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "1", 10, 20, 1));
		blocks.add(createBlock("block_2", "Main content", 10, 400, 1));

		int detected = detector.detect(blocks);

		assertEquals(1, detected);
		assertEquals("page_number", blocks.get(0).getType());
	}

	@Test
	void testDetect_dashedPageNumber()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "Content", 10, 100, 1));
		blocks.add(createBlock("block_2", "- 12 -", 10, 810, 1));

		int detected = detector.detect(blocks);

		assertEquals(1, detected);
		assertEquals("page_number", blocks.get(1).getType());
	}

	@Test
	void testDetect_fractionPageNumber()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "Content", 10, 100, 1));
		blocks.add(createBlock("block_2", "3/10", 10, 810, 1));

		int detected = detector.detect(blocks);

		assertEquals(1, detected);
		assertEquals("page_number", blocks.get(1).getType());
	}

	@Test
	void testDetect_notPageNumber_middleOfPage()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "Top content", 10, 10, 1));
		blocks.add(createBlock("block_2", "42", 10, 400, 1));
		blocks.add(createBlock("block_3", "Bottom content", 10, 800, 1));

		int detected = detector.detect(blocks);

		assertEquals(0, detected);
	}

	@Test
	void testDetect_notPageNumber_longText()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "Content", 10, 100, 1));
		blocks.add(createBlock("block_2", "This is a paragraph", 10, 800, 1));

		int detected = detector.detect(blocks);

		assertEquals(0, detected);
	}

	@Test
	void testDetect_skipsTocBlocks()
	{
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "Content", 10, 100, 1));
		TextBlock tocBlock = createBlock("block_2", "3", 10, 800, 1);
		tocBlock.setType("toc");
		blocks.add(tocBlock);

		int detected = detector.detect(blocks);

		assertEquals(0, detected);
		assertEquals("toc", blocks.get(1).getType());
	}

	@Test
	void testDetect_emptyList()
	{
		assertEquals(0, detector.detect(new ArrayList<>()));
		assertEquals(0, detector.detect(null));
	}

	@Test
	void testDetect_multiplePages_ebookFormat()
	{
		// Simulate ebook with smaller page (content Y range ~50-550)
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "Chapter text", 10, 50, 5));
		blocks.add(createBlock("block_2", "5", 300, 551, 5));
		blocks.add(createBlock("block_3", "More text", 10, 50, 6));
		blocks.add(createBlock("block_4", "6", 300, 551, 6));

		int detected = detector.detect(blocks);

		assertEquals(2, detected);
		assertEquals("page_number", blocks.get(1).getType());
		assertEquals("page_number", blocks.get(3).getType());
	}

	@Test
	void testDetect_singleBlockOnPage_noDetection()
	{
		// Single block on page - need at least 2 blocks to determine position
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(createBlock("block_1", "42", 10, 400, 1));

		int detected = detector.detect(blocks);

		assertEquals(0, detected);
	}

	@Test
	void testMatchesPageNumberPattern()
	{
		assertTrue(detector.matchesPageNumberPattern("1"));
		assertTrue(detector.matchesPageNumberPattern("42"));
		assertTrue(detector.matchesPageNumberPattern("1234"));
		assertTrue(detector.matchesPageNumberPattern("- 12 -"));
		assertTrue(detector.matchesPageNumberPattern("-5-"));
		assertTrue(detector.matchesPageNumberPattern("3/10"));
		assertTrue(detector.matchesPageNumberPattern("1 / 5"));

		assertFalse(detector.matchesPageNumberPattern(""));
		assertFalse(detector.matchesPageNumberPattern("Chapter 1"));
		assertFalse(detector.matchesPageNumberPattern("12345"));
		assertFalse(detector.matchesPageNumberPattern("abc"));
	}

	private TextBlock createBlock(String id, String text, float x, float y, int page)
	{
		TextBlock block = new TextBlock(id, page);
		block.addTextElement(new TextElement(text, new BoundingBox(x, y, 30, 12), page, 10.0f, "Arial"));
		return block;
	}
}
