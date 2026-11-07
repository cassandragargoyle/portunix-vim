/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a single entry in the document's Table of Contents
 *
 * @author Zdenek
 * @since 2026-03-05
 */
@JsonPropertyOrder({"title", "page", "level", "block_id", "confidence", "source"})
public class TocEntry
{
	public static final String SOURCE_HEURISTIC = "heuristic";
	public static final String SOURCE_EXPLICIT = "explicit";

	@JsonProperty("title")
	private String title;

	@JsonProperty("page")
	private int page;

	@JsonProperty("level")
	private int level;

	@JsonProperty("block_id")
	private String blockId;

	@JsonProperty("confidence")
	private double confidence;

	@JsonProperty("source")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String source;

	public TocEntry()
	{
	}

	public TocEntry(String title, int page, int level, String blockId, double confidence)
	{
		this(title, page, level, blockId, confidence, SOURCE_HEURISTIC);
	}

	public TocEntry(String title, int page, int level, String blockId, double confidence, String source)
	{
		this.title = title;
		this.page = page;
		this.level = level;
		this.blockId = blockId;
		this.confidence = confidence;
		this.source = source;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public int getPage()
	{
		return page;
	}

	public void setPage(int page)
	{
		this.page = page;
	}

	public int getLevel()
	{
		return level;
	}

	public void setLevel(int level)
	{
		this.level = level;
	}

	public String getBlockId()
	{
		return blockId;
	}

	public void setBlockId(String blockId)
	{
		this.blockId = blockId;
	}

	public double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(double confidence)
	{
		this.confidence = confidence;
	}

	public String getSource()
	{
		return source;
	}

	public void setSource(String source)
	{
		this.source = source;
	}

	@Override
	public String toString()
	{
		return String.format("TocEntry[title='%s', page=%d, level=%d, confidence=%.2f]",
				title, page, level, confidence);
	}
}
