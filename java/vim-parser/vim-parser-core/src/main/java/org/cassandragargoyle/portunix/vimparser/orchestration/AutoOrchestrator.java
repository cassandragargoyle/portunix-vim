/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.orchestration;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.Main;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.ParserPlugin;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginInfo;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.core.PluginManager;
import org.cassandragargoyle.portunix.vimparser.engine.ExecutionEngine;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Automatic orchestrator for plugin execution
 * Discovers plugins, resolves dependencies, and executes in correct order
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class AutoOrchestrator
{
	private static final Logger LOG = LogFactory.getLogger(AutoOrchestrator.class);

	private final PluginManager pluginManager;
	private final ExecutionEngine executionEngine;
	private final DependencyResolver dependencyResolver;

	public AutoOrchestrator(PluginManager pluginManager, ExecutionEngine executionEngine)
	{
		this.pluginManager = pluginManager;
		this.executionEngine = executionEngine;
		this.dependencyResolver = new DependencyResolver();
	}

	/**
	 * Build execution plan for auto mode
	 *
	 * @param pdfContent PDF file content
	 * @param inputFileName input file name for display
	 * @param config orchestration configuration
	 * @return execution plan
	 */
	public ExecutionPlan buildPlan(byte[] pdfContent, String inputFileName, AutoConfig config)
	{
		// Collect all plugin metadata
		List<PluginInfo> allPlugins = new ArrayList<>();
		List<PluginInfo> excludedPlugins = new ArrayList<>();

		// Add extractor plugins
		for (ExtractorPlugin plugin : pluginManager.getExtractorPlugins())
		{
			PluginInfo info = PluginInfo.fromExtractor(plugin);
			if (shouldInclude(info, config))
			{
				allPlugins.add(info);
			}
			else
			{
				excludedPlugins.add(info);
			}
		}

		// Add parser plugins
		for (ParserPlugin plugin : pluginManager.getParserPlugins())
		{
			PluginInfo info = PluginInfo.fromParser(plugin);
			if (shouldInclude(info, config))
			{
				allPlugins.add(info);
			}
			else
			{
				excludedPlugins.add(info);
			}
		}

		// Resolve dependencies
		List<PluginInfo> executionOrder;
		try
		{
			executionOrder = dependencyResolver.resolve(allPlugins);
		}
		catch (DependencyResolver.CyclicDependencyException e)
		{
			LOG.log(Level.SEVERE, "Cyclic dependency detected: {0}", e.getInvolvedPlugins());
			throw e;
		}
		catch (DependencyResolver.UnresolvedDependencyException e)
		{
			LOG.log(Level.SEVERE, "Unresolved dependency: {0}", e.getMessage());
			throw e;
		}

		// Get PDF info
		int pageCount = 0;
		if (pdfContent != null && pdfContent.length > 0)
		{
			try (PDDocument doc = Loader.loadPDF(pdfContent))
			{
				pageCount = doc.getNumberOfPages();
			}
			catch (IOException e)
			{
				LOG.log(Level.WARNING, "Failed to read PDF page count: {0}", e.getMessage());
			}
		}

		return new ExecutionPlan.Builder()
			.executionOrder(executionOrder)
			.excludedPlugins(excludedPlugins)
			.inputFile(inputFileName)
			.inputSizeBytes(pdfContent != null ? pdfContent.length : 0)
			.pageCount(pageCount)
			.build();
	}

	/**
	 * Execute all auto-enabled plugins in resolved order
	 *
	 * @param pdfContent PDF file content
	 * @param inputFileName input file name
	 * @param config orchestration configuration
	 * @return VIM document with all extracted data
	 */
	public VimDocument execute(byte[] pdfContent, String inputFileName, AutoConfig config)
	{
		Tracer tracer = Main.getTelemetryProvider().getTracer("vim-parser-orchestrator");

		Span autoSpan = tracer.spanBuilder("auto-orchestrate")
			.setAttribute("vim.command", "auto")
			.setAttribute("document.path", inputFileName)
			.setAttribute("document.size", (long) pdfContent.length)
			.startSpan();

		try (var autoScope = autoSpan.makeCurrent())
		{
			ExecutionPlan plan = buildPlan(pdfContent, inputFileName, config);

			if (config.isDryRun())
			{
				VimDocument doc = new VimDocument();
				doc.getDocument().setSource(inputFileName);
				return doc;
			}

			// Execute plugins according to plan
			PluginContext context = new PluginContext();
			context.setPdfContent(pdfContent);
			context.setVerbose(config.isVerbose());
			context.setTracer(tracer);

			// Set metadata/fulltext options
			context.setOption("includeMetadata", config.isIncludeMetadata());
			context.setOption("includeFulltext", config.isIncludeFulltext());
			context.setOption("includeHash", config.isIncludeHash());
			context.setOption("dropCapEnabled", config.isDropCapEnabled());

			VimDocument vimDoc = new VimDocument();
			vimDoc.getDocument().setSource(inputFileName);

			autoSpan.setAttribute("vim.plugin.count", (long) plan.getExecutionOrder().size());

			List<String> failedPlugins = new ArrayList<>();

			for (PluginInfo pluginInfo : plan.getExecutionOrder())
			{
				// Check if dependencies succeeded
				if (!checkDependencies(pluginInfo, context, failedPlugins))
				{
					if (config.isVerbose())
					{
						LOG.log(Level.INFO, "Skipping {0}: dependency not satisfied", pluginInfo.getId());
					}
					failedPlugins.add(pluginInfo.getId());
					continue;
				}

				if (config.isVerbose())
				{
					LOG.log(Level.INFO, "Executing: {0} ({1})", LogFactory.args(pluginInfo.getId(), pluginInfo.getPhase()));
				}

				Span pluginSpan = tracer.spanBuilder("plugin:" + pluginInfo.getId())
					.setAttribute("vim.plugin.name", pluginInfo.getId())
					.startSpan();

				try (var pluginScope = pluginSpan.makeCurrent())
				{
					PluginResult result = executePlugin(pluginInfo, context);

					if (result.isSuccess())
					{
						context.addPreviousResult(pluginInfo.getId(), result);
						if (config.isVerbose())
						{
							LOG.log(Level.INFO, "  Completed in {0}ms", result.getProcessingTimeMs());
						}
					}
					else
					{
						pluginSpan.setStatus(StatusCode.ERROR, result.getErrorMessage());
						LOG.log(Level.WARNING, "Plugin {0} failed: {1}",
							LogFactory.args(pluginInfo.getId(), result.getErrorMessage()));
						failedPlugins.add(pluginInfo.getId());

						if (config.isFailFast())
						{
							throw new RuntimeException("Plugin execution failed: " + pluginInfo.getId() +
								" - " + result.getErrorMessage());
						}
					}
				}
				catch (Exception e)
				{
					pluginSpan.setStatus(StatusCode.ERROR, e.getMessage());
					pluginSpan.recordException(e);
					LOG.log(Level.SEVERE, "Exception executing {0}: {1}",
						LogFactory.args(pluginInfo.getId(), e.getMessage()));
					failedPlugins.add(pluginInfo.getId());

					if (config.isFailFast())
					{
						throw new RuntimeException("Plugin execution failed: " + pluginInfo.getId(), e);
					}
				}
				finally
				{
					pluginSpan.end();
				}
			}

			// Build final document from context
			Span buildSpan = tracer.spanBuilder("build-vim-document").startSpan();
			try (var bScope = buildSpan.makeCurrent())
			{
				executionEngine.buildVimDocumentFromContext(vimDoc, context);
			}
			finally
			{
				buildSpan.end();
			}

			return vimDoc;
		}
		finally
		{
			autoSpan.end();
		}
	}

	/**
	 * Check if plugin should be included in auto mode
	 */
	private boolean shouldInclude(PluginInfo info, AutoConfig config)
	{
		// Check explicit exclusion
		if (config.isExcluded(info.getId()))
		{
			return false;
		}

		// Check auto-enabled flag
		if (config.isSkipDisabled() && !info.isAutoEnabled())
		{
			return false;
		}

		return true;
	}

	/**
	 * Check if all dependencies succeeded
	 */
	private boolean checkDependencies(PluginInfo plugin, PluginContext context, List<String> failedPlugins)
	{
		for (String predecessor : plugin.getPredecessors())
		{
			if (failedPlugins.contains(predecessor))
			{
				return false;
			}

			PluginResult result = context.getPreviousResult(predecessor);
			if (result == null || !result.isSuccess())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Execute a single plugin
	 */
	private PluginResult executePlugin(PluginInfo pluginInfo, PluginContext context)
	{
		switch (pluginInfo.getType())
		{
			case EXTRACTOR:
				ExtractorPlugin extractor = pluginManager.getExtractorPlugin(pluginInfo.getId());
				if (extractor != null)
				{
					return extractor.extract(context);
				}
				break;

			case PARSER:
				ParserPlugin parser = pluginManager.getParserPlugin(pluginInfo.getId());
				if (parser != null)
				{
					return parser.parse(context);
				}
				break;

			default:
				// Visualizers are not executed in auto mode
				break;
		}

		return PluginResult.failure("Plugin not found: " + pluginInfo.getId());
	}

	/**
	 * Get dependency information for display
	 */
	public String formatDependencyGraph()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Available Plugins\n");
		sb.append("=================\n");
		sb.append(String.format("%-12s %-10s %-14s %-7s %-8s%n",
			"ID", "Phase", "Predecessors", "Auto", "Status"));
		sb.append(String.format("%-12s %-10s %-14s %-7s %-8s%n",
			"-".repeat(12), "-".repeat(10), "-".repeat(14), "-".repeat(7), "-".repeat(8)));

		// Extractors
		for (ExtractorPlugin plugin : pluginManager.getExtractorPlugins())
		{
			PluginInfo info = PluginInfo.fromExtractor(plugin);
			String preds = info.getPredecessors().isEmpty() ? "-" : String.join(",", info.getPredecessors());
			sb.append(String.format("%-12s %-10s %-14s %-7s %-8s%n",
				info.getId(),
				info.getPhase(),
				preds,
				info.isAutoEnabled() ? "yes" : "no",
				"loaded"));
		}

		// Parsers
		for (ParserPlugin plugin : pluginManager.getParserPlugins())
		{
			PluginInfo info = PluginInfo.fromParser(plugin);
			String preds = info.getPredecessors().isEmpty() ? "-" : String.join(",", info.getPredecessors());
			sb.append(String.format("%-12s %-10s %-14s %-7s %-8s%n",
				info.getId(),
				info.getPhase(),
				preds,
				info.isAutoEnabled() ? "yes" : "no",
				"loaded"));
		}

		// Visualizers
		for (var plugin : pluginManager.getVisualizerPlugins())
		{
			PluginInfo info = PluginInfo.fromVisualizer(plugin);
			String preds = "*";  // Depends on all
			sb.append(String.format("%-12s %-10s %-14s %-7s %-8s%n",
				info.getId(),
				info.getPhase(),
				preds,
				info.isAutoEnabled() ? "yes" : "no",
				"loaded"));
		}

		// Dependency graph visualization
		sb.append("\nDependency Graph:\n");

		List<String> extractors = pluginManager.getExtractorPlugins().stream()
			.map(ExtractorPlugin::getId)
			.collect(Collectors.toList());

		List<String> parsers = pluginManager.getParserPlugins().stream()
			.map(ParserPlugin::getId)
			.collect(Collectors.toList());

		if (!extractors.isEmpty())
		{
			for (String ext : extractors)
			{
				sb.append(String.format("  %s", ext));
				if (extractors.indexOf(ext) < extractors.size() - 1)
				{
					sb.append(" ──┐\n");
				}
				else
				{
					sb.append(" ───┤\n");
				}
			}

			if (!parsers.isEmpty())
			{
				sb.append("             │\n");
				for (String parser : parsers)
				{
					sb.append(String.format("             └──> %s\n", parser));
				}
			}
		}

		sb.append("\nLegend: * = depends on all previous phases\n");

		return sb.toString();
	}
}
