/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Barcode model
 */
class BarcodeTest
{
	@Test
	void testConstructor()
	{
		Barcode barcode = new Barcode("barcode_1", 1);

		assertEquals("barcode_1", barcode.getId());
		assertEquals(1, barcode.getPage());
		assertEquals(1.0, barcode.getConfidence());
	}

	@Test
	void testIsSpaydPayment_true()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent("SPD*1.0*ACC:CZ123*AM:100");

		assertTrue(barcode.isSpaydPayment());
	}

	@Test
	void testIsSpaydPayment_false()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent("https://example.com");

		assertFalse(barcode.isSpaydPayment());
	}

	@Test
	void testIsSpaydPayment_null()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent(null);

		assertFalse(barcode.isSpaydPayment());
	}

	@Test
	void testIsUrl_http()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent("http://example.com");

		assertTrue(barcode.isUrl());
	}

	@Test
	void testIsUrl_https()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent("https://example.com/path?query=1");

		assertTrue(barcode.isUrl());
	}

	@Test
	void testIsUrl_false()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent("just some text");

		assertFalse(barcode.isUrl());
	}

	@Test
	void testSettersAndGetters()
	{
		Barcode barcode = new Barcode();
		barcode.setId("test_id");
		barcode.setPage(2);
		barcode.setFormat("QR_CODE");
		barcode.setRawContent("content");
		barcode.setContentType("text");
		barcode.setConfidence(0.95);
		barcode.setBbox(new BoundingBox(10, 20, 30, 40));

		assertEquals("test_id", barcode.getId());
		assertEquals(2, barcode.getPage());
		assertEquals("QR_CODE", barcode.getFormat());
		assertEquals("content", barcode.getRawContent());
		assertEquals("text", barcode.getContentType());
		assertEquals(0.95, barcode.getConfidence());
		assertNotNull(barcode.getBbox());
	}

	@Test
	void testToString()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setFormat("QR_CODE");
		barcode.setRawContent("test content");

		String str = barcode.toString();

		assertTrue(str.contains("barcode_1"));
		assertTrue(str.contains("QR_CODE"));
		assertTrue(str.contains("test content"));
	}

	@Test
	void testToString_longContent()
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setFormat("QR_CODE");
		barcode.setRawContent("a".repeat(100));

		String str = barcode.toString();

		assertTrue(str.contains("..."));
	}
}
