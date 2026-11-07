/*
 * SPDX-License-Identifier: MIT
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cassandragargoyle.api.telemetry.ExporterType;
import org.cassandragargoyle.api.telemetry.TelemetryProvider;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.ParserPlugin;
import org.cassandragargoyle.portunix.vimparser.api.PluginInfo;
import org.cassandragargoyle.portunix.vimparser.api.VisualizerPlugin;
import org.cassandragargoyle.portunix.vimparser.config.VimParserConfig;
import org.cassandragargoyle.portunix.vimparser.core.PluginManager;
import org.cassandragargoyle.portunix.vimparser.engine.ExecutionEngine;
import org.cassandragargoyle.portunix.vimparser.json.VimJsonMapper;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.cassandragargoyle.portunix.vimparser.orchestration.AutoConfig;
import org.cassandragargoyle.portunix.vimparser.orchestration.AutoOrchestrator;
import org.cassandragargoyle.portunix.vimparser.orchestration.ExecutionPlan;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.LogManager;

/**
 * Main entry point for VIM Parser Plugin
 * Modular Layered Information Model (VIM) parser for semantic extraction from documents
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@Command(
	name = "vim-parser",
	mixinStandardHelpOptions = true,
	version = "vim-parser 0.1.1",
	description = "Modular Layered Information Model (VIM) parser for semantic extraction from documents",
	subcommands =
	{
		Main.AutoCommand.class,
		Main.ExtractCommand.class,
		Main.VisualizeCommand.class,
		Main.PluginsCommand.class
	}
)
public class Main implements Callable<Integer>
{
	private static PluginManager pluginManager;

	private static ExecutionEngine executionEngine;

	private static TelemetryProvider telemetryProvider;

	@Option(names = {"--trace"}, description = "Enable console trace output (development shortcut)")
	private boolean trace = false;

	@Option(names = {"--trace-endpoint"}, description = "Override OTLP endpoint (e.g., localhost:4317)")
	private String traceEndpoint;

	public static void main(String[] args)
	{
		// Configure logging to suppress PDFBox warnings
		try (InputStream is = Main.class.getResourceAsStream("/logging.properties"))
		{
			if (is != null)
			{
				LogManager.getLogManager().readConfiguration(is);
			}
		}
		catch (Exception e)
		{
			// Ignore logging configuration errors
		}

		// Force UTF-8 output encoding
		System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
		System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

		// Initialize plugin system
		pluginManager = new PluginManager();
		pluginManager.discoverPlugins();
		executionEngine = new ExecutionEngine(pluginManager);

		Main main = new Main();
		CommandLine cmd = new CommandLine(main)
			.setExecutionExceptionHandler((ex, cmdLine, parseResult) ->
			{
				System.err.println("Error: " + ex.getMessage());
				return 1;
			});

		// Parse to get --trace flags before execution
		cmd.parseArgs(args);
		main.initTelemetry();
		executionEngine.setTelemetryProvider(telemetryProvider);

		int exitCode = cmd.execute(args);

		// Cleanup
		telemetryProvider.shutdown();
		executionEngine.shutdown();
		pluginManager.shutdown();

		System.exit(exitCode);
	}

	/**
	 * Report a missing plugin installation; extraction without plugins writes an empty document
	 * @return true when at least one extractor or parser plugin was found
	 */
	private static boolean checkPluginsFound()
	{
		if (pluginManager.hasExtractionPlugins())
		{
			return true;
		}
		System.err.println("Error: No vim-parser plugins found. Searched: " + pluginManager.describePluginSource());
		System.err.println("Plugins are loaded from a \"plugins\" directory next to the JAR, or in the working directory.");
		return false;
	}

	/**
	 * Initialize TelemetryProvider based on CLI flags
	 */
	private void initTelemetry()
	{
		if (trace)
		{
			TelemetryProvider.enableTraceLogging();

			TelemetryProvider.Builder builder = TelemetryProvider.builder("ptx-vim-parser")
				.enabled(true)
				.exporter(ExporterType.CONSOLE);

			if (traceEndpoint != null)
			{
				builder.exporter(ExporterType.OTLP).endpoint(traceEndpoint);
			}

			telemetryProvider = builder.build();
			return;
		}

		if (traceEndpoint != null)
		{
			TelemetryProvider.enableTraceLogging();
			telemetryProvider = TelemetryProvider.builder("ptx-vim-parser")
				.enabled(true)
				.exporter(ExporterType.OTLP)
				.endpoint(traceEndpoint)
				.build();
			return;
		}

		telemetryProvider = TelemetryProvider.noOp();
	}

	public static TelemetryProvider getTelemetryProvider()
	{
		return telemetryProvider;
	}

	@Override
	public Integer call()
	{
		// No subcommand specified, print usage
		CommandLine.usage(this, System.out);
		return 0;
	}

	/**
	 * Auto command - automatic orchestration mode
	 */
	@Command(
		name = "auto",
		description = "Automatically extract VIM layers using all available plugins in correct order",
		mixinStandardHelpOptions = true
	)
	static class AutoCommand implements Callable<Integer>
	{
		@Parameters(index = "0", description = "Input PDF file")
		private File inputFile;

		@Option(names =
		{
			"-o", "--output"
		}, description = "Output file (default: stdout)")
		private File outputFile;

		@Option(names =
		{
			"-w", "--write"
		}, description = "Write output to sidecar file (<input>.vim.json)")
		private boolean writeSidecar = false;

		@Option(names =
		{
			"--compact"
		}, description = "Output compact JSON (no pretty printing)")
		private boolean compact = false;

		@Option(names =
		{
			"--exclude"
		}, description = "Comma-separated list of plugins to exclude")
		private String excludePlugins;

		@Option(names =
		{
			"--no-fail-fast"
		}, description = "Continue on plugin errors instead of aborting")
		private boolean noFailFast = false;

		@Option(names =
		{
			"--dry-run", "--plan"
		}, description = "Show execution plan without running")
		private boolean dryRun = false;

		@Option(names =
		{
			"-v", "--verbose"
		}, description = "Verbose output")
		private boolean verbose = false;

		@Option(names =
		{
			"--metadata"
		}, description = "Include document metadata (default: true)", negatable = true)
		private Boolean metadata;

		@Option(names =
		{
			"--fulltext"
		}, description = "Include raw fulltext content (default: true)", negatable = true)
		private Boolean fulltext;

		@Option(names =
		{
			"--hash"
		}, description = "Include SHA-256 hash in metadata (default: false)", negatable = true)
		private Boolean hash;

		@Option(names =
		{
			"--no-drop-cap"
		}, description = "Disable drop cap detection and merging")
		private boolean noDropCap = false;

		@Override
		public Integer call() throws Exception
		{
			// Validate input
			if (!inputFile.exists())
			{
				System.err.println("Error: File does not exist: " + inputFile);
				return 1;
			}

			if (!inputFile.isFile())
			{
				System.err.println("Error: Path is not a file: " + inputFile);
				return 1;
			}

			String fileName = inputFile.getName().toLowerCase();
			if (!fileName.endsWith(".pdf"))
			{
				System.err.println("Error: Only PDF files are supported: " + inputFile);
				return 1;
			}

			if (!checkPluginsFound())
			{
				return 1;
			}

			// Build auto config (use AutoConfig defaults, override only if explicitly set)
			AutoConfig config = AutoConfig.defaults()
				.setFailFast(!noFailFast)
				.setDryRun(dryRun)
				.setVerbose(verbose);

			if (metadata != null) config.setIncludeMetadata(metadata);
			if (fulltext != null) config.setIncludeFulltext(fulltext);
			if (hash != null) config.setIncludeHash(hash);
			if (noDropCap) config.setDropCapEnabled(false);

			if (excludePlugins != null && !excludePlugins.isEmpty())
			{
				for (String pluginId : excludePlugins.split(","))
				{
					config.exclude(pluginId.trim());
				}
			}

			// Read PDF content
			byte[] pdfContent = Files.readAllBytes(inputFile.toPath());

			// Create orchestrator
			AutoOrchestrator orchestrator = new AutoOrchestrator(pluginManager, executionEngine);

			if (dryRun)
			{
				// Show execution plan
				System.out.println("Plugins: " + pluginManager.describePluginSource());
				ExecutionPlan plan = orchestrator.buildPlan(pdfContent, inputFile.getName(), config);
				System.out.println(plan.format());
				System.out.println("\nRun without --dry-run to execute.");
				return 0;
			}

			if (verbose)
			{
				System.err.println("Processing: " + inputFile.getAbsolutePath());
			}

			// Execute
			VimDocument vimDoc = orchestrator.execute(pdfContent, inputFile.getName(), config);

			// Convert to JSON
			ObjectMapper mapper = VimJsonMapper.writer(!compact);
			String json = mapper.writeValueAsString(vimDoc);

			// Determine output destination
			if (outputFile != null)
			{
				Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);
				if (verbose)
				{
					System.err.println("Output written to: " + outputFile);
				}
			}
			else if (writeSidecar)
			{
				Path sidecarPath = Path.of(inputFile.getAbsolutePath() + ".vim.json");
				Files.writeString(sidecarPath, json, StandardCharsets.UTF_8);
				if (verbose)
				{
					System.err.println("Output written to: " + sidecarPath);
				}
			}
			else
			{
				System.out.println(json);
			}

			return 0;
		}
	}

	/**
	 * Extract command - main extraction using modular plugins
	 */
	@Command(
		name = "extract",
		description = "Extract VIM layers from PDF document using modular plugins",
		mixinStandardHelpOptions = true
	)
	static class ExtractCommand implements Callable<Integer>
	{
		@Parameters(index = "0", description = "Input PDF file")
		private File inputFile;

		@Option(names =
		{
			"-o", "--output"
		}, description = "Output file (default: stdout)")
		private File outputFile;

		@Option(names =
		{
			"-w", "--write"
		}, description = "Write output to sidecar file (<input>.vim.json)")
		private boolean writeSidecar = false;

		@Option(names =
		{
			"--compact"
		}, description = "Output compact JSON (no pretty printing)")
		private boolean compact = false;

		@Option(names =
		{
			"--mode"
		}, description = "Execution mode: sync, thread, grpc (default: sync)")
		private String mode = "sync";

		@Option(names =
		{
			"--threads"
		}, description = "Thread pool size for async mode (default: 4)")
		private int threads = 4;

		@Option(names =
		{
			"--plugins"
		}, description = "Comma-separated list of plugins to use (default: all)")
		private String plugins;

		@Option(names =
		{
			"--config"
		}, description = "Path to configuration file (vim-parser.json)")
		private File configFile;

		@Option(names =
		{
			"-v", "--verbose"
		}, description = "Verbose output")
		private boolean verbose = false;

		@Override
		public Integer call() throws Exception
		{
			// Validate input
			if (!inputFile.exists())
			{
				System.err.println("Error: File does not exist: " + inputFile);
				return 1;
			}

			if (!inputFile.isFile())
			{
				System.err.println("Error: Path is not a file: " + inputFile);
				return 1;
			}

			String fileName = inputFile.getName().toLowerCase();
			if (!fileName.endsWith(".pdf"))
			{
				System.err.println("Error: Only PDF files are supported: " + inputFile);
				return 1;
			}

			if (!checkPluginsFound())
			{
				return 1;
			}

			// Load configuration
			VimParserConfig config;
			if (configFile != null && configFile.exists())
			{
				config = VimParserConfig.load(configFile);
				if (verbose)
				{
					System.err.println("Loaded configuration from: " + configFile);
				}
			}
			else
			{
				config = VimParserConfig.defaultConfig();
			}

			// Configure execution mode
			ExecutionMode execMode = parseExecutionMode(mode);
			if (execMode == null)
			{
				execMode = config.getExecution().getDefaultMode();
			}
			executionEngine.setDefaultMode(execMode);
			executionEngine.setThreadPoolSize(threads > 0 ? threads : config.getExecution().getThreadPoolSize());

			if (verbose)
			{
				System.err.println("Extracting from: " + inputFile.getAbsolutePath());
				System.err.println("Execution mode: " + execMode);
			}

			// Parse enabled plugins
			Set<String> enabledPlugins = null;
			if (plugins != null && !plugins.isEmpty())
			{
				enabledPlugins = new HashSet<>(Arrays.asList(plugins.split(",")));
				if (verbose)
				{
					System.err.println("Enabled plugins: " + enabledPlugins);
				}
			}

			// Read PDF content
			byte[] pdfContent = Files.readAllBytes(inputFile.toPath());

			// Process
			VimDocument vimDoc = executionEngine.process(
				pdfContent,
				inputFile.getName(),
				verbose,
				enabledPlugins
			);

			// Convert to JSON
			ObjectMapper mapper = VimJsonMapper.writer(!compact);
			String json = mapper.writeValueAsString(vimDoc);

			// Determine output destination
			if (outputFile != null)
			{
				Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);
				if (verbose)
				{
					System.err.println("Output written to: " + outputFile);
				}
			}
			else if (writeSidecar)
			{
				Path sidecarPath = Path.of(inputFile.getAbsolutePath() + ".vim.json");
				Files.writeString(sidecarPath, json, StandardCharsets.UTF_8);
				if (verbose)
				{
					System.err.println("Output written to: " + sidecarPath);
				}
			}
			else
			{
				System.out.println(json);
			}

			return 0;
		}

		private ExecutionMode parseExecutionMode(String mode)
		{
			if (mode == null)
			{
				return null;
			}
			return switch (mode.toLowerCase())
			{
				case "sync" ->
					ExecutionMode.SYNC;
				case "thread", "async_thread" ->
					ExecutionMode.ASYNC_THREAD;
				case "grpc", "async_grpc" ->
					ExecutionMode.ASYNC_GRPC;
				default ->
					null;
			};
		}
	}

	/**
	 * Visualize command - generate HTML from VIM JSON
	 */
	@Command(
		name = "visualize",
		description = "Generate HTML visualization from VIM JSON output",
		mixinStandardHelpOptions = true
	)
	static class VisualizeCommand implements Callable<Integer>
	{
		@Parameters(index = "0", description = "Input VIM JSON file")
		private File inputFile;

		@Option(names =
		{
			"-o", "--output"
		}, description = "Output HTML file (default: stdout)")
		private File outputFile;

		@Option(names =
		{
			"-w", "--write"
		}, description = "Write output to sidecar file (<input>.html)")
		private boolean writeSidecar = false;

		@Option(names =
		{
			"--visualizer"
		}, description = "Visualizer plugin ID (default: html)")
		private String visualizerId = "html";

		@Override
		public Integer call() throws Exception
		{
			// Validate input
			if (!inputFile.exists())
			{
				System.err.println("Error: File does not exist: " + inputFile);
				return 1;
			}

			if (!inputFile.isFile())
			{
				System.err.println("Error: Path is not a file: " + inputFile);
				return 1;
			}

			String fileName = inputFile.getName().toLowerCase();
			if (!fileName.endsWith(".json"))
			{
				System.err.println("Error: Expected JSON file: " + inputFile);
				return 1;
			}

			// Get visualizer plugin
			VisualizerPlugin visualizer = pluginManager.getVisualizerPlugin(visualizerId);
			if (visualizer == null)
			{
				System.err.println("Error: Visualizer plugin not found: " + visualizerId);
				System.err.println("Available visualizers:");
				for (VisualizerPlugin v : pluginManager.getVisualizerPlugins())
				{
					System.err.println("  - " + v.getId() + " (" + v.getName() + ")");
				}
				return 1;
			}

			// Parse VIM document - strict scalar types, unknown fields tolerated
			ObjectMapper mapper = VimJsonMapper.strictReader();
			String jsonContent = Files.readString(inputFile.toPath());
			VimDocument vimDoc = mapper.readValue(jsonContent, VimDocument.class);

			// Generate visualization
			String html = visualizer.visualize(vimDoc);

			// Determine output destination
			if (outputFile != null)
			{
				Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
				System.err.println("HTML visualization written to: " + outputFile);
			}
			else if (writeSidecar)
			{
				// Replace .vim.json or .json with visualizer extension
				String baseName = inputFile.getAbsolutePath();
				if (baseName.endsWith(".vim.json"))
				{
					baseName = baseName.substring(0, baseName.length() - 9);
				}
				else if (baseName.endsWith(".json"))
				{
					baseName = baseName.substring(0, baseName.length() - 5);
				}
				Path htmlPath = Path.of(baseName + visualizer.getFileExtension());
				Files.writeString(htmlPath, html, StandardCharsets.UTF_8);
				System.err.println("HTML visualization written to: " + htmlPath);
			}
			else
			{
				System.out.println(html);
			}

			return 0;
		}
	}

	/**
	 * List available plugins
	 */
	@Command(
		name = "plugins",
		description = "List available plugins",
		mixinStandardHelpOptions = true
	)
	static class PluginsCommand implements Callable<Integer>
	{
		@Option(names =
		{
			"--deps"
		}, description = "Show dependency graph")
		private boolean showDeps = false;

		@Override
		public Integer call()
		{
			if (showDeps)
			{
				// Show dependency information
				AutoOrchestrator orchestrator = new AutoOrchestrator(pluginManager, executionEngine);
				System.out.println(orchestrator.formatDependencyGraph());
				return 0;
			}

			System.out.println("Available plugins:\n");

			System.out.println("Extractor plugins:");
			for (ExtractorPlugin plugin : pluginManager.getExtractorPlugins())
			{
				PluginInfo info = PluginInfo.fromExtractor(plugin);
				System.out.printf("  %-15s %s (v%s, priority: %d)%n",
								  plugin.getId(),
								  plugin.getName(),
								  plugin.getVersion(),
								  plugin.getPriority());
				System.out.printf("                  Modes: %s%n",
								  Arrays.toString(plugin.getSupportedModes()));
				System.out.printf("                  Phase: %s, Auto: %s%n",
								  info.getPhase(), info.isAutoEnabled() ? "yes" : "no");
				if (!info.getPredecessors().isEmpty())
				{
					System.out.printf("                  Predecessors: %s%n",
									  info.getPredecessors());
				}
			}

			System.out.println("\nParser plugins:");
			for (ParserPlugin plugin : pluginManager.getParserPlugins())
			{
				PluginInfo info = PluginInfo.fromParser(plugin);
				System.out.printf("  %-15s %s (v%s, priority: %d)%n",
								  plugin.getId(),
								  plugin.getName(),
								  plugin.getVersion(),
								  plugin.getPriority());
				System.out.printf("                  Phase: %s, Auto: %s%n",
								  info.getPhase(), info.isAutoEnabled() ? "yes" : "no");
				if (!info.getPredecessors().isEmpty())
				{
					System.out.printf("                  Predecessors: %s%n",
									  info.getPredecessors());
				}
			}

			System.out.println("\nVisualizer plugins:");
			for (VisualizerPlugin plugin : pluginManager.getVisualizerPlugins())
			{
				PluginInfo info = PluginInfo.fromVisualizer(plugin);
				System.out.printf("  %-15s %s (v%s, format: %s)%n",
								  plugin.getId(),
								  plugin.getName(),
								  plugin.getVersion(),
								  plugin.getOutputFormat());
				System.out.printf("                  Phase: %s, Auto: %s%n",
								  info.getPhase(), info.isAutoEnabled() ? "yes" : "no");
			}

			return 0;
		}
	}
}
