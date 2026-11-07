/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.visualizer;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.SpaydPayment;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VimHtmlVisualizerPlugin
 */
class VimHtmlVisualizerPluginTest
{
	private VimHtmlVisualizerPlugin plugin;

	@BeforeEach
	void setUp()
	{
		plugin = new VimHtmlVisualizerPlugin();
	}

	@Test
	void testPluginMetadata()
	{
		assertEquals("html", plugin.getId());
		assertEquals("HTML Visualizer", plugin.getName());
		assertNotNull(plugin.getVersion());
		assertEquals("HTML", plugin.getOutputFormat());
		assertEquals(".vim.html", plugin.getFileExtension());
	}

	@Test
	void testVisualize_emptyDocument()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("test.pdf");
		doc.getDocument().setPages(1);

		String html = plugin.visualize(doc);

		assertNotNull(html);
		assertTrue(html.contains("<!DOCTYPE html>"));
		assertTrue(html.contains("test.pdf"));
		assertTrue(html.contains("VIM Block Visualization"));
	}

	@Test
	void testVisualize_withBlocks()
	{
		VimDocument doc = createTestDocument();

		String html = plugin.visualize(doc);

		// Check structural blocks
		assertTrue(html.contains("block_1"));
		assertTrue(html.contains("Test Header"));
		assertTrue(html.contains("Content line"));

		// Check page info displayed on structural block card
		assertTrue(html.contains("block-page"), "Should contain page badge CSS class");
		assertTrue(html.contains("Page 1"), "Should contain page section header");

		// Check tabs
		assertTrue(html.contains("Structural"));
		assertTrue(html.contains("Physical"));
		assertTrue(html.contains("Semantic"));
	}

	@Test
	void testVisualize_multiPageStructuralBlocks()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("multipage.pdf");
		doc.getDocument().setPages(3);

		// Page 1 blocks
		TextBlock block1 = new TextBlock("block_1", 1);
		block1.addTextElement(new TextElement("Page 1 Header",
			new BoundingBox(10, 10, 100, 20), 1, 14.0f, "Arial-Bold"));
		block1.analyzeStructure(12.0f);
		doc.getLayers().getStructural().addBlock(VimDocument.StructuralBlock.fromTextBlock(block1));

		// Page 2 blocks
		TextBlock block2 = new TextBlock("block_2", 2);
		block2.addTextElement(new TextElement("Page 2 Header",
			new BoundingBox(10, 10, 100, 20), 2, 14.0f, "Arial-Bold"));
		block2.analyzeStructure(12.0f);
		doc.getLayers().getStructural().addBlock(VimDocument.StructuralBlock.fromTextBlock(block2));

		TextBlock block3 = new TextBlock("block_3", 2);
		block3.addTextElement(new TextElement("Page 2 Content",
			new BoundingBox(10, 40, 100, 12), 2, 12.0f, "Arial"));
		block3.analyzeStructure(12.0f);
		doc.getLayers().getStructural().addBlock(VimDocument.StructuralBlock.fromTextBlock(block3));

		String html = plugin.visualize(doc);

		// Check page grouping
		assertTrue(html.contains("Page 1"), "Should have Page 1 section");
		assertTrue(html.contains("Page 2"), "Should have Page 2 section");
		assertTrue(html.contains("page-section-header"), "Should have page section headers");

		// Check all blocks rendered
		assertTrue(html.contains("block_1"));
		assertTrue(html.contains("block_2"));
		assertTrue(html.contains("block_3"));
	}

	@Test
	void testVisualize_structuralBlockWithoutPage()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("legacy.pdf");
		doc.getDocument().setPages(1);

		// Manually create structural block without page (page defaults to 0)
		VimDocument.StructuralBlock block = new VimDocument.StructuralBlock();
		block.setId("block_legacy");
		block.setType("paragraph");
		doc.getLayers().getStructural().addBlock(block);

		String html = plugin.visualize(doc);

		// Should render gracefully with "Unknown Page"
		assertTrue(html.contains("block_legacy"), "Should render block without page");
		assertTrue(html.contains("Unknown Page"), "Should show Unknown Page for page=0");
		assertFalse(html.contains("\"block-page\""), "Should not show page badge when page is 0");
	}

	@Test
	void testVisualize_escapesHtml()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("<script>alert('xss')</script>.pdf");
		doc.getDocument().setPages(1);

		String html = plugin.visualize(doc);

		assertFalse(html.contains("<script>alert"));
		assertTrue(html.contains("&lt;script&gt;"));
	}

	@Test
	void testVisualize_withPayments()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("invoice.pdf");
		doc.getDocument().setPages(1);

		SpaydPayment payment = new SpaydPayment();
		payment.setSourceBarcode("barcode_1");
		payment.getFields().setAccount("CZ123456789");
		payment.getFields().setAmount(1500.50);
		payment.getFields().setCurrency("CZK");
		payment.getFields().setVariableSymbol("123456");
		doc.getLayers().getSemantic().addPayment(payment);

		String html = plugin.visualize(doc);

		assertTrue(html.contains("Payment QR Codes"), "Should contain payment section header");
		assertTrue(html.contains("CZ123456789"), "Should contain account number");
		assertTrue(html.contains("PAYMENT"), "Should contain PAYMENT type label");
		assertTrue(html.contains("CZK"), "Should contain currency code");
		assertTrue(html.contains("123456"), "Should contain variable symbol");
	}

	@Test
	void testVisualize_withEntities()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("test.pdf");
		doc.getDocument().setPages(1);

		VimDocument.SemanticEntity entity = new VimDocument.SemanticEntity();
		entity.setType("customer");
		entity.setSourceBlock("block_1");
		entity.setConfidence(0.85);
		entity.addField("name", "John Doe");
		entity.addField("street", "Main Street 123");
		doc.getLayers().getSemantic().addEntity(entity);

		String html = plugin.visualize(doc);

		assertTrue(html.contains("CUSTOMER"));
		assertTrue(html.contains("John Doe"));
		assertTrue(html.contains("Main Street 123"));
		assertTrue(html.contains("85%"));
	}

	@Test
	void testVisualize_hasStyles()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("test.pdf");

		String html = plugin.visualize(doc);

		assertTrue(html.contains("<style>"));
		assertTrue(html.contains("</style>"));
		assertTrue(html.contains(".block"));
		assertTrue(html.contains(".tabs"));
	}

	@Test
	void testVisualize_hasJavaScript()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("test.pdf");

		String html = plugin.visualize(doc);

		assertTrue(html.contains("<script>"));
		assertTrue(html.contains("showLayer"));
		assertTrue(html.contains("</script>"));
	}

	private VimDocument createTestDocument()
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("test.pdf");
		doc.getDocument().setPages(2);

		// Create a text block
		TextBlock block = new TextBlock("block_1", 1);
		block.addTextElement(new TextElement("Test Header",
			new BoundingBox(10, 10, 100, 20), 1, 14.0f, "Arial-Bold"));
		block.addTextElement(new TextElement("Content line",
			new BoundingBox(10, 35, 100, 12), 1, 12.0f, "Arial"));
		block.analyzeStructure(12.0f);

		doc.getLayers().getPhysical().addBlock(VimDocument.PhysicalBlock.fromTextBlock(block));
		doc.getLayers().getStructural().addBlock(VimDocument.StructuralBlock.fromTextBlock(block));

		return doc;
	}
}
