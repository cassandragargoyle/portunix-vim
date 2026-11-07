/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext;

import org.apache.tika.Tika;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.PagedText;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.DocumentMetadata;
import org.cassandragargoyle.portunix.vimparser.model.FulltextContent;
import org.pf4j.Extension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Metadata and fulltext extractor plugin using Apache Tika
 * Extracts Schema.org compatible metadata and raw text content
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@Extension
public class MetadataExtractorPlugin implements ExtractorPlugin, OrchestrationAware
{
	private static final Logger LOG = LogFactory.getLogger(MetadataExtractorPlugin.class);

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

	public static final String PLUGIN_ID = "metadata";

	public static final String RESULT_KEY_METADATA = "documentMetadata";

	public static final String RESULT_KEY_FULLTEXT = "fulltextContent";

	// Context option keys
	public static final String OPTION_INCLUDE_METADATA = "includeMetadata";

	public static final String OPTION_INCLUDE_FULLTEXT = "includeFulltext";

	public static final String OPTION_INCLUDE_HASH = "includeHash";

	private final Tika tika;

	private final AutoDetectParser parser;

	public MetadataExtractorPlugin()
	{
		this.tika = new Tika();
		this.parser = new AutoDetectParser();
	}

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "Metadata & Fulltext Extractor";
	}

	@Override
	public String getVersion()
	{
		return "0.1.0";
	}

	@Override
	public int getPriority()
	{
		return 5;  // Run before text block extractor
	}

	@Override
	public Set<String> getDependencies()
	{
		return Collections.emptySet();
	}

	@Override
	public Set<String> getPredecessors()
	{
		return Collections.emptySet();
	}

	@Override
	public boolean isAutoEnabled()
	{
		return true;
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.EXTRACT;
	}

	@Override
	public ExecutionMode[] getSupportedModes()
	{
		return new ExecutionMode[]
		{
			ExecutionMode.SYNC,
			ExecutionMode.ASYNC_THREAD
		};
	}

	@Override
	public PluginResult extract(PluginContext context)
	{
		long startTime = System.currentTimeMillis();

		try
		{
			byte[] pdfContent = context.getPdfContent();
			if (pdfContent == null || pdfContent.length == 0)
			{
				return PluginResult.failure("No PDF content provided");
			}

			// Check options (default: metadata=true, fulltext=true, hash=false)
			boolean includeMetadata = getBooleanOption(context, OPTION_INCLUDE_METADATA, true);
			boolean includeFulltext = getBooleanOption(context, OPTION_INCLUDE_FULLTEXT, true);
			boolean includeHash = getBooleanOption(context, OPTION_INCLUDE_HASH, false);

			DocumentMetadata metadata = null;
			FulltextContent fulltext = null;

			if (includeMetadata || includeFulltext)
			{
				// Extract using Tika
				Metadata tikaMetadata = new Metadata();
				String extractedText = null;

				try (InputStream stream = new ByteArrayInputStream(pdfContent))
				{
					// Limit content extraction to 10MB
					BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024);
					ParseContext parseContext = new ParseContext();

					parser.parse(stream, handler, tikaMetadata, parseContext);
					extractedText = handler.toString();
				}

				if (includeMetadata)
				{
					metadata = buildMetadata(context, tikaMetadata, extractedText, pdfContent.length, includeHash);
					context.putSharedData(RESULT_KEY_METADATA, metadata);
				}

				if (includeFulltext && extractedText != null)
				{
					fulltext = new FulltextContent(extractedText.trim());
					context.putSharedData(RESULT_KEY_FULLTEXT, fulltext);
				}
			}

			PluginResult result = PluginResult.success(metadata);
			result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

			if (metadata != null)
			{
				result.addMetadata("has_metadata", "true");
			}
			if (fulltext != null)
			{
				result.addMetadata("has_fulltext", "true");
				result.addMetadata("word_count", String.valueOf(fulltext.getWordCount()));
			}

			return result;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Error extracting metadata: {0}", e.getMessage());
			return PluginResult.failure("Metadata extraction failed: " + e.getMessage());
		}
	}

	private DocumentMetadata buildMetadata(PluginContext context, Metadata tikaMetadata,
										   String extractedText, long contentSize, boolean includeHash)
	{
		DocumentMetadata metadata = new DocumentMetadata();

		// MIME type
		String mimeType = tikaMetadata.get(Metadata.CONTENT_TYPE);
		if (mimeType == null)
		{
			mimeType = "application/pdf";
		}
		metadata.setEncodingFormat(mimeType);

		// Content size
		metadata.setContentSize(contentSize);

		// File timestamps from source file if available
		String sourcePath = context.getSourcePath();
		if (sourcePath != null)
		{
			File sourceFile = new File(sourcePath);
			if (sourceFile.exists())
			{
				try
				{
					BasicFileAttributes attrs = Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
					metadata.setDateCreated(formatInstant(attrs.creationTime().toInstant()));
					metadata.setDateModified(formatInstant(attrs.lastModifiedTime().toInstant()));
				}
				catch (Exception e)
				{
					LOG.log(Level.FINE, "Could not read file attributes: {0}", e.getMessage());
				}
			}
		}

		// Creator/Author
		String author = tikaMetadata.get(TikaCoreProperties.CREATOR);
		if (author != null && !author.isEmpty())
		{
			metadata.setCreator(author);
		}

		// Producer (application that created the file)
		String producer = tikaMetadata.get("pdf:producer");
		if (producer == null)
		{
			producer = tikaMetadata.get(TikaCoreProperties.CREATOR_TOOL);
		}
		if (producer != null && !producer.isEmpty())
		{
			metadata.setProducer(producer);
		}

		// Title
		String title = tikaMetadata.get(TikaCoreProperties.TITLE);
		if (title != null && !title.isEmpty())
		{
			metadata.setTitle(title);
		}

		// Page count
		String pageCount = tikaMetadata.get(PagedText.N_PAGES);
		if (pageCount != null)
		{
			metadata.addProperty("pageCount", pageCount);
		}

		// Language detection
		String language = tikaMetadata.get(TikaCoreProperties.LANGUAGE);
		if ((language == null || language.isEmpty()) && extractedText != null && extractedText.length() > 50)
		{
			language = detectLanguage(extractedText);
		}
		if (language != null && !language.isEmpty())
		{
			metadata.setInLanguage(language);
		}

		// SHA-256 hash if requested
		if (includeHash)
		{
			String hash = calculateSha256(context.getPdfContent());
			if (hash != null)
			{
				metadata.setSha256(hash);
			}
		}

		return metadata;
	}

	private String detectLanguage(String text)
	{
		try
		{
			LanguageDetector detector = LanguageDetector.getDefaultLanguageDetector();
			detector.loadModels();

			// Use first 10000 chars for detection
			String sample = text.length() > 10000 ? text.substring(0, 10000) : text;
			LanguageResult result = detector.detect(sample);

			if (result.getRawScore() >= 0.5f || result.isReasonablyCertain())
			{
				return result.getLanguage();
			}
		}
		catch (Exception e)
		{
			LOG.log(Level.FINE, "Language detection failed: {0}", e.getMessage());
		}
		return null;
	}

	private String calculateSha256(byte[] content)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(content);
			StringBuilder sb = new StringBuilder();
			for (byte b : hashBytes)
			{
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, "Could not calculate hash: {0}", e.getMessage());
			return null;
		}
	}

	private String formatInstant(Instant instant)
	{
		return instant.atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
	}

	private boolean getBooleanOption(PluginContext context, String key, boolean defaultValue)
	{
		Object value = context.getOptions().get(key);
		if (value instanceof Boolean)
		{
			return (Boolean) value;
		}
		if (value instanceof String)
		{
			return Boolean.parseBoolean((String) value);
		}
		return defaultValue;
	}
}
