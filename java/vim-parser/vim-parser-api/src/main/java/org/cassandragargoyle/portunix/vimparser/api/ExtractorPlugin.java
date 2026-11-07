/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import org.pf4j.ExtensionPoint;

import java.util.Set;

/**
 * Interface for extractor plugins that process PDF documents
 * Plugins are discovered via PF4J plugin framework
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public interface ExtractorPlugin extends ExtensionPoint
{
	/**
	 * Get unique plugin identifier
	 * @return plugin ID (e.g., "barcode", "fulltext")
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
	 * Get IDs of plugins this plugin depends on
	 * Results from dependencies are available in PluginContext
	 * @return set of dependency plugin IDs
	 */
	Set<String> getDependencies();

	/**
	 * Get supported execution modes
	 * @return array of supported modes
	 */
	ExecutionMode[] getSupportedModes();

	/**
	 * Initialize the plugin
	 * Called once before first extraction
	 */
	default void initialize()
	{
		// Default no-op
	}

	/**
	 * Perform extraction
	 * @param context extraction context with PDF content and previous results
	 * @return extraction result
	 */
	PluginResult extract(PluginContext context);

	/**
	 * Shutdown the plugin
	 * Called when plugin is being unloaded
	 */
	default void shutdown()
	{
		// Default no-op
	}
}
