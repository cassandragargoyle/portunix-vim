/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Result of stemming/lemmatization operation
 * Contains tokens and statistics
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"language", "backend", "tokens", "statistics"})
public class StemmedResult
{
	@JsonProperty("language")
	private String language;

	@JsonProperty("backend")
	private String backend;

	@JsonProperty("tokens")
	private List<StemmedToken> tokens;

	@JsonProperty("statistics")
	private Statistics statistics;

	public StemmedResult()
	{
		this.tokens = new ArrayList<>();
	}

	public StemmedResult(String language, String backend)
	{
		this();
		this.language = language;
		this.backend = backend;
	}

	public String getLanguage()
	{
		return language;
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public String getBackend()
	{
		return backend;
	}

	public void setBackend(String backend)
	{
		this.backend = backend;
	}

	public List<StemmedToken> getTokens()
	{
		return tokens;
	}

	public void setTokens(List<StemmedToken> tokens)
	{
		this.tokens = tokens;
	}

	public void addToken(StemmedToken token)
	{
		this.tokens.add(token);
	}

	public Statistics getStatistics()
	{
		return statistics;
	}

	public void setStatistics(Statistics statistics)
	{
		this.statistics = statistics;
	}

	/**
	 * Calculate and set statistics based on current tokens
	 * @param processingTimeMs processing time in milliseconds
	 */
	public void calculateStatistics(long processingTimeMs)
	{
		Set<String> uniqueStems = new HashSet<>();
		for (StemmedToken token : tokens)
		{
			uniqueStems.add(token.getStem());
		}

		this.statistics = new Statistics(tokens.size(), uniqueStems.size(), processingTimeMs);
	}

	/**
	 * Statistics about stemming result
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonPropertyOrder({"totalTokens", "uniqueStems", "processingTimeMs"})
	public static class Statistics
	{
		@JsonProperty("totalTokens")
		private int totalTokens;

		@JsonProperty("uniqueStems")
		private int uniqueStems;

		@JsonProperty("processingTimeMs")
		private long processingTimeMs;

		public Statistics()
		{
		}

		public Statistics(int totalTokens, int uniqueStems, long processingTimeMs)
		{
			this.totalTokens = totalTokens;
			this.uniqueStems = uniqueStems;
			this.processingTimeMs = processingTimeMs;
		}

		public int getTotalTokens()
		{
			return totalTokens;
		}

		public void setTotalTokens(int totalTokens)
		{
			this.totalTokens = totalTokens;
		}

		public int getUniqueStems()
		{
			return uniqueStems;
		}

		public void setUniqueStems(int uniqueStems)
		{
			this.uniqueStems = uniqueStems;
		}

		public long getProcessingTimeMs()
		{
			return processingTimeMs;
		}

		public void setProcessingTimeMs(long processingTimeMs)
		{
			this.processingTimeMs = processingTimeMs;
		}
	}
}
