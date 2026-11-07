/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.tk;

/**
 * Exception thrown when attempting to create a stemmer for an unsupported language
 */
public class UnsupportedLanguageException extends RuntimeException
{
    private final String language;

    public UnsupportedLanguageException(String language)
    {
        super("Unsupported language: " + language);
        this.language = language;
    }

    public UnsupportedLanguageException(String language, Throwable cause)
    {
        super("Unsupported language: " + language, cause);
        this.language = language;
    }

    public String getLanguage()
    {
        return language;
    }
}
