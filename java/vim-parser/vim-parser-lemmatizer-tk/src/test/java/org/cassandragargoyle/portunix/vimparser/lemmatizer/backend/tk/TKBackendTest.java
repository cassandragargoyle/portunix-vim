/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.tk;

import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.tk.TKStemmer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TKBackend
 *
 * @author Zdenek
 * @since 2026-02-01
 */
class TKBackendTest
{
    private TKBackend backend;

    @BeforeEach
    void setUp()
    {
        backend = new TKBackend();
    }

    @AfterEach
    void tearDown()
    {
        if (backend != null)
        {
            backend.close();
        }
    }

    @Test
    void shouldReturnCorrectBackendId()
    {
        assertEquals("tk", backend.getBackendId());
    }

    @Test
    void shouldReturnCorrectBackendName()
    {
        assertEquals("TK Stemmer (33 languages)", backend.getBackendName());
    }

    @Test
    void shouldSupportMultipleLanguages()
    {
        Set<String> languages = backend.getSupportedLanguages();

        assertTrue(languages.size() >= 30, "Should support at least 30 languages");
        assertTrue(languages.contains("cs"), "Should support Czech");
        assertTrue(languages.contains("en"), "Should support English");
        assertTrue(languages.contains("de"), "Should support German");
        assertTrue(languages.contains("fr"), "Should support French");
        assertTrue(languages.contains("pl"), "Should support Polish");
        assertTrue(languages.contains("ru"), "Should support Russian");
    }

    @Test
    void shouldSupportLanguageCheck()
    {
        assertTrue(backend.supportsLanguage("cs"));
        assertTrue(backend.supportsLanguage("en"));
        assertFalse(backend.supportsLanguage("xx"));
        assertFalse(backend.supportsLanguage(""));
    }

    @Test
    void shouldHandleEmptyText()
    {
        StemmedResult result = backend.stem("", "cs");

        assertNotNull(result);
        assertTrue(result.getTokens().isEmpty());
        assertEquals("cs", result.getLanguage());
        assertEquals("tk", result.getBackend());
    }

    @Test
    void shouldHandleNullText()
    {
        StemmedResult result = backend.stem(null, "cs");

        assertNotNull(result);
        assertTrue(result.getTokens().isEmpty());
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldStemCzechText()
    {
        StemmedResult result = backend.stem("Faktura za služby", "cs");

        assertNotNull(result);
        assertFalse(result.getTokens().isEmpty());
        assertEquals(3, result.getTokens().size());
        assertEquals("cs", result.getLanguage());
        assertEquals("tk", result.getBackend());

        // Check first token
        assertEquals("Faktura", result.getTokens().get(0).getSurface());
        assertNotNull(result.getTokens().get(0).getStem());
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldStemEnglishText()
    {
        StemmedResult result = backend.stem("Running programs quickly", "en");

        assertNotNull(result);
        assertEquals(3, result.getTokens().size());
        assertEquals("en", result.getLanguage());
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldPreserveTokenOffsets()
    {
        String text = "Hello world";
        StemmedResult result = backend.stem(text, "en");

        assertEquals(2, result.getTokens().size());

        // "Hello" at offset 0-5
        assertEquals(0, result.getTokens().get(0).getOffset().getStart());
        assertEquals(5, result.getTokens().get(0).getOffset().getEnd());

        // "world" at offset 6-11
        assertEquals(6, result.getTokens().get(1).getOffset().getStart());
        assertEquals(11, result.getTokens().get(1).getOffset().getEnd());
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldHandleUnicodeText()
    {
        StemmedResult result = backend.stem("Příliš žluťoučký kůň", "cs");

        assertNotNull(result);
        assertEquals(3, result.getTokens().size());
        assertEquals("Příliš", result.getTokens().get(0).getSurface());
    }

    @Test
    void shouldReportAvailability()
    {
        // Just verify the method doesn't throw
        boolean available = backend.isAvailable();
        // Result depends on whether native libraries are loaded
        assertNotNull(Boolean.valueOf(available));
    }

    // Condition for @EnabledIf
    static boolean isNativeAvailable()
    {
        return TKStemmer.isAvailable();
    }
}
