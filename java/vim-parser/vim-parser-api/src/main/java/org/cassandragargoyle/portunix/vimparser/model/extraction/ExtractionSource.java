/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-source extraction result — fields and tables read from one input
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"source_id", "source_file", "source_page", "provenance", "fields", "tables"
})
public class ExtractionSource
{
	@JsonProperty("source_id")
	private String sourceId;

	@JsonProperty("source_file")
	private String sourceFile;

	@JsonProperty("source_page")
	private Integer sourcePage;

	@JsonProperty("provenance")
	private Provenance provenance;

	@JsonProperty("fields")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<ExtractionField> fields;

	@JsonProperty("tables")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<ExtractionTable> tables;

	public ExtractionSource()
	{
		this.fields = new ArrayList<>();
		this.tables = new ArrayList<>();
	}

	public String getSourceId()
	{
		return sourceId;
	}

	public void setSourceId(String sourceId)
	{
		this.sourceId = sourceId;
	}

	public String getSourceFile()
	{
		return sourceFile;
	}

	public void setSourceFile(String sourceFile)
	{
		this.sourceFile = sourceFile;
	}

	public Integer getSourcePage()
	{
		return sourcePage;
	}

	public void setSourcePage(Integer sourcePage)
	{
		this.sourcePage = sourcePage;
	}

	public Provenance getProvenance()
	{
		return provenance;
	}

	public void setProvenance(Provenance provenance)
	{
		this.provenance = provenance;
	}

	public List<ExtractionField> getFields()
	{
		return fields;
	}

	public void setFields(List<ExtractionField> fields)
	{
		this.fields = fields;
	}

	public List<ExtractionTable> getTables()
	{
		return tables;
	}

	public void setTables(List<ExtractionTable> tables)
	{
		this.tables = tables;
	}
}
