/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.graphics;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GraphicsExtractorPlugin
 */
class GraphicsExtractorPluginTest
{
	private GraphicsExtractorPlugin plugin;

	@BeforeEach
	void setUp()
	{
		plugin = new GraphicsExtractorPlugin();
		plugin.initialize();
	}

	@AfterEach
	void tearDown()
	{
		plugin.shutdown();
	}

	@Test
	void testPluginMetadata()
	{
		assertEquals("graphics", plugin.getId());
		assertEquals("Graphics Extractor", plugin.getName());
		assertEquals("0.1.0", plugin.getVersion());
		assertEquals(25, plugin.getPriority());
	}

	@Test
	void testPluginId_constant()
	{
		assertEquals("graphics", GraphicsExtractorPlugin.PLUGIN_ID);
	}

	@Test
	void testResultKeyGraphics_constant()
	{
		assertEquals("graphicElements", GraphicsExtractorPlugin.RESULT_KEY_GRAPHICS);
	}

	@Test
	void testDependencies_empty()
	{
		assertTrue(plugin.getDependencies().isEmpty());
	}

	@Test
	void testPredecessors_containsBarcode()
	{
		assertTrue(plugin.getPredecessors().contains("barcode"));
	}

	@Test
	void testPhase_isExtract()
	{
		assertEquals(ExecutionPhase.EXTRACT, plugin.getPhase());
	}

	@Test
	void testAutoEnabled_true()
	{
		assertTrue(plugin.isAutoEnabled());
	}

	@Test
	void testSupportedModes()
	{
		ExecutionMode[] modes = plugin.getSupportedModes();
		assertEquals(3, modes.length);
		assertEquals(ExecutionMode.SYNC, modes[0]);
		assertEquals(ExecutionMode.ASYNC_THREAD, modes[1]);
		assertEquals(ExecutionMode.ASYNC_GRPC, modes[2]);
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
	void testInitialize_success()
	{
		PluginContext context = new PluginContext();
		context.setPdfContent(null);

		PluginResult result = plugin.extract(context);

		// Should fail due to null content, not initialization error
		assertTrue(result.getErrorMessage().contains("No PDF content"));
	}

	@Test
	void testShutdown_canReinitialize()
	{
		plugin.shutdown();

		assertDoesNotThrow(() -> plugin.initialize());
	}

	@Test
	void testExtract_invalidPdf_noSharedData()
	{
		PluginContext context = new PluginContext();
		context.setPdfContent("not a valid pdf".getBytes());

		plugin.extract(context);

		// On invalid PDF, graphics won't be stored
		assertFalse(context.getSharedData().containsKey(GraphicsExtractorPlugin.RESULT_KEY_GRAPHICS));
	}
}
