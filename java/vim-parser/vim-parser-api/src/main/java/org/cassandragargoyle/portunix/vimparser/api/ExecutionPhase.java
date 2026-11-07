/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

/**
 * Execution phases for plugin orchestration
 * Phases determine the order in which plugins are executed during auto mode
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public enum ExecutionPhase
{
	/**
	 * Raw data extraction from PDF (fulltext, barcode)
	 */
	EXTRACT(10),

	/**
	 * Semantic parsing of extracted data (spayd)
	 */
	PARSE(20),

	/**
	 * Data transformation and enrichment
	 */
	TRANSFORM(30),

	/**
	 * Output generation and visualization (html)
	 */
	VISUALIZE(40);

	private final int order;

	ExecutionPhase(int order)
	{
		this.order = order;
	}

	/**
	 * Get the execution order value
	 * Lower values execute first
	 * @return order value
	 */
	public int getOrder()
	{
		return order;
	}
}
