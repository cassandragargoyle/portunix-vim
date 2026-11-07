/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita;

import org.cassandragargoyle.morphodita.internal.ModelManager;
import org.cassandragargoyle.morphodita.internal.NativeLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Java wrapper for MorphoDiTa C++ library.
 * Provides morphological analysis, lemmatization, and POS tagging
 * for Czech, Slovak, and English.
 *
 * <p>Usage example:
 * <pre>{@code
 * try (MorphoDiTa md = MorphoDiTa.load("/path/to/czech.tagger")) {
 *     LemmaResult result = md.lemmatize("programování");
 *     // result.lemma() = "programování"
 *     // result.tag() = "NNNS1-----A----"
 *
 *     List<TaggedToken> tokens = md.process("Faktura za služby");
 *     // tokens contains lemmatized words with POS tags
 * }
 * }</pre>
 *
 * <p>Thread Safety: MorphoDiTa instances are thread-safe for read operations
 * (lemmatize, analyze, process). Create separate instances for concurrent write operations.
 */
public class MorphoDiTa implements AutoCloseable
{
    /** Library version matching the native MorphoDiTa version */
    public static final String VERSION = "1.11.3";

    private final long taggerHandle;
    private final long morphoHandle;
    private final String modelPath;
    private volatile boolean closed = false;

    private MorphoDiTa(long taggerHandle, long morphoHandle, String modelPath)
    {
        this.taggerHandle = taggerHandle;
        this.morphoHandle = morphoHandle;
        this.modelPath = modelPath;
    }

    /**
     * Load tagger with default Czech model from Portunix data directory.
     *
     * @return MorphoDiTa instance
     * @throws IllegalStateException if model not found or native library unavailable
     */
    public static MorphoDiTa loadDefault()
    {
        return loadForLanguage("czech");
    }

    /**
     * Load tagger for specified language from Portunix data directory.
     *
     * @param language Language code (e.g., "czech", "english", "slovak")
     * @return MorphoDiTa instance
     * @throws IllegalStateException if model not found or native library unavailable
     */
    public static MorphoDiTa loadForLanguage(String language)
    {
        ModelManager modelManager = new ModelManager();
        Optional<Path> modelPath = modelManager.findModel(language);

        if (modelPath.isEmpty())
        {
            throw new IllegalStateException(
                "Model for language '" + language + "' not found. " +
                "Install using: portunix install morphodita-model-" + language.toLowerCase() +
                ". Models directory: " + modelManager.getModelsDirectory()
            );
        }

        return load(modelPath.get().toString());
    }

    /**
     * Load tagger from external model file.
     *
     * @param modelPath Path to .tagger model file
     * @return MorphoDiTa instance
     * @throws IllegalStateException if model cannot be loaded or native library unavailable
     * @throws IllegalArgumentException if model file does not exist
     */
    public static MorphoDiTa load(String modelPath)
    {
        if (modelPath == null || modelPath.isBlank())
        {
            throw new IllegalArgumentException("Model path cannot be null or empty");
        }

        Path path = Path.of(modelPath);
        if (!Files.isRegularFile(path))
        {
            throw new IllegalArgumentException("Model file does not exist: " + modelPath);
        }

        if (!NativeLoader.load())
        {
            throw new IllegalStateException(
                "Failed to load MorphoDiTa native library: " + NativeLoader.getLoadError() +
                ". Run scripts/fetch-natives.sh to download pre-built binaries."
            );
        }

        long taggerHandle = nLoadTagger(modelPath);
        if (taggerHandle == 0)
        {
            throw new IllegalStateException("Failed to load tagger from: " + modelPath);
        }

        long morphoHandle = nGetMorpho(taggerHandle);
        if (morphoHandle == 0)
        {
            nDestroyTagger(taggerHandle);
            throw new IllegalStateException("Failed to get morpho instance from tagger");
        }

        return new MorphoDiTa(taggerHandle, morphoHandle, modelPath);
    }

    /**
     * Lemmatize a single word using tagger context.
     *
     * @param word Word to lemmatize
     * @return Lemma and morphological tag
     * @throws IllegalStateException if instance has been closed
     */
    public LemmaResult lemmatize(String word)
    {
        ensureOpen();

        if (word == null || word.isBlank())
        {
            return new LemmaResult(word, "", word);
        }

        // Use tagger for context-aware lemmatization
        String[] result = nTag(taggerHandle, word);
        if (result != null && result.length >= 2)
        {
            String rawLemma = result[0];
            String tag = result[1];
            String cleanLemma = cleanLemma(rawLemma);
            return new LemmaResult(cleanLemma, tag, rawLemma);
        }

        return new LemmaResult(word, "", word);
    }

