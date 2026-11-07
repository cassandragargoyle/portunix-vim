/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import org.pf4j.ExtensionPoint;

import java.util.Set;

/**
 * Interface for parser plugins that interpret extracted data
 * Parsers process output from extractors (e.g., SPAYD parser processes barcode content)
 * Plugins are discovered via PF4J plugin framework
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public interface ParserPlugin extends ExtensionPoint
{
	/**
	 * Get unique plugin identifier
	 * @return plugin ID (e.g., "spayd")
	 */
	String getId();

	/**
	 * Get human-readable plugin name
	 * @return plugin name
	 */
	String getName();

	/**
	 * Get plugin version
	 * @return version string
	 */
	String getVersion();

	/**
	 * Get execution priority (lower = earlier execution)
	 * @return priority value
	 */
	int getPriority();

	/**
	 * Get IDs of extractor plugins this parser depends on
	 * @return set of dependency plugin IDs
	 */
	Set<String> getDependencies();

	/**
	 * Get supported execution modes
	 * @return array of supported modes
	 */
	ExecutionMode[] getSupportedModes();

	/**
	 * Check if this parser can handle the given content
	 * @param content content to check
	 * @return true if parser can process this content
	 */
	boolean canParse(String content);

	/**
	 * Parse extracted content
	 * @param context context with previous extraction results
	 * @return parsing result
	 */
	PluginResult parse(PluginContext context);
}
