/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.tk;

import org.cassandragargoyle.tk.internal.NativeLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Java wrapper for TK Stemmer C++ library.
 * Provides word stemming for 33 languages using Snowball algorithms.
 *
 * <p>Usage example:
 * <pre>{@code
 * try (TKStemmer stemmer = TKStemmer.create("czech")) {
 *     String stem = stemmer.stem("programování");  // returns "programov"
 * }
 * }</pre>
 *
 * <p>Thread Safety: Each TKStemmer instance is NOT thread-safe.
 * Create separate instances for concurrent use.
 */
public class TKStemmer implements AutoCloseable
{
    // Supported languages (Snowball names)
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
        "arabic", "armenian", "basque", "catalan", "czech", "danish", "dutch",
        "english", "finnish", "french", "german", "greek", "hindi", "hungarian",
        "indonesian", "irish", "italian", "lithuanian", "nepali", "norwegian",
        "polish", "portuguese", "romanian", "russian", "serbian", "slovak",
        "spanish", "swedish", "tamil", "turkish", "ukrainian", "yiddish"
    );

    private final long handle;
    private final String language;
    private volatile boolean closed = false;

    private TKStemmer(long handle, String language)
    {
        this.handle = handle;
        this.language = language;
    }

    /**
     * Create stemmer for specified language.
     *
     * @param language Snowball language name (e.g., "czech", "english", "german")
     * @return Stemmer instance
     * @throws UnsupportedLanguageException if language not supported
     * @throws UnsatisfiedLinkError if native library cannot be loaded
     */
    public static TKStemmer create(String language)
    {
        if (language == null || language.isBlank())
        {
            throw new IllegalArgumentException("Language cannot be null or empty");
        }

        String normalizedLang = language.toLowerCase().trim();

        if (!SUPPORTED_LANGUAGES.contains(normalizedLang))
        {
            throw new UnsupportedLanguageException(language);
        }

        if (!NativeLoader.load())
        {
            throw new UnsatisfiedLinkError("Failed to load TK native library: " + NativeLoader.getLoadError());
        }

        long handle = nCreate(normalizedLang);
        if (handle == 0)
        {
            throw new UnsupportedLanguageException(language);
        }

        return new TKStemmer(handle, normalizedLang);
    }

    /**
     * Stem a single word.
     *
     * @param word Word to stem (UTF-8)
     * @return Stemmed word, or original word if stemming fails
     * @throws IllegalStateException if stemmer has been closed
     */
    public String stem(String word)
    {
        ensureOpen();

        if (word == null || word.isEmpty())
        {
            return word;
        }

        String result = nStem(handle, word);
        return result != null ? result : word;
    }

    /**
     * Stem multiple words (batch operation for performance).
     *
     * @param words List of words to stem
     * @return List of stemmed words in same order
     * @throws IllegalStateException if stemmer has been closed
     */
    public List<String> stemBatch(List<String> words)
    {
        ensureOpen();

        if (words == null || words.isEmpty())
        {
            return Collections.emptyList();
        }

        String[] input = words.toArray(new String[0]);
        String[] result = nStemBatch(handle, input);

        return result != null ? Arrays.asList(result) : words;
    }

    /**
     * Get the language this stemmer was created for.
     *
     * @return Language name (Snowball format)
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * Get list of all supported languages.
     *
     * @return Unmodifiable set of Snowball language names
     */
    public static Set<String> supportedLanguages()
    {
        return SUPPORTED_LANGUAGES;
    }

    /**
     * Check if a specific language is supported.
     *
     * @param language Language name to check
     * @return true if language is supported
     */
    public static boolean supportsLanguage(String language)
    {
        if (language == null)
        {
            return false;
        }
        return SUPPORTED_LANGUAGES.contains(language.toLowerCase().trim());
    }

    /**
     * Check if TK native library is available on this platform.
     *
     * @return true if native library can be loaded
     */
    public static boolean isAvailable()
    {
        return NativeLoader.isAvailable();
    }

    /**
     * Release native resources.
     * After calling this method, the stemmer cannot be used.
     */
    @Override
    public void close()
    {
        if (!closed)
        {
            closed = true;
            nDestroy(handle);
        }
    }

    private void ensureOpen()
    {
        if (closed)
        {
            throw new IllegalStateException("Stemmer has been closed");
        }
    }

    // Native methods
    private static native long nCreate(String language);
    private static native String nStem(long handle, String word);
    private static native String[] nStemBatch(long handle, String[] words);
    private static native void nDestroy(long handle);
}
