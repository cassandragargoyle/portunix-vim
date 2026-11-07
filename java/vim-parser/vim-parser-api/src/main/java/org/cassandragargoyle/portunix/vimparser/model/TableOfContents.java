/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a document's Table of Contents derived from heading detection
 *
 * @author Zdenek
 * @since 2026-03-05
 */
@JsonPropertyOrder({"entries", "toc_page"})
public class TableOfContents
{
	@JsonProperty("entries")
	private List<TocEntry> entries;

	@JsonProperty("toc_page")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer tocPage;

	public TableOfContents()
	{
		this.entries = new ArrayList<>();
	}

	public List<TocEntry> getEntries()
	{
		return entries;
	}

	public void setEntries(List<TocEntry> entries)
	{
		this.entries = entries;
	}

	public void addEntry(TocEntry entry)
	{
		this.entries.add(entry);
	}

	public Integer getTocPage()
	{
		return tocPage;
	}

	public void setTocPage(Integer tocPage)
	{
		this.tocPage = tocPage;
	}

	@JsonIgnore
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}

	@JsonIgnore
	public int size()
	{
		return entries.size();
	}
}
