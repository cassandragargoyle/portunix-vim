/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.orchestration;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.PluginInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Execution plan showing resolved plugin order grouped by phases
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class ExecutionPlan
{
	private final List<PluginInfo> executionOrder;
	private final List<PluginInfo> excludedPlugins;
	private final String inputFile;
	private final long inputSizeBytes;
	private final int pageCount;

	public ExecutionPlan(List<PluginInfo> executionOrder, List<PluginInfo> excludedPlugins,
						 String inputFile, long inputSizeBytes, int pageCount)
	{
		this.executionOrder = executionOrder != null ? List.copyOf(executionOrder) : Collections.emptyList();
		this.excludedPlugins = excludedPlugins != null ? List.copyOf(excludedPlugins) : Collections.emptyList();
		this.inputFile = inputFile;
		this.inputSizeBytes = inputSizeBytes;
		this.pageCount = pageCount;
	}

	/**
	 * Get ordered list of plugins to execute
	 */
	public List<PluginInfo> getExecutionOrder()
	{
		return executionOrder;
	}

	/**
	 * Get plugins excluded from auto mode
	 */
	public List<PluginInfo> getExcludedPlugins()
	{
		return excludedPlugins;
	}

	/**
	 * Get plugins grouped by execution phase
	 */
	public Map<ExecutionPhase, List<PluginInfo>> getPhaseGroups()
	{
		Map<ExecutionPhase, List<PluginInfo>> groups = new LinkedHashMap<>();

		for (ExecutionPhase phase : ExecutionPhase.values())
		{
			List<PluginInfo> phasePlugins = executionOrder.stream()
				.filter(p -> p.getPhase() == phase)
				.collect(Collectors.toList());

			if (!phasePlugins.isEmpty())
			{
				groups.put(phase, phasePlugins);
			}
		}

		return groups;
	}

	/**
	 * Get total number of plugins to execute
	 */
	public int getPluginCount()
	{
		return executionOrder.size();
	}

	/**
	 * Get input file name
	 */
	public String getInputFile()
	{
		return inputFile;
	}

	/**
	 * Get input file size in bytes
	 */
	public long getInputSizeBytes()
	{
		return inputSizeBytes;
	}

	/**
	 * Get page count
	 */
	public int getPageCount()
	{
		return pageCount;
	}

	/**
	 * Format execution plan for display
	 */
	public String format()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("VIM Parser - Execution Plan\n");
		sb.append("============================\n");

		if (inputFile != null)
		{
			sb.append(String.format("Input: %s", inputFile));
			if (inputSizeBytes > 0)
			{
				sb.append(String.format(" (%s", formatSize(inputSizeBytes)));
				if (pageCount > 0)
				{
					sb.append(String.format(", %d page%s", pageCount, pageCount == 1 ? "" : "s"));
				}
				sb.append(")");
			}
			sb.append("\n");
		}

		sb.append("\n");
		sb.append(String.format("Discovered plugins: %d\n", executionOrder.size() + excludedPlugins.size()));
		sb.append(String.format("Auto-enabled: %d\n", executionOrder.size()));
		sb.append("\n");

		if (executionOrder.isEmpty())
		{
			sb.append("No plugins to execute.\n");
		}
		else
		{
			sb.append("Execution sequence:\n");

			Map<ExecutionPhase, List<PluginInfo>> groups = getPhaseGroups();
			int stepCounter = 1;

			for (Map.Entry<ExecutionPhase, List<PluginInfo>> entry : groups.entrySet())
			{
				ExecutionPhase phase = entry.getKey();
				List<PluginInfo> plugins = entry.getValue();

				sb.append(String.format("  Phase %d: %s\n", phase.getOrder() / 10, phase.name()));

				for (PluginInfo plugin : plugins)
				{
					String deps = plugin.getPredecessors().isEmpty()
						? "(no dependencies)"
						: "(requires: " + String.join(", ", plugin.getPredecessors()) + ")";

					sb.append(String.format("    [%d] %-12s %s\n", stepCounter++, plugin.getId(), deps));
				}

				sb.append("\n");
			}
		}

		if (!excludedPlugins.isEmpty())
		{
			sb.append("Excluded from auto mode:\n");
			for (PluginInfo plugin : excludedPlugins)
			{
				String reason = plugin.isAutoEnabled() ? "explicitly excluded" : "autoEnabled=false";
				sb.append(String.format("    [-] %-12s (%s)\n", plugin.getId(), reason));
			}
			sb.append("\n");
		}

		if (inputFile != null)
		{
			String outputFile = inputFile + ".vim.json";
			sb.append(String.format("Estimated output: %s\n", outputFile));
		}

		return sb.toString();
	}

	/**
	 * Format byte size for display
	 */
	private String formatSize(long bytes)
	{
		if (bytes < 1024)
		{
			return bytes + " B";
		}
		else if (bytes < 1024 * 1024)
		{
			return String.format("%.1f KB", bytes / 1024.0);
		}
		else
		{
			return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
		}
	}

	/**
	 * Builder for ExecutionPlan
	 */
	public static class Builder
	{
		private List<PluginInfo> executionOrder = new ArrayList<>();
		private List<PluginInfo> excludedPlugins = new ArrayList<>();
		private String inputFile;
		private long inputSizeBytes;
		private int pageCount;

		public Builder executionOrder(List<PluginInfo> order)
		{
			this.executionOrder = order;
			return this;
		}

		public Builder excludedPlugins(List<PluginInfo> excluded)
		{
			this.excludedPlugins = excluded;
			return this;
		}

		public Builder inputFile(String file)
		{
			this.inputFile = file;
			return this;
		}

		public Builder inputSizeBytes(long size)
		{
			this.inputSizeBytes = size;
			return this;
		}

		public Builder pageCount(int count)
		{
			this.pageCount = count;
			return this;
		}

		public ExecutionPlan build()
		{
			return new ExecutionPlan(executionOrder, excludedPlugins, inputFile, inputSizeBytes, pageCount);
		}
	}
}
