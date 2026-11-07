/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.tk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TKStemmer
 *
 * Note: Tests that require native libraries are conditionally enabled
 * based on TKStemmer.isAvailable()
 */
class TKStemmerTest
{
    @Test
    void shouldListSupportedLanguages()
    {
        Set<String> languages = TKStemmer.supportedLanguages();

        assertNotNull(languages);
        assertTrue(languages.size() >= 32, "Should support at least 32 languages");
        assertTrue(languages.contains("czech"));
        assertTrue(languages.contains("english"));
        assertTrue(languages.contains("german"));
        assertTrue(languages.contains("french"));
        assertTrue(languages.contains("slovak"));
    }

    @Test
    void shouldCheckLanguageSupport()
    {
        assertTrue(TKStemmer.supportsLanguage("czech"));
        assertTrue(TKStemmer.supportsLanguage("ENGLISH"));
        assertTrue(TKStemmer.supportsLanguage(" German "));
        assertFalse(TKStemmer.supportsLanguage("klingon"));
        assertFalse(TKStemmer.supportsLanguage(null));
        assertFalse(TKStemmer.supportsLanguage(""));
    }

    @Test
    void shouldThrowForUnsupportedLanguage()
    {
        assertThrows(UnsupportedLanguageException.class, () ->
            TKStemmer.create("klingon"));
    }

    @Test
    void shouldThrowForNullLanguage()
    {
        assertThrows(IllegalArgumentException.class, () ->
            TKStemmer.create(null));
    }

    @Test
    void shouldThrowForEmptyLanguage()
    {
        assertThrows(IllegalArgumentException.class, () ->
            TKStemmer.create(""));
    }

    // Tests below require native libraries to be available

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldCreateStemmer()
    {
        try (TKStemmer stemmer = TKStemmer.create("czech"))
        {
            assertNotNull(stemmer);
            assertEquals("czech", stemmer.getLanguage());
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldStemCzechWord()
    {
        try (TKStemmer stemmer = TKStemmer.create("czech"))
        {
            assertEquals("programov", stemmer.stem("programování"));
            assertEquals("faktur", stemmer.stem("fakturách"));
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldStemEnglishWord()
    {
        try (TKStemmer stemmer = TKStemmer.create("english"))
        {
            assertEquals("run", stemmer.stem("running"));
            assertEquals("connect", stemmer.stem("connection"));
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldHandleNullAndEmptyInput()
    {
        try (TKStemmer stemmer = TKStemmer.create("czech"))
        {
            assertNull(stemmer.stem(null));
            assertEquals("", stemmer.stem(""));
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldBatchStem()
    {
        try (TKStemmer stemmer = TKStemmer.create("czech"))
        {
            List<String> words = List.of("programování", "fakturách", "částka");
            List<String> stems = stemmer.stemBatch(words);

            assertNotNull(stems);
            assertEquals(3, stems.size());
            assertEquals("programov", stems.get(0));
            assertEquals("faktur", stems.get(1));
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldHandleEmptyBatch()
    {
        try (TKStemmer stemmer = TKStemmer.create("czech"))
        {
            List<String> stems = stemmer.stemBatch(List.of());
            assertNotNull(stems);
            assertTrue(stems.isEmpty());
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldThrowAfterClose()
    {
        TKStemmer stemmer = TKStemmer.create("czech");
        stemmer.close();

        assertThrows(IllegalStateException.class, () ->
            stemmer.stem("test"));
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void shouldBeUsableWithTryWithResources()
    {
        String result;
        try (TKStemmer stemmer = TKStemmer.create("czech"))
        {
            result = stemmer.stem("programování");
        }
        assertEquals("programov", result);
    }

    // Helper method for @EnabledIf
    static boolean isNativeAvailable()
    {
        return TKStemmer.isAvailable();
    }
}
