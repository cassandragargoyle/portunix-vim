/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita;

/**
 * Token from text processing with lemma and position information.
 *
 * @param form Original word form as it appears in text
 * @param lemma Dictionary form of the word
 * @param tag Morphological tag
 * @param start Start offset in original text (0-based)
 * @param end End offset in original text (exclusive)
 */
public record TaggedToken(
    String form,
    String lemma,
    String tag,
    int start,
    int end
) {
    /**
     * Create token without position information.
     *
     * @param form Original word form
     * @param lemma Dictionary form
     * @param tag Morphological tag
     */
    public TaggedToken(String form, String lemma, String tag)
    {
        this(form, lemma, tag, -1, -1);
    }
}
