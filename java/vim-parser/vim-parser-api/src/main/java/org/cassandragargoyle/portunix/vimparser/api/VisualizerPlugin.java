/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.pf4j.ExtensionPoint;

/**
 * Interface for visualizer plugins that generate output from VIM documents
 * Plugins are discovered via PF4J plugin framework
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public interface VisualizerPlugin extends ExtensionPoint
{
	/**
	 * Get unique plugin identifier
	 * @return plugin ID (e.g., "html")
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
	 * Get output format name
	 * @return format name (e.g., "HTML", "PDF")
	 */
	String getOutputFormat();

	/**
	 * Get output file extension
	 * @return file extension (e.g., ".html", ".pdf")
	 */
	String getFileExtension();

	/**
	 * Generate visualization from VIM document
	 * @param document VIM document to visualize
	 * @return generated content as string
	 */
	String visualize(VimDocument document);
}
