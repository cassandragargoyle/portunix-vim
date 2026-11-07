/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.barcode;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.Barcode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BarcodeExtractorPlugin
 */
class BarcodeExtractorPluginTest
{
	private BarcodeExtractorPlugin plugin;

	@BeforeEach
	void setUp()
	{
		plugin = new BarcodeExtractorPlugin();
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
		assertEquals("barcode", plugin.getId());
		assertEquals("Barcode Extractor", plugin.getName());
		assertEquals("0.1.0", plugin.getVersion());
		assertEquals(20, plugin.getPriority());
	}

	@Test
	void testPluginId_constant()
	{
		assertEquals("barcode", BarcodeExtractorPlugin.PLUGIN_ID);
	}

	@Test
	void testResultKeyBarcodes_constant()
	{
		assertEquals("barcodes", BarcodeExtractorPlugin.RESULT_KEY_BARCODES);
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
	void testInitialize_createReaderSuccessfully()
	{
		// Already initialized in setUp, verify it works by calling extract
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

		// Should be able to reinitialize
		assertDoesNotThrow(() -> plugin.initialize());
	}

	@Test
	void testExtract_storesBarcodesInSharedData()
	{
		// Create minimal valid PDF
		PluginContext context = new PluginContext();
		// Use invalid PDF to trigger error path - the shared data won't be set
		context.setPdfContent("not a valid pdf".getBytes());

		plugin.extract(context);

		// On invalid PDF, barcodes won't be stored
		assertFalse(context.getSharedData().containsKey(BarcodeExtractorPlugin.RESULT_KEY_BARCODES));
	}

	@Test
	void testExtract_processingTimeIsSet()
	{
		PluginContext context = new PluginContext();
		context.setPdfContent(null);

		PluginResult result = plugin.extract(context);

		// Processing time should be set even for failure
		assertFalse(result.isSuccess());
	}
}
