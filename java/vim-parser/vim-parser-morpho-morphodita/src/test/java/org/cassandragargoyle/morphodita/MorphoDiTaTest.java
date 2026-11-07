/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MorphoDiTa wrapper.
 * Some tests require native libraries and models to be present.
 */
class MorphoDiTaTest
{
    @Test
    void shouldReportVersion()
    {
        String version = MorphoDiTa.getVersion();
        assertNotNull(version);
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"), "Version should be semantic: " + version);
    }

    @Test
    void shouldCheckAvailability()
    {
        // Just verify the method doesn't throw
        boolean available = MorphoDiTa.isAvailable();
        // Result depends on whether native libraries are present
        assertNotNull(Boolean.valueOf(available));
    }

    @Test
    void shouldRejectNullModelPath()
    {
        assertThrows(IllegalArgumentException.class, () -> MorphoDiTa.load(null));
    }

    @Test
    void shouldRejectEmptyModelPath()
    {
        assertThrows(IllegalArgumentException.class, () -> MorphoDiTa.load(""));
        assertThrows(IllegalArgumentException.class, () -> MorphoDiTa.load("   "));
    }

    @Test
    void shouldRejectNonExistentModel()
    {
        assertThrows(IllegalArgumentException.class,
            () -> MorphoDiTa.load("/nonexistent/path/model.tagger"));
    }

    @Test
    void shouldListAvailableModels()
    {
        // This should not throw even if no models are installed
        List<String> models = MorphoDiTa.availableModels();
        assertNotNull(models);
    }

    @Test
    @EnabledIf("nativeLibrariesAvailable")
    void shouldLoadDefaultModel()
    {
        try (MorphoDiTa md = MorphoDiTa.loadDefault())
        {
            assertNotNull(md);
            assertNotNull(md.getModelPath());
        }
    }

    @Test
    @EnabledIf("nativeLibrariesAvailable")
    void shouldLemmatizeCzechWord()
    {
        try (MorphoDiTa md = MorphoDiTa.loadDefault())
        {
            LemmaResult result = md.lemmatize("programování");
            assertNotNull(result);
            assertNotNull(result.lemma());
            assertNotNull(result.tag());
        }
    }

    @Test
    @EnabledIf("nativeLibrariesAvailable")
    void shouldAnalyzeCzechVerb()
    {
        try (MorphoDiTa md = MorphoDiTa.loadDefault())
        {
            List<LemmaResult> analyses = md.analyze("běželi");
            assertNotNull(analyses);
            // Verb should have multiple possible analyses
            assertFalse(analyses.isEmpty());
        }
    }

    @Test
    @EnabledIf("nativeLibrariesAvailable")
    void shouldProcessText()
    {
        try (MorphoDiTa md = MorphoDiTa.loadDefault())
        {
            List<TaggedToken> tokens = md.process("Faktura za služby");
            assertNotNull(tokens);
            assertFalse(tokens.isEmpty());
        }
    }

    @Test
    @EnabledIf("nativeLibrariesAvailable")
    void shouldHandleEmptyInput()
    {
        try (MorphoDiTa md = MorphoDiTa.loadDefault())
        {
            LemmaResult result = md.lemmatize("");
            assertNotNull(result);

            List<LemmaResult> analyses = md.analyze("");
            assertTrue(analyses.isEmpty());

            List<TaggedToken> tokens = md.process("");
            assertTrue(tokens.isEmpty());
        }
    }

    @Test
    @EnabledIf("nativeLibrariesAvailable")
    void shouldHandleNullInput()
    {
        try (MorphoDiTa md = MorphoDiTa.loadDefault())
        {
            LemmaResult result = md.lemmatize(null);
            assertNotNull(result);

            List<LemmaResult> analyses = md.analyze(null);
            assertTrue(analyses.isEmpty());

            List<TaggedToken> tokens = md.process(null);
            assertTrue(tokens.isEmpty());
        }
    }

    @Test
    @EnabledIf("nativeLibrariesAvailable")
    void shouldRejectOperationsAfterClose()
    {
        MorphoDiTa md = MorphoDiTa.loadDefault();
        md.close();

        assertThrows(IllegalStateException.class, () -> md.lemmatize("test"));
        assertThrows(IllegalStateException.class, () -> md.analyze("test"));
        assertThrows(IllegalStateException.class, () -> md.process("test"));
    }

    // Condition method for @EnabledIf
    static boolean nativeLibrariesAvailable()
    {
        return MorphoDiTa.isAvailable() && hasDefaultModel();
    }

    private static boolean hasDefaultModel()
    {
        // Check if Czech model is available
        try
        {
            String portunixData = System.getenv("PORTUNIX_DATA");
            if (portunixData != null)
            {
                Path modelsDir = Path.of(portunixData, "models", "morphodita");
                if (Files.isDirectory(modelsDir))
                {
                    try (var stream = Files.list(modelsDir))
                    {
                        return stream.anyMatch(p ->
                            p.getFileName().toString().toLowerCase().startsWith("czech") &&
                            p.getFileName().toString().endsWith(".tagger")
                        );
                    }
                }
            }
        }
        catch (Exception e)
        {
            // Ignore
        }
        return false;
    }
}
