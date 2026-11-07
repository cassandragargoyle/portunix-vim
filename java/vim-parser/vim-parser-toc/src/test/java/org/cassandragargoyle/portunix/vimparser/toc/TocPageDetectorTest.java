/*
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for explicit TOC page detection
 *
 * @author Zdenek
 * @since 2026-03-07
 */
class TocPageDetectorTest
{
	private TocPageDetector detector;

	@BeforeEach
	void setUp()
	{
		detector = new TocPageDetector();
	}

	@Test
	void detectNullBlocks()
	{
		assertNull(detector.detect(null));
	}

	@Test
	void detectEmptyBlocks()
	{
		assertNull(detector.detect(new ArrayList<>()));
	}

	@Test
	void detectBabickaTocPage()
	{
		// Simulate Babička TOC page (page 4)
		List<TextBlock> blocks = new ArrayList<>();

		// Header block with "OBSAH"
		TextBlock headerBlock = createBlock("block_1", 4,
				new TextElement("OBSAH", new BoundingBox(200, 50, 100, 14), 4, 14.0f, "Arial-Bold"));
		headerBlock.analyzeStructure(10.0f);
		blocks.add(headerBlock);

		// TOC entries block
		TextBlock tocBlock = createBlock("block_2", 4,
				new TextElement("Úvod.........................................5", new BoundingBox(50, 80, 400, 10), 4, 10.0f, "Arial"),
				new TextElement("I. ..............................................6", new BoundingBox(50, 95, 400, 10), 4, 10.0f, "Arial"),
				new TextElement("II. ...........................................14", new BoundingBox(50, 110, 400, 10), 4, 10.0f, "Arial"),
				new TextElement("III...........................................20", new BoundingBox(50, 125, 400, 10), 4, 10.0f, "Arial"),
				new TextElement("IV............................................28", new BoundingBox(50, 140, 400, 10), 4, 10.0f, "Arial"),
				new TextElement("V.............................................39", new BoundingBox(50, 155, 400, 10), 4, 10.0f, "Arial"));
		tocBlock.analyzeStructure(10.0f);
		blocks.add(tocBlock);

		// Body block on different page (should not interfere)
		TextBlock bodyBlock = createBlock("block_3", 5,
				new TextElement("Regular body text on page 5.", new BoundingBox(50, 50, 400, 10), 5, 10.0f, "Arial"));
		bodyBlock.analyzeStructure(10.0f);
		blocks.add(bodyBlock);

		TocPageDetector.TocPageResult result = detector.detect(blocks);

		assertNotNull(result);
		assertEquals(4, result.getTocPage());
		assertTrue(result.getEntries().size() >= 5);

		// Verify first entry
		TocEntry first = result.getEntries().get(0);
		assertEquals("Úvod", first.getTitle());
		assertEquals(5, first.getPage());
		assertEquals(TocEntry.SOURCE_EXPLICIT, first.getSource());
		assertEquals(0.95, first.getConfidence(), 0.01);

		// Verify roman numeral entry
		TocEntry chapter1 = result.getEntries().get(1);
		assertEquals(6, chapter1.getPage());
		assertEquals(1, chapter1.getLevel());
	}

	@Test
	void detectEnglishContentsPage()
	{
		List<TextBlock> blocks = new ArrayList<>();

		TextBlock headerBlock = createBlock("block_1", 3,
				new TextElement("Table of Contents", new BoundingBox(150, 50, 200, 16), 3, 16.0f, "Arial-Bold"));
		headerBlock.analyzeStructure(10.0f);
		blocks.add(headerBlock);

		TextBlock tocBlock = createBlock("block_2", 3,
				new TextElement("Introduction.......................1", new BoundingBox(50, 80, 400, 10), 3, 10.0f, "Arial"),
				new TextElement("Chapter 1..........................5", new BoundingBox(50, 95, 400, 10), 3, 10.0f, "Arial"),
				new TextElement("Chapter 2.........................15", new BoundingBox(50, 110, 400, 10), 3, 10.0f, "Arial"),
				new TextElement("Conclusion........................42", new BoundingBox(50, 125, 400, 10), 3, 10.0f, "Arial"));
		tocBlock.analyzeStructure(10.0f);
		blocks.add(tocBlock);

		TocPageDetector.TocPageResult result = detector.detect(blocks);

		assertNotNull(result);
		assertEquals(3, result.getTocPage());
		assertEquals(4, result.getEntries().size());
	}