    /**
     * Get all possible morphological analyses for a word.
     * Returns all lemma/tag combinations without context disambiguation.
     *
     * @param word Word to analyze
     * @return List of possible lemma+tag combinations
     * @throws IllegalStateException if instance has been closed
     */
    public List<LemmaResult> analyze(String word)
    {
        ensureOpen();

        if (word == null || word.isBlank())
        {
            return Collections.emptyList();
        }

        String[][] analyses = nAnalyze(morphoHandle, word);
        if (analyses == null || analyses.length == 0)
        {
            return Collections.emptyList();
        }

        List<LemmaResult> results = new ArrayList<>(analyses.length);
        for (String[] analysis : analyses)
        {
            if (analysis.length >= 2)
            {
                String rawLemma = analysis[0];
                String tag = analysis[1];
                String cleanLemma = cleanLemma(rawLemma);
                results.add(new LemmaResult(cleanLemma, tag, rawLemma));
            }
        }
        return results;
    }

    /**
     * Lemmatize and tag entire text.
     * Tokenizes the text and returns list of tokens with lemmas and POS tags.
     *
     * @param text Text to process
     * @return List of tokens with lemmas and tags
     * @throws IllegalStateException if instance has been closed
     */
    public List<TaggedToken> process(String text)
    {
        ensureOpen();

        if (text == null || text.isBlank())
        {
            return Collections.emptyList();
        }

        String[][] tokens = nProcess(taggerHandle, text);
        if (tokens == null || tokens.length == 0)
        {
            return Collections.emptyList();
        }

        List<TaggedToken> results = new ArrayList<>(tokens.length);
        for (String[] token : tokens)
        {
            if (token.length >= 4)
            {
                String form = token[0];
                String rawLemma = token[1];
                String tag = token[2];
                int start = parseIntSafe(token[3], -1);
                int end = start >= 0 ? start + form.length() : -1;
                String cleanLemma = cleanLemma(rawLemma);
                results.add(new TaggedToken(form, cleanLemma, tag, start, end));
            }
        }
        return results;
    }

    /**
     * Get the model path this instance was loaded from.
     *
     * @return Path to model file
     */
    public String getModelPath()
    {
        return modelPath;
    }

    /**
     * Check if MorphoDiTa native library is available on this platform.
     *
     * @return true if native library can be loaded
     */
    public static boolean isAvailable()
    {
        return NativeLoader.isAvailable();
    }

    /**
     * Get current library version.
     *
     * @return Version string
     */
    public static String getVersion()
    {
        return VERSION;
    }

    /**
     * Check for newer versions on GitHub.
     *
     * @return UpdateInfo if update available, empty if up to date or check failed
     */
    public static Optional<UpdateInfo> checkForUpdates()
    {
        return VersionChecker.checkForUpdates();
    }

    /**
     * Get list of available models from Portunix data directory.
     *
     * @return List of available model names (languages)
     */
    public static List<String> availableModels()
    {
        ModelManager modelManager = new ModelManager();
        return modelManager.listModelNames();
    }

    /**
     * Release native resources.
     * After calling this method, the instance cannot be used.
     */
    @Override
    public void close()
    {
        if (!closed)
        {
            closed = true;
            nDestroyTagger(taggerHandle);
        }
    }

    private void ensureOpen()
    {
        if (closed)
        {
            throw new IllegalStateException("MorphoDiTa instance has been closed");
        }
    }

    /**
     * Clean lemma by removing technical suffixes.
     * MorphoDiTa may return lemmas with suffixes like "-1" or "_^" for disambiguation.
     */
    private static String cleanLemma(String rawLemma)
    {
        if (rawLemma == null)
        {
            return null;
        }

        // Remove technical suffixes (e.g., "být-1", "velký_^(*2ý)")
        int suffixStart = rawLemma.indexOf('-');
        if (suffixStart < 0)
        {
            suffixStart = rawLemma.indexOf('_');
        }
        if (suffixStart < 0)
        {
            suffixStart = rawLemma.indexOf('`');
        }

        return suffixStart > 0 ? rawLemma.substring(0, suffixStart) : rawLemma;
    }

    private static int parseIntSafe(String value, int defaultValue)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    // Native methods - implemented in libmorphodita_java.so
    private static native long nLoadTagger(String modelPath);
    private static native long nGetMorpho(long taggerHandle);
    private static native String[] nTag(long taggerHandle, String word);
    private static native String[][] nAnalyze(long morphoHandle, String word);
    private static native String[][] nProcess(long taggerHandle, String text);
    private static native void nDestroyTagger(long taggerHandle);
}
