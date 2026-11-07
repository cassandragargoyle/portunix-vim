/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.orchestration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for automatic orchestration mode
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class AutoConfig
{
	private boolean failFast = true;
	private boolean skipDisabled = true;
	private Set<String> excludePlugins = new HashSet<>();
	private boolean verbose = false;
	private boolean dryRun = false;

	// Metadata/fulltext options
	private boolean includeMetadata = true;
	private boolean includeFulltext = true;
	private boolean includeHash = false;

	// Text reconstruction options
	private boolean dropCapEnabled = true;

	/**
	 * Create default configuration
	 */
	public AutoConfig()
	{
	}

	/**
	 * Whether to stop on first plugin error
	 * @return true if execution should abort on error
	 */
	public boolean isFailFast()
	{
		return failFast;
	}

	/**
	 * Set fail-fast mode
	 * @param failFast true to abort on first error
	 * @return this for chaining
	 */
	public AutoConfig setFailFast(boolean failFast)
	{
		this.failFast = failFast;
		return this;
	}

	/**
	 * Whether to skip plugins that are not auto-enabled
	 * @return true if non-auto plugins should be skipped
	 */
	public boolean isSkipDisabled()
	{
		return skipDisabled;
	}

	/**
	 * Set skip disabled mode
	 * @param skipDisabled true to skip non-auto plugins
	 * @return this for chaining
	 */
	public AutoConfig setSkipDisabled(boolean skipDisabled)
	{
		this.skipDisabled = skipDisabled;
		return this;
	}

	/**
	 * Get explicitly excluded plugin IDs
	 * @return set of excluded plugin IDs
	 */
	public Set<String> getExcludePlugins()
	{
		return Collections.unmodifiableSet(excludePlugins);
	}

	/**
	 * Set excluded plugins
	 * @param excludePlugins set of plugin IDs to exclude
	 * @return this for chaining
	 */
	public AutoConfig setExcludePlugins(Set<String> excludePlugins)
	{
		this.excludePlugins = excludePlugins != null ? new HashSet<>(excludePlugins) : new HashSet<>();
		return this;
	}

	/**
	 * Add plugin to exclusion list
	 * @param pluginId plugin ID to exclude
	 * @return this for chaining
	 */
	public AutoConfig exclude(String pluginId)
	{
		this.excludePlugins.add(pluginId);
		return this;
	}

	/**
	 * Check if plugin should be excluded
	 * @param pluginId plugin ID to check
	 * @return true if plugin is excluded
	 */
	public boolean isExcluded(String pluginId)
	{
		return excludePlugins.contains(pluginId);
	}

	/**
	 * Whether verbose output is enabled
	 * @return true if verbose
	 */
	public boolean isVerbose()
	{
		return verbose;
	}

	/**
	 * Set verbose mode
	 * @param verbose true for verbose output
	 * @return this for chaining
	 */
	public AutoConfig setVerbose(boolean verbose)
	{
		this.verbose = verbose;
		return this;
	}

	/**
	 * Whether dry-run mode is enabled (plan only, no execution)
	 * @return true if dry-run
	 */
	public boolean isDryRun()
	{
		return dryRun;
	}

	/**
	 * Set dry-run mode
	 * @param dryRun true to show plan without executing
	 * @return this for chaining
	 */
	public AutoConfig setDryRun(boolean dryRun)
	{
		this.dryRun = dryRun;
		return this;
	}

	/**
	 * Whether to include document metadata in output
	 * @return true if metadata should be included
	 */
	public boolean isIncludeMetadata()
	{
		return includeMetadata;
	}

	/**
	 * Set include metadata option
	 * @param includeMetadata true to include metadata
	 * @return this for chaining
	 */
	public AutoConfig setIncludeMetadata(boolean includeMetadata)
	{
		this.includeMetadata = includeMetadata;
		return this;
	}

	/**
	 * Whether to include raw fulltext content in output
	 * @return true if fulltext should be included
	 */
	public boolean isIncludeFulltext()
	{
		return includeFulltext;
	}

	/**
	 * Set include fulltext option
	 * @param includeFulltext true to include fulltext
	 * @return this for chaining
	 */
	public AutoConfig setIncludeFulltext(boolean includeFulltext)
	{
		this.includeFulltext = includeFulltext;
		return this;
	}

	/**
	 * Whether to include SHA-256 hash in metadata
	 * @return true if hash should be included
	 */
	public boolean isIncludeHash()
	{
		return includeHash;
	}

	/**
	 * Set include hash option
	 * @param includeHash true to include hash
	 * @return this for chaining
	 */
	public AutoConfig setIncludeHash(boolean includeHash)
	{
		this.includeHash = includeHash;
		return this;
	}

	/**
	 * Whether drop cap detection and merging is enabled
	 * @return true if drop cap processing is enabled
	 */
	public boolean isDropCapEnabled()
	{
		return dropCapEnabled;
	}

	/**
	 * Set drop cap processing option
	 * @param dropCapEnabled true to enable drop cap detection and merging
	 * @return this for chaining
	 */
	public AutoConfig setDropCapEnabled(boolean dropCapEnabled)
	{
		this.dropCapEnabled = dropCapEnabled;
		return this;
	}

	/**
	 * Create default configuration
	 * @return new AutoConfig with defaults
	 */
	public static AutoConfig defaults()
	{
		return new AutoConfig();
	}
}
