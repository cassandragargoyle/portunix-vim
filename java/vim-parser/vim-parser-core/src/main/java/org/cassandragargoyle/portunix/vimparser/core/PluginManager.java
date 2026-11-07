/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.core;

import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.ParserPlugin;
import org.cassandragargoyle.portunix.vimparser.api.VisualizerPlugin;
import org.pf4j.CompoundPluginDescriptorFinder;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginWrapper;
import org.pf4j.PropertiesPluginDescriptorFinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Plugin discovery and lifecycle management using PF4J
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class PluginManager
{
	private static final Logger LOG = LogFactory.getLogger(PluginManager.class);

	/** Name of the plugins directory, next to the JAR or in the working directory */
	static final String PLUGINS_DIR_NAME = "plugins";

	private final Map<String, ExtractorPlugin> extractorPlugins = new HashMap<>();
	private final Map<String, ParserPlugin> parserPlugins = new HashMap<>();
	private final Map<String, VisualizerPlugin> visualizerPlugins = new HashMap<>();

	private org.pf4j.PluginManager pf4jManager;
	private boolean initialized = false;
	private boolean developmentMode = false;
	private Path pluginsDir;

	/**
	 * Create plugin manager with default plugins directory (see {@link #defaultPluginsDir(Path)})
	 */
	public PluginManager()
	{
		this(defaultPluginsDir(codeSourceOf(PluginManager.class)));
	}

	/**
	 * Create plugin manager with custom plugins directory
	 * @param pluginsDir path to plugins directory
	 */
	public PluginManager(Path pluginsDir)
	{
		this.pluginsDir = pluginsDir;
	}

	/**
	 * Discover and load all available plugins using PF4J
	 */
	public void discoverPlugins()
	{
		if (initialized)
		{
			return;
		}

		developmentMode = isDevelopmentMode();

		if (developmentMode)
		{
			LOG.log(Level.FINE, "Discovering plugins in DEVELOPMENT mode (classpath)");
			pf4jManager = new DefaultPluginManager()
			{
				@Override
				protected CompoundPluginDescriptorFinder createPluginDescriptorFinder()
				{
					return new CompoundPluginDescriptorFinder()
						.add(new PropertiesPluginDescriptorFinder())
						.add(new ManifestPluginDescriptorFinder());
				}

				@Override
				public boolean isDevelopment()
				{
					return true;
				}
			};
		}
		else
		{
			LOG.log(Level.FINE, "Discovering plugins from: {0}", pluginsDir.toAbsolutePath());
			pf4jManager = new DefaultPluginManager(pluginsDir);
		}

		// Load and start plugins
		pf4jManager.loadPlugins();
		pf4jManager.startPlugins();

		// Log loaded PF4J plugins
		List<PluginWrapper> loadedPlugins = pf4jManager.getStartedPlugins();
		for (PluginWrapper plugin : loadedPlugins)
		{
			LOG.log(Level.FINE, "Loaded PF4J plugin: {0} v{1}",
					LogFactory.args(plugin.getPluginId(), plugin.getDescriptor().getVersion()));
		}

		// Discover extensions
		discoverExtractorPlugins();
		discoverParserPlugins();
		discoverVisualizerPlugins();

		initialized = true;

		LOG.log(Level.FINE, "Discovered {0} extractor(s), {1} parser(s), {2} visualizer(s)",
				LogFactory.args(extractorPlugins.size(), parserPlugins.size(), visualizerPlugins.size()));
	}

	/**
	 * Resolve the default plugins directory. An installed JAR has its plugins next to it, so
	 * the result does not depend on the working directory; without such a directory the
	 * relative {@code plugins} directory is used, as before
	 * @param codeSource location of the running code (a JAR file or a classes directory), or null
	 * @return plugins directory
	 */
	static Path defaultPluginsDir(Path codeSource)
	{
		if (codeSource != null && Files.isRegularFile(codeSource))
		{
			Path besideJar = codeSource.toAbsolutePath().getParent().resolve(PLUGINS_DIR_NAME);
			if (Files.isDirectory(besideJar))
			{
				return besideJar;
			}
		}
		return Paths.get(PLUGINS_DIR_NAME);
	}

	/**
	 * Location of the code of a class (a JAR file or a classes directory)
	 * @param type class to locate
	 * @return path of the code source, or null when it is not known
	 */
	private static Path codeSourceOf(Class<?> type)
	{
		try
		{
			return Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (Exception e)
		{
			LOG.log(Level.FINE, "Code source of {0} is not known: {1}",
					LogFactory.args(type.getName(), e.toString()));
			return null;
		}
	}

	/**
	 * Get the plugins directory
	 * @return plugins directory (relative when it is resolved against the working directory)
	 */
	public Path getPluginsDir()
	{
		return pluginsDir;
	}

	/**
	 * Check whether plugins were loaded from the classpath because the plugins directory is
	 * missing or empty
	 * @return true in development mode
	 */
	public boolean isDevelopment()
	{
		return developmentMode;
	}

	/**
	 * Describe where plugins were looked for, for messages to the user
	 * @return plugins directory, or the classpath in development mode
	 */
	public String describePluginSource()
	{
		Path dir = pluginsDir.toAbsolutePath().normalize();
		if (developmentMode)
		{
			return "classpath (development mode; no plugins in " + dir + ")";
		}
		return dir.toString();
	}

	/**
	 * Check whether any extractor or parser plugin was found
	 * @return true when extraction can produce content
	 */
	public boolean hasExtractionPlugins()
	{
		return !extractorPlugins.isEmpty() || !parserPlugins.isEmpty();
	}

	/**
	 * Check if running in development mode (no plugins directory or empty)
	 */
	private boolean isDevelopmentMode()
	{
		if (!Files.exists(pluginsDir))
		{
			return true;
		}

		try
		{
			return Files.list(pluginsDir).findFirst().isEmpty();
		}
		catch (Exception e)
		{
			return true;
		}
	}

	/**
	 * Discover extractor plugins
	 */
	private void discoverExtractorPlugins()
	{
		List<ExtractorPlugin> extractors = pf4jManager.getExtensions(ExtractorPlugin.class);

		for (ExtractorPlugin plugin : extractors)
		{
			String id = plugin.getId();
			if (extractorPlugins.containsKey(id))
			{
				LOG.log(Level.WARNING, "Duplicate extractor plugin ID: {0}", id);
				continue;
			}

			try
			{
				plugin.initialize();
				extractorPlugins.put(id, plugin);
				LOG.log(Level.FINE, "Loaded extractor plugin: {0} v{1}",
						LogFactory.args(plugin.getName(), plugin.getVersion()));
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, "Failed to initialize extractor plugin {0}: {1}",
						LogFactory.args(id, e.getMessage()));
			}
		}
	}

	/**
	 * Discover parser plugins
	 */
	private void discoverParserPlugins()
	{
		List<ParserPlugin> parsers = pf4jManager.getExtensions(ParserPlugin.class);

		for (ParserPlugin plugin : parsers)
		{
			String id = plugin.getId();
			if (parserPlugins.containsKey(id))
			{
				LOG.log(Level.WARNING, "Duplicate parser plugin ID: {0}", id);
				continue;
			}

			parserPlugins.put(id, plugin);
			LOG.log(Level.FINE, "Loaded parser plugin: {0} v{1}",
					LogFactory.args(plugin.getName(), plugin.getVersion()));
		}
	}

	/**
	 * Discover visualizer plugins
	 */
	private void discoverVisualizerPlugins()
	{
		List<VisualizerPlugin> visualizers = pf4jManager.getExtensions(VisualizerPlugin.class);

		for (VisualizerPlugin plugin : visualizers)
		{
			String id = plugin.getId();
			if (visualizerPlugins.containsKey(id))
			{
				LOG.log(Level.WARNING, "Duplicate visualizer plugin ID: {0}", id);
				continue;
			}

			visualizerPlugins.put(id, plugin);
			LOG.log(Level.FINE, "Loaded visualizer plugin: {0} v{1}",
					LogFactory.args(plugin.getName(), plugin.getVersion()));
		}
	}

	/**
	 * Get extractor plugin by ID
	 */
	public ExtractorPlugin getExtractorPlugin(String id)
	{
		return extractorPlugins.get(id);
	}

	/**
	 * Get parser plugin by ID
	 */
	public ParserPlugin getParserPlugin(String id)
	{
		return parserPlugins.get(id);
	}

	/**
	 * Get visualizer plugin by ID
	 */
	public VisualizerPlugin getVisualizerPlugin(String id)
	{
		return visualizerPlugins.get(id);
	}

	/**
	 * Get all extractor plugins sorted by priority
	 */
	public List<ExtractorPlugin> getExtractorPlugins()
	{
		return extractorPlugins.values().stream()
			.sorted(Comparator.comparingInt(ExtractorPlugin::getPriority))
			.collect(Collectors.toList());
	}

	/**
	 * Get all parser plugins sorted by priority
	 */
	public List<ParserPlugin> getParserPlugins()
	{
		return parserPlugins.values().stream()
			.sorted(Comparator.comparingInt(ParserPlugin::getPriority))
			.collect(Collectors.toList());
	}

	/**
	 * Get all visualizer plugins
	 */
	public List<VisualizerPlugin> getVisualizerPlugins()
	{
		return new ArrayList<>(visualizerPlugins.values());
	}

	/**
	 * Get extractor plugins filtered by IDs
	 */
	public List<ExtractorPlugin> getExtractorPlugins(Set<String> pluginIds)
	{
		if (pluginIds == null || pluginIds.isEmpty())
		{
			return getExtractorPlugins();
		}

		return extractorPlugins.values().stream()
			.filter(p -> pluginIds.contains(p.getId()))
			.sorted(Comparator.comparingInt(ExtractorPlugin::getPriority))
			.collect(Collectors.toList());
	}

	/**
	 * Get parser plugins filtered by IDs
	 */
	public List<ParserPlugin> getParserPlugins(Set<String> pluginIds)
	{
		if (pluginIds == null || pluginIds.isEmpty())
		{
			return getParserPlugins();
		}

		return parserPlugins.values().stream()
			.filter(p -> pluginIds.contains(p.getId()))
			.sorted(Comparator.comparingInt(ParserPlugin::getPriority))
			.collect(Collectors.toList());
	}

	/**
	 * Check if an extractor plugin exists
	 */
	public boolean hasExtractorPlugin(String id)
	{
		return extractorPlugins.containsKey(id);
	}

	/**
	 * Check if a parser plugin exists
	 */
	public boolean hasParserPlugin(String id)
	{
		return parserPlugins.containsKey(id);
	}

	/**
	 * Check if a visualizer plugin exists
	 */
	public boolean hasVisualizerPlugin(String id)
	{
		return visualizerPlugins.containsKey(id);
	}

	/**
	 * Get list of all plugin IDs
	 */
	public List<String> getAllPluginIds()
	{
		List<String> ids = new ArrayList<>();
		ids.addAll(extractorPlugins.keySet());
		ids.addAll(parserPlugins.keySet());
		ids.addAll(visualizerPlugins.keySet());
		return ids;
	}

	/**
	 * Get PF4J plugin information for display
	 * @return list of plugin info maps with id, version, state, description
	 */
	public List<Map<String, String>> getPluginInfoList()
	{
		List<Map<String, String>> infoList = new ArrayList<>();

		if (pf4jManager != null)
		{
			for (PluginWrapper wrapper : pf4jManager.getPlugins())
			{
				Map<String, String> info = new HashMap<>();
				info.put("id", wrapper.getPluginId());
				info.put("version", wrapper.getDescriptor().getVersion());
				info.put("state", wrapper.getPluginState().toString());
				info.put("description", wrapper.getDescriptor().getPluginDescription());
				info.put("provider", wrapper.getDescriptor().getProvider());
				infoList.add(info);
			}
		}

		return infoList;
	}

	/**
	 * Shutdown all plugins
	 */
	public void shutdown()
	{
		LOG.log(Level.FINE, "Shutting down plugins...");

		// Shutdown extractor plugins
		for (ExtractorPlugin plugin : extractorPlugins.values())
		{
			try
			{
				plugin.shutdown();
			}
			catch (Exception e)
			{
				LOG.log(Level.WARNING, "Error shutting down plugin {0}: {1}",
						LogFactory.args(plugin.getId(), e.getMessage()));
			}
		}

		// Stop PF4J plugins - never let shutdown throw (PF4J may raise
		// ConcurrentModificationException while iterating its started-plugins list)
		if (pf4jManager != null)
		{
			try
			{
				pf4jManager.stopPlugins();
			}
			catch (Exception e)
			{
				LOG.log(Level.WARNING, "Error stopping PF4J plugins: {0}",
						LogFactory.args(e.toString()));
			}
		}

		extractorPlugins.clear();
		parserPlugins.clear();
		visualizerPlugins.clear();
		initialized = false;
	}

	/**
	 * Get the PF4J plugin manager instance
	 * @return PF4J plugin manager
	 */
	public org.pf4j.PluginManager getPf4jManager()
	{
		return pf4jManager;
	}
}
