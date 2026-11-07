/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext.reconstruction;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DropCapMerger - drop cap detection and text merging
 */
class DropCapMergerTest
{
	private DropCapMerger merger;

	@BeforeEach
	void setUp()
	{
		merger = new DropCapMerger();
	}

	// === Test case 1: Basic Latin drop cap ===

	@Test
	void testBasicLatinDropCap()
	{
		TextBlock block = createBlock("block_1", 1);
		// Drop cap "T" at 36pt
		block.addTextElement(new TextElement("T",
			new BoundingBox(29.35f, 241.85f, 23.17f, 27.25f), 1, 36.0f, "TimesNewRoman-Bold"));
		// Body text at 9pt
		block.addTextElement(new TextElement("he quick brown fox...",
			new BoundingBox(52.52f, 241.85f, 200.0f, 10.0f), 1, 9.0f, "TimesNewRoman"));
		// More body text
		block.addTextElement(new TextElement("jumps over the lazy dog.",
			new BoundingBox(29.35f, 255.0f, 200.0f, 10.0f), 1, 9.0f, "TimesNewRoman"));

		List<TextBlock> blocks = List.of(block);
		int result = merger.apply(new ArrayList<>(blocks));

		assertEquals(1, result);
		assertEquals(2, block.getTextElements().size());

		TextElement merged = block.getTextElements().get(0);
		assertEquals("The quick brown fox...", merged.getText());
		assertTrue(merged.isDropCapMerged());
		assertNotNull(merged.getDropCapBbox());
		assertEquals(29.35f, merged.getDropCapBbox().getX());
	}

	// === Test case 2: Czech text drop cap ===

