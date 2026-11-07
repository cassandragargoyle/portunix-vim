/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TextBlock model
 */
class TextBlockTest
{
	@Test
	void testConstructor()
	{
		TextBlock block = new TextBlock("block_1", 1);

		assertEquals("block_1", block.getId());
		assertEquals(1, block.getPage());
		assertEquals(1.0, block.getConfidence());
		assertNotNull(block.getTextElements());
		assertTrue(block.getTextElements().isEmpty());
	}

	@Test
	void testAddTextElement()
	{
		TextBlock block = new TextBlock("block_1", 1);
		TextElement element = new TextElement("Hello", new BoundingBox(10, 20, 50, 15), 1, 12.0f, "Arial");

		block.addTextElement(element);

		assertEquals(1, block.getTextElements().size());
		assertNotNull(block.getBbox());
		assertEquals(10, block.getBbox().getX());
		assertEquals(20, block.getBbox().getY());
	}

	@Test
	void testAddTextElement_expandsBbox()
	{
		TextBlock block = new TextBlock("block_1", 1);
		TextElement element1 = new TextElement("Hello", new BoundingBox(10, 20, 50, 15), 1, 12.0f, "Arial");
		TextElement element2 = new TextElement("World", new BoundingBox(100, 50, 60, 15), 1, 12.0f, "Arial");

		block.addTextElement(element1);
		block.addTextElement(element2);

		assertEquals(2, block.getTextElements().size());
		// Bbox should expand to include both elements
		assertEquals(10, block.getBbox().getX());
		assertEquals(20, block.getBbox().getY());
		assertEquals(150, block.getBbox().getWidth());  // 160 - 10
	}

	@Test
	void testGetFullText()
	{
		TextBlock block = new TextBlock("block_1", 1);
		block.addTextElement(new TextElement("Hello", new BoundingBox(10, 20, 50, 15), 1, 12.0f, "Arial"));
		block.addTextElement(new TextElement("World", new BoundingBox(70, 20, 50, 15), 1, 12.0f, "Arial"));

		assertEquals("Hello World", block.getFullText());
	}

	@Test
	void testGetFullText_empty()
	{
		TextBlock block = new TextBlock("block_1", 1);
		assertEquals("", block.getFullText());
	}

	@Test
	void testAnalyzeStructure_withHeader()
	{
		TextBlock block = new TextBlock("block_1", 1);
		// Large font = header
		block.addTextElement(new TextElement("Title", new BoundingBox(10, 10, 50, 20), 1, 16.0f, "Arial-Bold"));
		// Normal font = content
		block.addTextElement(new TextElement("Content line 1", new BoundingBox(10, 35, 100, 12), 1, 12.0f, "Arial"));
		block.addTextElement(new TextElement("Content line 2", new BoundingBox(10, 50, 100, 12), 1, 12.0f, "Arial"));

		block.analyzeStructure(12.0f);

		assertEquals("Title", block.getHeader());
		assertEquals("labeled_content", block.getType());
		assertEquals(2, block.getContent().size());
		assertTrue(block.getContent().contains("Content line 1"));
		assertTrue(block.getContent().contains("Content line 2"));
	}

	@Test
	void testAnalyzeStructure_noHeader()
	{
		TextBlock block = new TextBlock("block_1", 1);
		// All same font size, long text = no header
		block.addTextElement(new TextElement("This is a long sentence that is not a header",
			new BoundingBox(10, 10, 200, 12), 1, 12.0f, "Arial"));
		block.addTextElement(new TextElement("Another line", new BoundingBox(10, 25, 100, 12), 1, 12.0f, "Arial"));

		block.analyzeStructure(12.0f);

		assertEquals("content", block.getType());
	}

	@Test
	void testToString()
	{
		TextBlock block = new TextBlock("block_1", 1);
		block.setHeader("Test Header");
		block.addTextElement(new TextElement("text", new BoundingBox(10, 10, 50, 15), 1, 12.0f, "Arial"));

		String str = block.toString();

		assertTrue(str.contains("block_1"));
		assertTrue(str.contains("Test Header"));
	}
}
