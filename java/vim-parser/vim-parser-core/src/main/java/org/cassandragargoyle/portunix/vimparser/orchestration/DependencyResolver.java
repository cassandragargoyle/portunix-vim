/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.orchestration;

import org.cassandragargoyle.portunix.vimparser.api.PluginInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DAG-based dependency resolver using Kahn's algorithm for topological sort
 * Resolves plugin execution order based on dependencies and phases
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class DependencyResolver
{
	/**
	 * Build execution plan from plugins using topological sort
	 *
	 * @param plugins collection of plugin metadata
	 * @return ordered list of plugins for execution
	 * @throws CyclicDependencyException if circular dependency detected
	 * @throws UnresolvedDependencyException if dependency cannot be satisfied
	 */
	public List<PluginInfo> resolve(Collection<PluginInfo> plugins)
		throws CyclicDependencyException, UnresolvedDependencyException
	{
		if (plugins == null || plugins.isEmpty())
		{
			return new ArrayList<>();
		}

		// Build plugin ID map
		Map<String, PluginInfo> pluginMap = new HashMap<>();
		for (PluginInfo plugin : plugins)
		{
			pluginMap.put(plugin.getId(), plugin);
		}

		// Validate dependencies
		ValidationResult validation = validate(plugins);
		if (!validation.isValid())
		{
			if (!validation.getMissingDependencies().isEmpty())
			{
				Map.Entry<String, Set<String>> first = validation.getMissingDependencies()
					.entrySet().iterator().next();
				throw new UnresolvedDependencyException(
					first.getKey(),
					first.getValue().iterator().next()
				);
			}
			if (!validation.getCyclicDependencies().isEmpty())
			{
				throw new CyclicDependencyException(validation.getCyclicDependencies());
			}
		}

		// Kahn's algorithm for topological sort
		// Calculate in-degree for each plugin
		Map<String, Integer> inDegree = new HashMap<>();
		Map<String, Set<String>> dependents = new HashMap<>();

		for (PluginInfo plugin : plugins)
		{
			inDegree.put(plugin.getId(), 0);
			dependents.put(plugin.getId(), new HashSet<>());
		}

		for (PluginInfo plugin : plugins)
		{
			for (String predecessor : plugin.getPredecessors())
			{
				if (pluginMap.containsKey(predecessor))
				{
					inDegree.merge(plugin.getId(), 1, Integer::sum);
					dependents.get(predecessor).add(plugin.getId());
				}
			}
		}

		// Start with plugins that have no dependencies
		Queue<PluginInfo> queue = new LinkedList<>();
		for (PluginInfo plugin : plugins)
		{
			if (inDegree.get(plugin.getId()) == 0)
			{
				queue.add(plugin);
			}
		}

		// Sort queue by phase and priority for deterministic ordering
		List<PluginInfo> sortedQueue = new ArrayList<>(queue);
		sortedQueue.sort(Comparator
			.comparingInt((PluginInfo p) -> p.getPhase().getOrder())
			.thenComparingInt(PluginInfo::getPriority));
		queue.clear();
		queue.addAll(sortedQueue);

		List<PluginInfo> result = new ArrayList<>();
		Set<String> processed = new HashSet<>();

		while (!queue.isEmpty())
		{
			// Extract all plugins at current level
			List<PluginInfo> currentLevel = new ArrayList<>(queue);
			queue.clear();

			// Sort by phase, then by priority for deterministic ordering
			currentLevel.sort(Comparator
				.comparingInt((PluginInfo p) -> p.getPhase().getOrder())
				.thenComparingInt(PluginInfo::getPriority));

			for (PluginInfo plugin : currentLevel)
			{
				result.add(plugin);
				processed.add(plugin.getId());

				// Decrease in-degree for dependents
				for (String dependentId : dependents.get(plugin.getId()))
				{
					int newDegree = inDegree.get(dependentId) - 1;
					inDegree.put(dependentId, newDegree);

					if (newDegree == 0)
					{
						queue.add(pluginMap.get(dependentId));
					}
				}
			}
		}

		// Check for cycles (if not all plugins were processed)
		if (result.size() != plugins.size())
		{
			Set<String> remaining = plugins.stream()
				.map(PluginInfo::getId)
				.filter(id -> !processed.contains(id))
				.collect(Collectors.toSet());
			throw new CyclicDependencyException(remaining);
		}

		return result;
	}

	/**
	 * Validate all dependencies can be satisfied
	 *
	 * @param plugins collection of plugin metadata
	 * @return validation result with any issues found
	 */
	public ValidationResult validate(Collection<PluginInfo> plugins)
	{
		ValidationResult result = new ValidationResult();

		if (plugins == null || plugins.isEmpty())
		{
			return result;
		}

		// Build plugin ID set
		Set<String> availableIds = plugins.stream()
			.map(PluginInfo::getId)
			.collect(Collectors.toSet());

		// Check for missing dependencies
		for (PluginInfo plugin : plugins)
		{
			for (String predecessor : plugin.getPredecessors())
			{
				if (!availableIds.contains(predecessor))
				{
					result.addMissingDependency(plugin.getId(), predecessor);
				}
			}
		}

		// Check for cycles using DFS
		Set<String> visited = new HashSet<>();
		Set<String> recursionStack = new HashSet<>();

		for (PluginInfo plugin : plugins)
		{
			if (hasCycle(plugin, plugins, visited, recursionStack, result))
			{
				break;
			}
		}

		return result;
	}

	/**
	 * Detect cycles using DFS
	 */
	private boolean hasCycle(PluginInfo plugin, Collection<PluginInfo> allPlugins,
							 Set<String> visited, Set<String> recursionStack,
							 ValidationResult result)
	{
		String id = plugin.getId();

		if (recursionStack.contains(id))
		{
			result.addCyclicDependency(id);
			return true;
		}

		if (visited.contains(id))
		{
			return false;
		}

		visited.add(id);
		recursionStack.add(id);

		// Build map for lookup
		Map<String, PluginInfo> pluginMap = allPlugins.stream()
			.collect(Collectors.toMap(PluginInfo::getId, p -> p));

		for (String predecessor : plugin.getPredecessors())
		{
			PluginInfo predPlugin = pluginMap.get(predecessor);
			if (predPlugin != null && hasCycle(predPlugin, allPlugins, visited, recursionStack, result))
			{
				return true;
			}
		}

		recursionStack.remove(id);
		return false;
	}

	/**
	 * Exception thrown when cyclic dependency is detected
	 */
	public static class CyclicDependencyException extends RuntimeException
	{
		private final Set<String> involvedPlugins;

		public CyclicDependencyException(Set<String> involvedPlugins)
		{
			super("Cyclic dependency detected involving plugins: " + involvedPlugins);
			this.involvedPlugins = Set.copyOf(involvedPlugins);
		}

		public Set<String> getInvolvedPlugins()
		{
			return involvedPlugins;
		}
	}

	/**
	 * Exception thrown when dependency cannot be resolved
	 */
	public static class UnresolvedDependencyException extends RuntimeException
	{
		private final String pluginId;
		private final String missingDependency;

		public UnresolvedDependencyException(String pluginId, String missingDependency)
		{
			super(String.format("Plugin '%s' depends on missing plugin '%s'", pluginId, missingDependency));
			this.pluginId = pluginId;
			this.missingDependency = missingDependency;
		}

		public String getPluginId()
		{
			return pluginId;
		}

		public String getMissingDependency()
		{
			return missingDependency;
		}
	}

	/**
	 * Validation result containing any dependency issues
	 */
	public static class ValidationResult
	{
		private final Map<String, Set<String>> missingDependencies = new HashMap<>();
		private final Set<String> cyclicDependencies = new HashSet<>();

		public boolean isValid()
		{
			return missingDependencies.isEmpty() && cyclicDependencies.isEmpty();
		}

		public Map<String, Set<String>> getMissingDependencies()
		{
			return missingDependencies;
		}

		public Set<String> getCyclicDependencies()
		{
			return cyclicDependencies;
		}

		void addMissingDependency(String pluginId, String missingId)
		{
			missingDependencies.computeIfAbsent(pluginId, k -> new HashSet<>()).add(missingId);
		}

		void addCyclicDependency(String pluginId)
		{
			cyclicDependencies.add(pluginId);
		}
	}
}