	@Test
	void testCzechDropCap()
	{
		TextBlock block = createBlock("block_2", 1);
		// Drop cap "A" at 34pt
		block.addTextElement(new TextElement("A",
			new BoundingBox(30.0f, 100.0f, 22.0f, 26.0f), 1, 34.0f, "Arial-Bold"));
		// Czech body text at 8pt
		block.addTextElement(new TextElement("I se nachazi...",
			new BoundingBox(52.0f, 100.0f, 180.0f, 9.0f), 1, 8.0f, "Arial"));
		// Additional body text
		block.addTextElement(new TextElement("dalsi radek textu",
			new BoundingBox(30.0f, 112.0f, 180.0f, 9.0f), 1, 8.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		int result = merger.apply(blocks);

		// "A" + "I se nachazi..." → "AI se nachazi..." (2-letter abbreviation)
		assertEquals(1, result);
		assertEquals("AI se nachazi...", block.getTextElements().get(0).getText());
		assertTrue(block.getTextElements().get(0).isDropCapMerged());
	}

	@Test
	void testCzechDropCapWithLowercase()
	{
		TextBlock block = createBlock("block_2b", 1);
		// Drop cap "P" at 34pt
		block.addTextElement(new TextElement("P",
			new BoundingBox(30.0f, 100.0f, 22.0f, 26.0f), 1, 34.0f, "Arial-Bold"));
		// Czech body text starting with lowercase
		block.addTextElement(new TextElement("rvni radek clanku...",
			new BoundingBox(52.0f, 100.0f, 180.0f, 9.0f), 1, 8.0f, "Arial"));
		block.addTextElement(new TextElement("dalsi radek textu",
			new BoundingBox(30.0f, 112.0f, 180.0f, 9.0f), 1, 8.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		int result = merger.apply(blocks);

		assertEquals(1, result);
		assertEquals("Prvni radek clanku...", block.getTextElements().get(0).getText());
		assertTrue(block.getTextElements().get(0).isDropCapMerged());
	}

	// === Test case 3: Cyrillic drop cap ===

	@Test
	void testCyrillicDropCap()
	{
		TextBlock block = createBlock("block_3", 1);
		block.addTextElement(new TextElement("\u0412",
			new BoundingBox(30.0f, 100.0f, 22.0f, 35.0f), 1, 32.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("\u044b\u0441\u043e\u043a\u0438\u0435 \u0442\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u0438...",
			new BoundingBox(52.0f, 100.0f, 200.0f, 11.0f), 1, 10.0f, "Arial"));
		block.addTextElement(new TextElement("\u0434\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0439 \u0442\u0435\u043a\u0441\u0442",
			new BoundingBox(30.0f, 115.0f, 200.0f, 11.0f), 1, 10.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		int result = merger.apply(blocks);

		assertEquals(1, result);
		assertTrue(block.getTextElements().get(0).getText().startsWith("\u0412\u044b\u0441\u043e\u043a\u0438\u0435"));
		assertTrue(block.getTextElements().get(0).isDropCapMerged());
	}

	// === Test case 4: Digit - should NOT merge ===

	@Test
	void testDigitNotMerged()
	{
		TextBlock block = createBlock("block_4", 1);
		block.addTextElement(new TextElement("3",
			new BoundingBox(30.0f, 100.0f, 20.0f, 25.0f), 1, 30.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("rd quarter results...",
			new BoundingBox(52.0f, 100.0f, 180.0f, 10.0f), 1, 9.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		assertEquals(0, merger.apply(blocks));
		assertEquals("3", block.getTextElements().get(0).getText());
	}

	// === Test case 5: Standalone heading with gap - should NOT merge ===

	@Test
	void testStandaloneHeadingNotMerged()
	{
		TextBlock block = createBlock("block_5", 1);
		block.addTextElement(new TextElement("A",
			new BoundingBox(30.0f, 50.0f, 20.0f, 25.0f), 1, 24.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("introduction to the topic",
			new BoundingBox(30.0f, 150.0f, 200.0f, 10.0f), 1, 9.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		assertEquals(0, merger.apply(blocks));
	}

	// === Test case 6: No drop caps in document ===

	@Test
	void testNoDropCapsRegression()
	{
		TextBlock block = createBlock("block_6", 1);
		block.addTextElement(new TextElement("Regular heading",
			new BoundingBox(30.0f, 100.0f, 120.0f, 14.0f), 1, 14.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("Normal body text line 1",
			new BoundingBox(30.0f, 118.0f, 180.0f, 10.0f), 1, 10.0f, "Arial"));
		block.addTextElement(new TextElement("Normal body text line 2",
			new BoundingBox(30.0f, 132.0f, 180.0f, 10.0f), 1, 10.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		assertEquals(0, merger.apply(blocks));
		assertEquals(3, block.getTextElements().size());
	}

	// === Helper method tests ===

	@Test
	void testIsDropCapCandidate_singleLetter()
	{
		List<TextElement> elements = createBodyElements();
		TextElement candidate = new TextElement("T",
			new BoundingBox(10.0f, 10.0f, 20.0f, 25.0f), 1, 30.0f, "Arial");
		assertTrue(merger.isDropCapCandidate(candidate, 10.0f, 12.0f, 10.0f, elements));
	}

	@Test
	void testIsDropCapCandidate_multipleChars()
	{
		List<TextElement> elements = createBodyElements();
		TextElement candidate = new TextElement("Th",
			new BoundingBox(10.0f, 10.0f, 20.0f, 25.0f), 1, 30.0f, "Arial");
		assertFalse(merger.isDropCapCandidate(candidate, 10.0f, 12.0f, 10.0f, elements));
	}

	@Test
	void testIsDropCapCandidate_fontTooSmall()
	{
		List<TextElement> elements = createBodyElements();
		TextElement candidate = new TextElement("T",
			new BoundingBox(10.0f, 10.0f, 20.0f, 25.0f), 1, 15.0f, "Arial");
		assertFalse(merger.isDropCapCandidate(candidate, 10.0f, 12.0f, 10.0f, elements));
	}

	@Test
	void testIsDropCapCandidate_notAtLeftEdge()
	{
		List<TextElement> elements = createBodyElements();
		TextElement candidate = new TextElement("T",
			new BoundingBox(200.0f, 10.0f, 20.0f, 25.0f), 1, 30.0f, "Arial");
		assertFalse(merger.isDropCapCandidate(candidate, 10.0f, 12.0f, 10.0f, elements));
	}

	@Test
	void testIsPlausibleWordBeginning_lowercase()
	{
		assertTrue(merger.isPlausibleWordBeginning("T", "he quick"));
	}

	@Test
	void testIsPlausibleWordBeginning_uppercase()
	{
		assertFalse(merger.isPlausibleWordBeginning("A", "Introduction"));
	}

	@Test
	void testIsPlausibleWordBeginning_abbreviation()
	{
		// "A" + "I se nachází" → "AI se nachází" (2-letter abbreviation)
		assertTrue(merger.isPlausibleWordBeginning("A", "I se nachází"));
		// "E" + "U navyšuje" → "EU navyšuje"
		assertTrue(merger.isPlausibleWordBeginning("E", "U navyšuje"));
	}

	@Test
	void testIsPlausibleWordBeginning_uppercaseNoSpace()
	{
		// "A" + "BC" → not followed by space, reject
		assertFalse(merger.isPlausibleWordBeginning("A", "BC"));
	}

	@Test
	void testIsPlausibleWordBeginning_space()
	{
		assertFalse(merger.isPlausibleWordBeginning("T", " he quick"));
	}

	@Test
	void testIsPlausibleWordBeginning_digit()
	{
		assertFalse(merger.isPlausibleWordBeginning("A", "123 items"));
	}

	@Test
	void testIsPlausibleWordBeginning_empty()
	{
		assertFalse(merger.isPlausibleWordBeginning("T", ""));
		assertFalse(merger.isPlausibleWordBeginning("T", null));
	}

	@Test
	void testCalculateMedianFontSize()
	{
		List<TextElement> elements = new ArrayList<>();
		elements.add(new TextElement("a", new BoundingBox(0, 0, 10, 10), 1, 9.0f, "Arial"));
		elements.add(new TextElement("b", new BoundingBox(0, 0, 10, 10), 1, 10.0f, "Arial"));
		elements.add(new TextElement("c", new BoundingBox(0, 0, 10, 10), 1, 36.0f, "Arial"));
		elements.add(new TextElement("d", new BoundingBox(0, 0, 10, 10), 1, 9.0f, "Arial"));
		elements.add(new TextElement("e", new BoundingBox(0, 0, 10, 10), 1, 9.0f, "Arial"));
		assertEquals(9.0f, merger.calculateMedianFontSize(elements));
	}

	@Test
	void testBoundingBoxUnionAfterMerge()
	{
		TextBlock block = createBlock("block_bbox", 1);
		block.addTextElement(new TextElement("T",
			new BoundingBox(29.35f, 241.85f, 23.17f, 27.25f), 1, 36.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("he quick brown fox...",
			new BoundingBox(52.52f, 241.85f, 200.0f, 10.0f), 1, 9.0f, "Arial"));
		block.addTextElement(new TextElement("second line",
			new BoundingBox(29.35f, 255.0f, 200.0f, 10.0f), 1, 9.0f, "Arial"));

		merger.apply(List.of(block));

		TextElement merged = block.getTextElements().get(0);
		assertEquals(29.35f, merged.getBbox().getX(), 0.01f);
		assertTrue(merged.getBbox().getWidth() > 200.0f);
	}

	@Test
	void testMultipleBlocksOnlyOneWithDropCap()
	{
		TextBlock block1 = createBlock("block_a", 1);
		block1.addTextElement(new TextElement("T",
			new BoundingBox(30.0f, 100.0f, 22.0f, 32.0f), 1, 36.0f, "Arial-Bold"));
		block1.addTextElement(new TextElement("he first paragraph...",
			new BoundingBox(52.0f, 100.0f, 180.0f, 10.0f), 1, 9.0f, "Arial"));
		block1.addTextElement(new TextElement("continues here",
			new BoundingBox(30.0f, 114.0f, 180.0f, 10.0f), 1, 9.0f, "Arial"));

		TextBlock block2 = createBlock("block_b", 1);
		block2.addTextElement(new TextElement("Normal text",
			new BoundingBox(30.0f, 200.0f, 120.0f, 10.0f), 1, 10.0f, "Arial"));
		block2.addTextElement(new TextElement("More normal text",
			new BoundingBox(30.0f, 214.0f, 150.0f, 10.0f), 1, 10.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block1);
		blocks.add(block2);

		assertEquals(1, merger.apply(blocks));
		assertEquals("The first paragraph...", block1.getTextElements().get(0).getText());
		assertEquals(2, block2.getTextElements().size());
	}

	@Test
	void testEmptyBlockList()
	{
		assertEquals(0, merger.apply(new ArrayList<>()));
	}

	@Test
	void testSingleElementBlock()
	{
		TextBlock block = createBlock("block_single", 1);
		block.addTextElement(new TextElement("T",
			new BoundingBox(30.0f, 100.0f, 22.0f, 26.0f), 1, 36.0f, "Arial-Bold"));
		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		assertEquals(0, merger.apply(blocks));
	}

	@Test
	void testPipelineIntegration()
	{
		TextReconstructionPipeline pipeline = new TextReconstructionPipeline();
		pipeline.addTransform(new DropCapMerger());
		assertEquals(1, pipeline.getTransformCount());

		TextBlock block = createBlock("block_pipe", 1);
		block.addTextElement(new TextElement("T",
			new BoundingBox(30.0f, 100.0f, 22.0f, 32.0f), 1, 36.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("he pipeline test...",
			new BoundingBox(52.0f, 100.0f, 180.0f, 10.0f), 1, 9.0f, "Arial"));
		block.addTextElement(new TextElement("second line of text",
			new BoundingBox(30.0f, 114.0f, 180.0f, 10.0f), 1, 9.0f, "Arial"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		assertEquals(1, pipeline.process(blocks, false));
		assertEquals("The pipeline test...", block.getTextElements().get(0).getText());
	}

	// === Real PDF regression test (article page 1, block_4) ===

	@Test
	void testRealPdf_articlePage1_dropCapAI()
	{
		// Real data from "2026-02-23 Článek o AI.pdf" page 1, block_4
		// Drop cap "A" + body text "I se nachází..." → "AI se nachází..."
		TextBlock block = createBlock("block_4", 1);
		block.addTextElement(new TextElement("I se nachází v klíčovém bode",
			new BoundingBox(52.5f, 234.7f, 105.3f, 5.7f), 1, 6.0f, "TimesNewRomanPSMT"));
		block.addTextElement(new TextElement("A",
			new BoundingBox(29.4f, 241.8f, 23.2f, 27.3f), 1, 34.0f, "ArialMT"));
		block.addTextElement(new TextElement("své evoluce. Z experimen\u00AD",
			new BoundingBox(52.5f, 245.4f, 97.5f, 5.7f), 1, 8.0f, "TimesNewRomanPSMT"));
		block.addTextElement(new TextElement("tální technologie, která fasci\u00AD",
			new BoundingBox(52.5f, 256.6f, 108.9f, 5.7f), 1, 9.0f, "TimesNewRomanPSMT"));
		block.addTextElement(new TextElement("novala především úzký okruh",
			new BoundingBox(52.5f, 267.7f, 109.2f, 5.7f), 1, 9.0f, "TimesNewRomanPSMT"));
		block.addTextElement(new TextElement("odborníků, se stala všudypřítomná",
			new BoundingBox(28.9f, 278.9f, 128.4f, 5.7f), 1, 9.0f, "TimesNewRomanPSMT"));
		block.addTextElement(new TextElement("realita proměňující způsob práce,",
			new BoundingBox(28.9f, 290.1f, 124.3f, 5.7f), 1, 9.0f, "TimesNewRomanPSMT"));
		block.addTextElement(new TextElement("komunikace i každodenního života.",
			new BoundingBox(28.6f, 301.3f, 131.9f, 5.7f), 1, 9.0f, "TimesNewRomanPSMT"));

		List<TextBlock> blocks = new ArrayList<>();
		blocks.add(block);
		int result = merger.apply(blocks);

		assertEquals(1, result, "Should detect drop cap A");
		// First element after merge should be the merged "AI se nachází..."
		TextElement merged = block.getTextElements().stream()
			.filter(TextElement::isDropCapMerged)
			.findFirst()
			.orElse(null);
		assertNotNull(merged, "Should have a merged element");
		assertEquals("AI se nachází v klíčovém bode", merged.getText());
		assertTrue(merged.isDropCapMerged());
		assertNotNull(merged.getDropCapBbox());
		assertEquals(29.4f, merged.getDropCapBbox().getX(), 0.1f);
	}

	// === Helpers ===

	private TextBlock createBlock(String id, int page)
	{
		return new TextBlock(id, page);
	}

	private List<TextElement> createBodyElements()
	{
		List<TextElement> elements = new ArrayList<>();
		elements.add(new TextElement("T",
			new BoundingBox(10.0f, 10.0f, 20.0f, 25.0f), 1, 30.0f, "Arial"));
		elements.add(new TextElement("he body text",
			new BoundingBox(30.0f, 10.0f, 100.0f, 10.0f), 1, 10.0f, "Arial"));
		elements.add(new TextElement("second line",
			new BoundingBox(10.0f, 25.0f, 100.0f, 10.0f), 1, 10.0f, "Arial"));
		return elements;
	}
}