	@Test
	void noTocPageInRegularDocument()
	{
		List<TextBlock> blocks = new ArrayList<>();

		for (int i = 1; i <= 5; i++)
		{
			TextBlock block = createBlock("block_" + i, 1,
					new TextElement("Regular paragraph text number " + i + " with normal content.",
							new BoundingBox(50, 50 + i * 50, 400, 10), 1, 10.0f, "Arial"));
			block.analyzeStructure(10.0f);
			blocks.add(block);
		}

		assertNull(detector.detect(blocks));
	}

	@Test
	void parseTocLineWithDots()
	{
		TocEntry entry = detector.parseTocLine("Úvod.........................................5", 4, "block_1");
		assertNotNull(entry);
		assertEquals("Úvod", entry.getTitle());
		assertEquals(5, entry.getPage());
		assertEquals(TocEntry.SOURCE_EXPLICIT, entry.getSource());
	}

	@Test
	void parseTocLineWithRomanNumeral()
	{
		TocEntry entry = detector.parseTocLine("III...........................................20", 4, "block_1");
		assertNotNull(entry);
		assertEquals("III", entry.getTitle());
		assertEquals(20, entry.getPage());
		assertEquals(1, entry.getLevel());
	}

	@Test
	void parseTocLineSkipsKeyword()
	{
		assertNull(detector.parseTocLine("OBSAH", 4, "block_1"));
		assertNull(detector.parseTocLine("Contents", 4, "block_1"));
		assertNull(detector.parseTocLine("Table of Contents", 4, "block_1"));
	}

	@Test
	void parseTocLineRegularText()
	{
		assertNull(detector.parseTocLine("Regular text without page number", 1, "block_1"));
		assertNull(detector.parseTocLine("Short", 1, "block_1"));
		assertNull(detector.parseTocLine("", 1, "block_1"));
		assertNull(detector.parseTocLine(null, 1, "block_1"));
	}

	@Test
	void tocKeywordDetection()
	{
		assertTrue(detector.isTocKeyword("OBSAH"));
		assertTrue(detector.isTocKeyword("obsah"));
		assertTrue(detector.isTocKeyword("Contents"));
		assertTrue(detector.isTocKeyword("Table of Contents"));
		assertTrue(detector.isTocKeyword("Inhalt"));
		assertFalse(detector.isTocKeyword("Chapter 1"));
		assertFalse(detector.isTocKeyword(null));
		assertFalse(detector.isTocKeyword(""));
	}

	@Test
	void levelDetection()
	{
		assertEquals(1, detector.detectLevel("I."));
		assertEquals(1, detector.detectLevel("XIV."));
		assertEquals(1, detector.detectLevel("Úvod"));
		assertEquals(1, detector.detectLevel("Introduction"));
		assertEquals(1, detector.detectLevel("Chapter 1 - Getting Started"));
		assertEquals(1, detector.detectLevel("1. Introduction"));
		assertEquals(2, detector.detectLevel("1.1 Background"));
	}

	@Test
	void tocBlockIdsAreRecorded()
	{
		List<TextBlock> blocks = new ArrayList<>();

		TextBlock headerBlock = createBlock("header_block", 4,
				new TextElement("Obsah", new BoundingBox(200, 50, 100, 14), 4, 14.0f, "Arial-Bold"));
		headerBlock.analyzeStructure(10.0f);
		blocks.add(headerBlock);

		TextBlock tocBlock = createBlock("entries_block", 4,
				new TextElement("Chapter 1.......................5", new BoundingBox(50, 80, 400, 10), 4, 10.0f, "Arial"),
				new TextElement("Chapter 2......................15", new BoundingBox(50, 95, 400, 10), 4, 10.0f, "Arial"),
				new TextElement("Chapter 3......................25", new BoundingBox(50, 110, 400, 10), 4, 10.0f, "Arial"));
		tocBlock.analyzeStructure(10.0f);
		blocks.add(tocBlock);

		TocPageDetector.TocPageResult result = detector.detect(blocks);

		assertNotNull(result);
		assertTrue(result.getTocBlockIds().contains("header_block"));
		assertTrue(result.getTocBlockIds().contains("entries_block"));
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
