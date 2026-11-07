/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import java.util.Collections;
import java.util.Set;

/**
 * Interface for plugins that participate in automatic orchestration
 * Provides metadata for dependency resolution and execution ordering
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public interface OrchestrationAware
{
	/**
	 * Returns IDs of plugins that MUST run before this plugin
	 * The orchestrator ensures all predecessors complete successfully before running this plugin
	 *
	 * @return set of predecessor plugin IDs (empty = no dependencies)
	 */
	default Set<String> getPredecessors()
	{
		return Collections.emptySet();
	}

	/**
	 * Whether this plugin participates in auto-orchestration mode
	 * Plugins that return false must be invoked explicitly
	 *
	 * @return true if plugin should be included in 'auto' mode
	 */
	default boolean isAutoEnabled()
	{
		return true;
	}

	/**
	 * Execution phase for grouping plugins
	 * Plugins in earlier phases run before plugins in later phases
	 *
	 * @return execution phase (default: EXTRACT)
	 */
	default ExecutionPhase getPhase()
	{
		return ExecutionPhase.EXTRACT;
	}
}
