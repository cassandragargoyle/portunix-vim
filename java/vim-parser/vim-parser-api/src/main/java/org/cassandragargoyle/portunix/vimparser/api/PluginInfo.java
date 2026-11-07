/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Plugin metadata for orchestration and dependency resolution
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class PluginInfo
{
	private final String id;
	private final String name;
	private final String version;
	private final PluginType type;
	private final ExecutionPhase phase;
	private final Set<String> predecessors;
	private final boolean autoEnabled;
	private final int priority;

	/**
	 * Plugin type enumeration
	 */
	public enum PluginType
	{
		EXTRACTOR,
		PARSER,
		VISUALIZER
	}

	public PluginInfo(String id, String name, String version, PluginType type,
					  ExecutionPhase phase, Set<String> predecessors, boolean autoEnabled, int priority)
	{
		this.id = Objects.requireNonNull(id, "Plugin ID cannot be null");
		this.name = name;
		this.version = version;
		this.type = type;
		this.phase = phase != null ? phase : ExecutionPhase.EXTRACT;
		this.predecessors = predecessors != null ? Set.copyOf(predecessors) : Collections.emptySet();
		this.autoEnabled = autoEnabled;
		this.priority = priority;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getVersion()
	{
		return version;
	}

	public PluginType getType()
	{
		return type;
	}

	public ExecutionPhase getPhase()
	{
		return phase;
	}

	public Set<String> getPredecessors()
	{
		return predecessors;
	}

	public boolean isAutoEnabled()
	{
		return autoEnabled;
	}

	public int getPriority()
	{
		return priority;
	}

	/**
	 * Create PluginInfo from ExtractorPlugin
	 */
	public static PluginInfo fromExtractor(ExtractorPlugin plugin)
	{
		ExecutionPhase phase = ExecutionPhase.EXTRACT;
		Set<String> predecessors = plugin.getDependencies();
		boolean autoEnabled = true;

		if (plugin instanceof OrchestrationAware orchestration)
		{
			phase = orchestration.getPhase();
			predecessors = orchestration.getPredecessors();
			autoEnabled = orchestration.isAutoEnabled();
		}

		return new PluginInfo(
			plugin.getId(),
			plugin.getName(),
			plugin.getVersion(),
			PluginType.EXTRACTOR,
			phase,
			predecessors,
			autoEnabled,
			plugin.getPriority()
		);
	}

	/**
	 * Create PluginInfo from ParserPlugin
	 */
	public static PluginInfo fromParser(ParserPlugin plugin)
	{
		ExecutionPhase phase = ExecutionPhase.PARSE;
		Set<String> predecessors = plugin.getDependencies();
		boolean autoEnabled = true;

		if (plugin instanceof OrchestrationAware orchestration)
		{
			phase = orchestration.getPhase();
			predecessors = orchestration.getPredecessors();
			autoEnabled = orchestration.isAutoEnabled();
		}

		return new PluginInfo(
			plugin.getId(),
			plugin.getName(),
			plugin.getVersion(),
			PluginType.PARSER,
			phase,
			predecessors,
			autoEnabled,
			plugin.getPriority()
		);
	}

	/**
	 * Create PluginInfo from VisualizerPlugin
	 */
	public static PluginInfo fromVisualizer(VisualizerPlugin plugin)
	{
		ExecutionPhase phase = ExecutionPhase.VISUALIZE;
		Set<String> predecessors = Collections.emptySet();
		boolean autoEnabled = false;  // Visualizers not in auto mode by default

		if (plugin instanceof OrchestrationAware orchestration)
		{
			phase = orchestration.getPhase();
			predecessors = orchestration.getPredecessors();
			autoEnabled = orchestration.isAutoEnabled();
		}

		return new PluginInfo(
			plugin.getId(),
			plugin.getName(),
			plugin.getVersion(),
			PluginType.VISUALIZER,
			phase,
			predecessors,
			autoEnabled,
			0
		);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PluginInfo that = (PluginInfo) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}

	@Override
	public String toString()
	{
		return String.format("PluginInfo{id='%s', type=%s, phase=%s, auto=%s, predecessors=%s}",
			id, type, phase, autoEnabled, predecessors);
	}
}
