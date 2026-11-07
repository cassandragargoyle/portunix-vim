/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer;

import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.lemmatizer.api.BackendRegistry;
import org.cassandragargoyle.portunix.vimparser.lemmatizer.api.LemmatizerBackend;
import org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.lucene.LuceneBackend;
import org.cassandragargoyle.portunix.vimparser.model.DocumentMetadata;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.FulltextContent;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.pf4j.Extension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lemmatizer/Stemmer plugin for VIM Parser
 * Processes extracted fulltext and produces stemmed tokens
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@Extension
public class LemmatizerPlugin implements ExtractorPlugin, OrchestrationAware
{
	private static final Logger LOG = LogFactory.getLogger(LemmatizerPlugin.class);

	public static final String PLUGIN_ID = "lemmatizer";
	public static final String RESULT_KEY_STEMMING = "stemmingResult";

	// Context option keys
	public static final String OPTION_BACKEND = "stemmingBackend";
	public static final String OPTION_LANGUAGE = "stemmingLanguage";
	public static final String OPTION_ENABLED = "stemmingEnabled";

	// Default language
	private static final String DEFAULT_LANGUAGE = "cs";

	// Shared data keys from other plugins
	private static final String METADATA_KEY = "documentMetadata";
	private static final String FULLTEXT_KEY = "fulltextContent";
	private static final String BLOCKS_KEY = "textBlocks";

	private final BackendRegistry registry;

	public LemmatizerPlugin()
	{
		this.registry = new BackendRegistry();
		// Register default backend
		registry.register(new LuceneBackend());
	}

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "Lemmatizer/Stemmer";
	}

	@Override
	public String getVersion()
	{
		return "0.1.0";
	}

	@Override
	public int getPriority()
	{
		return 15;  // After fulltext (10) and metadata (5)
	}

	@Override
	public Set<String> getDependencies()
	{
		return Set.of("fulltext", "metadata");
	}

	@Override
	public Set<String> getPredecessors()
	{
		return Set.of("fulltext", "metadata");
	}

	@Override
	public boolean isAutoEnabled()
	{
		return false;  // Opt-in via --stemming flag
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.EXTRACT;
	}

	@Override
	public ExecutionMode[] getSupportedModes()
	{
		return new ExecutionMode[] {
			ExecutionMode.SYNC,
			ExecutionMode.ASYNC_THREAD
		};
	}

	@Override
	public PluginResult extract(PluginContext context)
	{
		long startTime = System.currentTimeMillis();

		// Check if stemming is enabled
		if (!isStemmingEnabled(context))
		{
			return PluginResult.success(null);
		}

		try
		{
			// Get text to process
			String text = getTextToProcess(context);
			if (text == null || text.isEmpty())
			{
				LOG.log(Level.WARNING, "No text available for stemming");
				return PluginResult.failure("No text available for stemming");
			}

			// Determine language
			String language = determineLanguage(context);

			// Get backend
			Optional<LemmatizerBackend> backendOpt = getBackend(context, language);
			if (backendOpt.isEmpty())
			{
				String msg = "No backend available for language: " + language;
				LOG.log(Level.WARNING, msg);
				return PluginResult.failure(msg);
			}

			LemmatizerBackend backend = backendOpt.get();

			if (context.isVerbose())
			{
				LOG.log(Level.INFO, "Using backend {0} for language {1}",
						LogFactory.args(backend.getBackendId(), language));
			}

			// Perform stemming
			StemmedResult result = backend.stem(text, language);

			// Store result
			context.putSharedData(RESULT_KEY_STEMMING, result);

			PluginResult pluginResult = PluginResult.success(result);
			pluginResult.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			pluginResult.addMetadata("backend", backend.getBackendId());
			pluginResult.addMetadata("language", language);
			pluginResult.addMetadata("token_count", String.valueOf(result.getTokens().size()));
			pluginResult.addMetadata("unique_stems", String.valueOf(result.getStatistics().getUniqueStems()));

			if (context.isVerbose())
			{
				LOG.log(Level.INFO, "Stemmed {0} tokens ({1} unique stems) in {2}ms",
						LogFactory.args(
							result.getTokens().size(),
							result.getStatistics().getUniqueStems(),
							result.getStatistics().getProcessingTimeMs()));
			}

			return pluginResult;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Stemming failed: {0}", e.getMessage());
			return PluginResult.failure("Stemming failed: " + e.getMessage());
		}
	}

	/**
	 * Check if stemming is enabled via context option
	 */
	private boolean isStemmingEnabled(PluginContext context)
	{
		Object value = context.getOption(OPTION_ENABLED);
		if (value instanceof Boolean)
		{
			return (Boolean) value;
		}
		if (value instanceof String)
		{
			return Boolean.parseBoolean((String) value);
		}
		return true;  // Default enabled when plugin is invoked
	}

	/**
	 * Get text to process from context
	 * Prefers fulltext content, falls back to text blocks
	 */
	private String getTextToProcess(PluginContext context)
	{
		// Try fulltext content first
		FulltextContent fulltext = context.getSharedData(FULLTEXT_KEY, FulltextContent.class);
		if (fulltext != null && fulltext.getContent() != null)
		{
			return fulltext.getContent();
		}

		// Fall back to text blocks
		@SuppressWarnings("unchecked")
		List<TextBlock> blocks = context.getSharedData(BLOCKS_KEY, List.class);
		if (blocks != null && !blocks.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			for (TextBlock block : blocks)
			{
				if (sb.length() > 0)
				{
					sb.append(" ");
				}
				sb.append(block.getFullText());
			}
			return sb.toString();
		}

		return null;
	}

	/**
	 * Determine language for stemming
	 * Priority: explicit option > detected from metadata > default
	 */
	private String determineLanguage(PluginContext context)
	{
		// Check explicit option
		Object langOption = context.getOption(OPTION_LANGUAGE);
		if (langOption instanceof String && !((String) langOption).isEmpty())
		{
			return (String) langOption;
		}

		// Check document metadata
		DocumentMetadata metadata = context.getSharedData(METADATA_KEY, DocumentMetadata.class);
		if (metadata != null && metadata.getInLanguage() != null)
		{
			return metadata.getInLanguage();
		}

		return DEFAULT_LANGUAGE;
	}

	/**
	 * Get backend for processing
	 * Priority: explicit option > best for language
	 */
	private Optional<LemmatizerBackend> getBackend(PluginContext context, String language)
	{
		// Check explicit backend option
		Object backendOption = context.getOption(OPTION_BACKEND);
		if (backendOption instanceof String && !((String) backendOption).isEmpty())
		{
			return registry.getBackend((String) backendOption);
		}

		// Get best backend for language
		return registry.getBackendForLanguage(language);
	}

	/**
	 * Get backend registry for backend management
	 */
	public BackendRegistry getRegistry()
	{
		return registry;
	}
}
