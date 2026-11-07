/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer.api;

import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.pf4j.ExtensionPoint;

import java.util.Set;

/**
 * Interface for lemmatizer/stemmer backends
 * Implementations provide language-specific stemming or lemmatization
 *
 * This is a PF4J ExtensionPoint - backends can be loaded as plugins
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public interface LemmatizerBackend extends ExtensionPoint
{
	/**
	 * Get unique backend identifier
	 * @return backend ID (e.g., "lucene", "morphodita")
	 */
	String getBackendId();

	/**
	 * Get human-readable backend name
	 * @return backend name for display
	 */
	String getBackendName();

	/**
	 * Get set of supported language codes
	 * @return set of ISO 639-1 language codes (e.g., "cs", "sk", "en")
	 */
	Set<String> getSupportedLanguages();

	/**
	 * Check if backend supports given language
	 * @param language ISO 639-1 language code
	 * @return true if language is supported
	 */
	default boolean supportsLanguage(String language)
	{
		return getSupportedLanguages().contains(language);
	}

	/**
	 * Check if backend is available and ready to use
	 * @return true if backend can be used
	 */
	boolean isAvailable();

	/**
	 * Stem or lemmatize text
	 * @param text input text to process
	 * @param language ISO 639-1 language code
	 * @return stemming result with tokens
	 */
	StemmedResult stem(String text, String language);
}
