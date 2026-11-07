/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.lemmatizer.api.LemmatizerBackend;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.StemmedToken;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Apache Lucene Stemmer backend
 * Supports multiple languages via Lucene analyzers
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class LuceneBackend implements LemmatizerBackend
{
	private static final Logger LOG = LogFactory.getLogger(LuceneBackend.class);

	public static final String BACKEND_ID = "lucene";
	public static final String BACKEND_NAME = "Apache Lucene Stemmer";

	// Supported languages with their analyzer factories
	private static final Map<String, Supplier<Analyzer>> ANALYZER_FACTORIES = Map.of(
		"cs", CzechAnalyzer::new,
		"de", GermanAnalyzer::new,
		"en", EnglishAnalyzer::new,
		"es", SpanishAnalyzer::new,
		"fr", FrenchAnalyzer::new,
		"it", ItalianAnalyzer::new
	);

	// Fallback analyzer for unsupported languages
	private static final Supplier<Analyzer> FALLBACK_ANALYZER = StandardAnalyzer::new;

	// Cache for analyzers (thread-safe)
	private final Map<String, Analyzer> analyzerCache;

	public LuceneBackend()
	{
		this.analyzerCache = new ConcurrentHashMap<>();
	}

	@Override
	public String getBackendId()
	{
		return BACKEND_ID;
	}

	@Override
	public String getBackendName()
	{
		return BACKEND_NAME;
	}

	@Override
	public Set<String> getSupportedLanguages()
	{
		return ANALYZER_FACTORIES.keySet();
	}

	@Override
	public boolean isAvailable()
	{
		return true;  // Lucene is always available (pure Java)
	}

	@Override
	public StemmedResult stem(String text, String language)
	{
		long startTime = System.currentTimeMillis();
		StemmedResult result = new StemmedResult(language, BACKEND_ID);

		if (text == null || text.isEmpty())
		{
			result.calculateStatistics(System.currentTimeMillis() - startTime);
			return result;
		}

		Analyzer analyzer = getAnalyzer(language);
		int tokenId = 1;

		try (TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(text)))
		{
			CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
			OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);

			tokenStream.reset();

			while (tokenStream.incrementToken())
			{
				String stem = termAttr.toString();
				int startOffset = offsetAttr.startOffset();
				int endOffset = offsetAttr.endOffset();

				// Get original surface form from text
				String surface = text.substring(startOffset, endOffset);

				StemmedToken stemmedToken = new StemmedToken(
					"t" + String.format("%03d", tokenId++),
					surface,
					stem,
					startOffset,
					endOffset
				);
				result.addToken(stemmedToken);
			}

			tokenStream.end();
		}
		catch (IOException e)
		{
			LOG.log(Level.WARNING, "Error during tokenization: {0}", e.getMessage());
		}

		result.calculateStatistics(System.currentTimeMillis() - startTime);

		if (LOG.isLoggable(Level.FINE))
		{
			LOG.log(Level.FINE, "Stemmed {0} tokens ({1}) in {2}ms",
					LogFactory.args(result.getTokens().size(), language,
							result.getStatistics().getProcessingTimeMs()));
		}

		return result;
	}

	/**
	 * Get or create analyzer for given language
	 */
	private Analyzer getAnalyzer(String language)
	{
		return analyzerCache.computeIfAbsent(language, lang -> {
			Supplier<Analyzer> factory = ANALYZER_FACTORIES.get(lang);
			if (factory != null)
			{
				LOG.log(Level.FINE, "Created Lucene analyzer for language: {0}", lang);
				return factory.get();
			}
			else
			{
				LOG.log(Level.INFO, "No specific analyzer for language {0}, using StandardAnalyzer", lang);
				return FALLBACK_ANALYZER.get();
			}
		});
	}

	/**
	 * Close all cached analyzers
	 */
	public void close()
	{
		for (Analyzer analyzer : analyzerCache.values())
		{
			analyzer.close();
		}
		analyzerCache.clear();
	}
}
