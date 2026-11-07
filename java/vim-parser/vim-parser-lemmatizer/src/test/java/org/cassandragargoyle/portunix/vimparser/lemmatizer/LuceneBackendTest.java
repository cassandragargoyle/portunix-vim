/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer;

import org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.lucene.LuceneBackend;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.StemmedToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LuceneBackend
 *
 * @author Zdenek
 * @since 2026-01-31
 */
class LuceneBackendTest
{
	private LuceneBackend backend;

	@BeforeEach
	void setUp()
	{
		backend = new LuceneBackend();
	}

	@AfterEach
	void tearDown()
	{
		backend.close();
	}

	@Test
	void testBackendIdentity()
	{
		assertEquals("lucene", backend.getBackendId());
		assertEquals("Apache Lucene Stemmer", backend.getBackendName());
		assertTrue(backend.isAvailable());
	}

	@Test
	void testSupportedLanguages()
	{
		assertTrue(backend.supportsLanguage("cs"));
		assertTrue(backend.supportsLanguage("en"));
		assertTrue(backend.supportsLanguage("de"));
		assertTrue(backend.supportsLanguage("fr"));
		assertTrue(backend.supportsLanguage("es"));
		assertTrue(backend.supportsLanguage("it"));
		assertFalse(backend.supportsLanguage("pl"));  // Polish requires separate module
	}

	@Test
	void testStemCzechText()
	{
		String text = "Faktura za služby";
		StemmedResult result = backend.stem(text, "cs");

		assertNotNull(result);
		assertEquals("cs", result.getLanguage());
		assertEquals("lucene", result.getBackend());
		assertTrue(result.getTokens().size() >= 2, "Should extract at least 2 content words");
	}

	@Test
	void testStemEnglishText()
	{
		String text = "Running tests for documentation";
		StemmedResult result = backend.stem(text, "en");

		assertNotNull(result);
		assertEquals("en", result.getLanguage());
		assertEquals("lucene", result.getBackend());
		assertTrue(result.getTokens().size() >= 3);

		// English stemmer should stem "Running" to "run", "tests" to "test", etc.
		boolean hasRunStem = result.getTokens().stream()
			.anyMatch(t -> t.getStem().equals("run"));
		assertTrue(hasRunStem, "Expected 'Running' to be stemmed to 'run'");
	}

	@Test
	void testStemGermanText()
	{
		String text = "Die Rechnung für Dienstleistungen";
		StemmedResult result = backend.stem(text, "de");

		assertNotNull(result);
		assertEquals("de", result.getLanguage());
		assertTrue(result.getTokens().size() >= 2);
	}

	@Test
	void testStemCzechInvoiceText()
	{
		String text = "Faktura č. 2024001 Dodavatel: ABC s.r.o. Odběratel: XYZ a.s. Celková částka: 12 500 Kč";
		StemmedResult result = backend.stem(text, "cs");

		assertNotNull(result);
		assertNotNull(result.getStatistics());
		assertTrue(result.getTokens().size() > 0);

		// Check statistics
		assertTrue(result.getStatistics().getTotalTokens() > 0);
		assertTrue(result.getStatistics().getUniqueStems() > 0);
		assertTrue(result.getStatistics().getUniqueStems() <= result.getStatistics().getTotalTokens());
	}

	@Test
	void testStemEmptyText()
	{
		StemmedResult result = backend.stem("", "cs");

		assertNotNull(result);
		assertEquals(0, result.getTokens().size());
		assertEquals(0, result.getStatistics().getTotalTokens());
	}

	@Test
	void testStemNullText()
	{
		StemmedResult result = backend.stem(null, "cs");

		assertNotNull(result);
		assertEquals(0, result.getTokens().size());
	}

	@Test
	void testTokenOffsets()
	{
		String text = "první druhý třetí";
		StemmedResult result = backend.stem(text, "cs");

		// Czech analyzer may filter some tokens, but we should have at least some
		assertFalse(result.getTokens().isEmpty());

		// Verify offsets match the actual positions
		for (StemmedToken token : result.getTokens())
		{
			String surface = text.substring(token.getOffset().getStart(), token.getOffset().getEnd());
			assertEquals(token.getSurface(), surface, "Surface should match text at offset");
		}
	}

	@Test
	void testUnsupportedLanguageFallback()
	{
		// Slovak is not explicitly supported, should fall back to StandardAnalyzer
		String text = "Faktúra za služby";
		StemmedResult result = backend.stem(text, "sk");

		assertNotNull(result);
		assertEquals("sk", result.getLanguage());
		assertTrue(result.getTokens().size() >= 2);
	}
}
