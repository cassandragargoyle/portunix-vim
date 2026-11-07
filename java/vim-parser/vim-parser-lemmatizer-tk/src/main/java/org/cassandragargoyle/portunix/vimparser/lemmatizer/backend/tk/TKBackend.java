/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.tk;

import org.cassandragargoyle.portunix.vimparser.lemmatizer.api.LemmatizerBackend;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.StemmedToken;
import org.cassandragargoyle.tk.TKStemmer;
import org.pf4j.Extension;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TK Stemmer backend for vim-parser-lemmatizer
 * Provides stemming for 33 languages using native C++ library via JNI
 *
 * @author Zdenek
 * @since 2026-02-01
 */
@Extension
public class TKBackend implements LemmatizerBackend
{
    public static final String BACKEND_ID = "tk";
    public static final String BACKEND_NAME = "TK Stemmer (33 languages)";

    // ISO 639-1 to TK language name mapping
    private static final Map<String, String> LANG_MAP = Map.ofEntries(
        Map.entry("ar", "arabic"),
        Map.entry("hy", "armenian"),
        Map.entry("eu", "basque"),
        Map.entry("ca", "catalan"),
        Map.entry("cs", "czech"),
        Map.entry("da", "danish"),
        Map.entry("nl", "dutch"),
        Map.entry("en", "english"),
        Map.entry("et", "estonian"),
        Map.entry("fi", "finnish"),
        Map.entry("fr", "french"),
        Map.entry("de", "german"),
        Map.entry("el", "greek"),
        Map.entry("hi", "hindi"),
        Map.entry("hu", "hungarian"),
        Map.entry("id", "indonesian"),
        Map.entry("ga", "irish"),
        Map.entry("it", "italian"),
        Map.entry("lt", "lithuanian"),
        Map.entry("ne", "nepali"),
        Map.entry("no", "norwegian"),
        Map.entry("pl", "polish"),
        Map.entry("pt", "portuguese"),
        Map.entry("ro", "romanian"),
        Map.entry("ru", "russian"),
        Map.entry("sr", "serbian"),
        Map.entry("es", "spanish"),
        Map.entry("sv", "swedish"),
        Map.entry("ta", "tamil"),
        Map.entry("tr", "turkish"),
        Map.entry("uk", "ukrainian"),
        Map.entry("yi", "yiddish")
    );

    // Pattern for tokenization (Unicode letters)
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\p{L}+");

    // Cached stemmer instances per language
    private final Map<String, TKStemmer> stemmers = new ConcurrentHashMap<>();

    @Override
    public String getBackendId()
    {
        return BACKEND_ID;
    }

    @Override
    public String getBackendName()
    {
        return BACKEND_NAME;
    }

    @Override
    public Set<String> getSupportedLanguages()
    {
        return LANG_MAP.keySet();
    }

    @Override
    public boolean isAvailable()
    {
        return TKStemmer.isAvailable();
    }

    @Override
    public StemmedResult stem(String text, String language)
    {
        long startTime = System.currentTimeMillis();
        StemmedResult result = new StemmedResult(language, BACKEND_ID);

        if (text == null || text.isEmpty())
        {
            result.calculateStatistics(System.currentTimeMillis() - startTime);
            return result;
        }

        String tkLang = LANG_MAP.getOrDefault(language, "english");
        TKStemmer stemmer = getStemmer(tkLang);

        if (stemmer == null)
        {
            result.calculateStatistics(System.currentTimeMillis() - startTime);
            return result;
        }

        // Tokenize and stem
        int tokenId = 1;
        Matcher matcher = TOKEN_PATTERN.matcher(text);

        while (matcher.find())
        {
            String word = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            String stem = stemmer.stem(word.toLowerCase());

            StemmedToken token = new StemmedToken(
                "t" + String.format("%03d", tokenId++),
                word,
                stem != null ? stem : word.toLowerCase(),
                start,
                end
            );
            result.addToken(token);
        }

        result.calculateStatistics(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Get or create stemmer for given TK language name
     */
    private TKStemmer getStemmer(String tkLang)
    {
        return stemmers.computeIfAbsent(tkLang, lang -> {
            try
            {
                return TKStemmer.create(lang);
            }
            catch (Exception e)
            {
                return null;
            }
        });
    }

    /**
     * Close all cached stemmers
     */
    public void close()
    {
        stemmers.values().forEach(TKStemmer::close);
        stemmers.clear();
    }
}
