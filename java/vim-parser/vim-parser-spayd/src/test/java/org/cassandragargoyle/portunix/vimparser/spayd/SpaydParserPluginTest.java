/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.spayd;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.Barcode;
import org.cassandragargoyle.portunix.vimparser.model.SpaydPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpaydParserPlugin
 */
class SpaydParserPluginTest
{
	private SpaydParserPlugin plugin;

	@BeforeEach
	void setUp()
	{
		plugin = new SpaydParserPlugin();
	}

	@Test
	void testPluginMetadata()
	{
		assertEquals("spayd", plugin.getId());
		assertEquals("SPAYD Parser", plugin.getName());
		assertNotNull(plugin.getVersion());
		assertEquals(30, plugin.getPriority());
	}

	@Test
	void testDependencies()
	{
		assertTrue(plugin.getDependencies().contains("barcode"));
	}

	@Test
	void testSupportedModes()
	{
		ExecutionMode[] modes = plugin.getSupportedModes();
		assertEquals(2, modes.length);
	}

	@Test
	void testCanParse_validSpayd()
	{
		assertTrue(plugin.canParse("SPD*1.0*ACC:CZ123"));
	}

	@Test
	void testCanParse_invalidSpayd()
	{
		assertFalse(plugin.canParse("https://example.com"));
		assertFalse(plugin.canParse(null));
		assertFalse(plugin.canParse(""));
	}

	@Test
	void testParse_noBarcodes()
	{
		PluginContext context = new PluginContext();
		// No barcodes in shared data

		PluginResult result = plugin.parse(context);

		assertTrue(result.isSuccess());
		@SuppressWarnings("unchecked")
		List<SpaydPayment> payments = (List<SpaydPayment>) result.getData();
		assertTrue(payments.isEmpty());
	}

	@Test
	void testParse_withSpaydBarcode()
	{
		PluginContext context = new PluginContext();

		// Create test barcode with SPAYD content
		List<Barcode> barcodes = new ArrayList<>();
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setFormat("QR_CODE");
		barcode.setRawContent("SPD*1.0*ACC:CZ6508000000192000145399*AM:1500.00*CC:CZK*X-VS:123456*MSG:Test payment");
		barcode.setContentType("spayd");
		barcodes.add(barcode);

		context.putSharedData("barcodes", barcodes);

		PluginResult result = plugin.parse(context);

		assertTrue(result.isSuccess());
		@SuppressWarnings("unchecked")
		List<SpaydPayment> payments = (List<SpaydPayment>) result.getData();
		assertEquals(1, payments.size());

		SpaydPayment payment = payments.get(0);
		assertEquals("barcode_1", payment.getSourceBarcode());
		assertEquals("spayd", payment.getFormat());
		assertEquals("CZ6508000000192000145399", payment.getFields().getAccount());
		assertEquals(1500.00, payment.getFields().getAmount());
		assertEquals("CZK", payment.getFields().getCurrency());
		assertEquals("123456", payment.getFields().getVariableSymbol());
		assertEquals("Test payment", payment.getFields().getMessage());
	}

	@Test
	void testParse_withNonSpaydBarcode()
	{
		PluginContext context = new PluginContext();

		List<Barcode> barcodes = new ArrayList<>();
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setFormat("QR_CODE");
		barcode.setRawContent("https://example.com");
		barcode.setContentType("url");
		barcodes.add(barcode);

		context.putSharedData("barcodes", barcodes);

		PluginResult result = plugin.parse(context);

		assertTrue(result.isSuccess());
		@SuppressWarnings("unchecked")
		List<SpaydPayment> payments = (List<SpaydPayment>) result.getData();
		assertTrue(payments.isEmpty());
	}

	@Test
	void testParse_multipleBarcodes()
	{
		PluginContext context = new PluginContext();

		List<Barcode> barcodes = new ArrayList<>();

		// SPAYD barcode
		Barcode spayd = new Barcode("barcode_1", 1);
		spayd.setRawContent("SPD*1.0*ACC:CZ123*AM:100");
		barcodes.add(spayd);

		// URL barcode
		Barcode url = new Barcode("barcode_2", 1);
		url.setRawContent("https://example.com");
		barcodes.add(url);

		// Another SPAYD
		Barcode spayd2 = new Barcode("barcode_3", 2);
		spayd2.setRawContent("SPD*1.0*ACC:CZ456*AM:200");
		barcodes.add(spayd2);

		context.putSharedData("barcodes", barcodes);

		PluginResult result = plugin.parse(context);

		assertTrue(result.isSuccess());
		@SuppressWarnings("unchecked")
		List<SpaydPayment> payments = (List<SpaydPayment>) result.getData();
		assertEquals(2, payments.size());
	}

	@Test
	void testParse_confidence()
	{
		PluginContext context = new PluginContext();

		List<Barcode> barcodes = new ArrayList<>();

		// Fully specified SPAYD
		Barcode full = new Barcode("barcode_1", 1);
		full.setRawContent("SPD*1.0*ACC:CZ123*AM:100*CC:CZK*X-VS:123");
		barcodes.add(full);

		// Minimal SPAYD
		Barcode minimal = new Barcode("barcode_2", 1);
		minimal.setRawContent("SPD*1.0*ACC:CZ123");
		barcodes.add(minimal);

		context.putSharedData("barcodes", barcodes);

		PluginResult result = plugin.parse(context);

		assertTrue(result.isSuccess());
		@SuppressWarnings("unchecked")
		List<SpaydPayment> payments = (List<SpaydPayment>) result.getData();

		// Full payment should have higher confidence
		assertTrue(payments.get(0).getConfidence() > payments.get(1).getConfidence());
	}

	@Test
	void testParse_urlEncodedMessage()
	{
		PluginContext context = new PluginContext();

		List<Barcode> barcodes = new ArrayList<>();
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent("SPD*1.0*ACC:CZ123*MSG:Test%20message%20with%20spaces");
		barcodes.add(barcode);

		context.putSharedData("barcodes", barcodes);

		PluginResult result = plugin.parse(context);

		assertTrue(result.isSuccess());
		@SuppressWarnings("unchecked")
		List<SpaydPayment> payments = (List<SpaydPayment>) result.getData();

		assertEquals("Test message with spaces", payments.get(0).getFields().getMessage());
	}
}
