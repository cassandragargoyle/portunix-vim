/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita;

/**
 * Result of lemmatization containing dictionary form and morphological tag.
 *
 * @param lemma Dictionary form of the word (cleaned from technical suffixes)
 * @param tag Morphological tag (e.g., "NNMS1-----A----" for Czech noun)
 * @param rawLemma Raw lemma as returned by MorphoDiTa (may contain technical suffixes)
 */
public record LemmaResult(
    String lemma,
    String tag,
    String rawLemma
) {
    /**
     * Create result with same value for lemma and rawLemma.
     *
     * @param lemma Dictionary form
     * @param tag Morphological tag
     */
    public LemmaResult(String lemma, String tag)
    {
        this(lemma, tag, lemma);
    }
}
