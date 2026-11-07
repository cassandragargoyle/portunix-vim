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
 * Merged table at the document level, assembled from one or more
 * ExtractionTable fragments
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"table_id", "label", "columns", "rows", "source_fragments", "total_rows", "confidence"
})
public class DocumentTable
{
	@JsonProperty("table_id")
	private String tableId;

	@JsonProperty("label")
	private String label;

	@JsonProperty("columns")
	private List<ColumnDef> columns;

	@JsonProperty("rows")
	private List<DocumentTableRow> rows;

	@JsonProperty("source_fragments")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<String> sourceFragments;

	@JsonProperty("total_rows")
	private Integer totalRows;

	@JsonProperty("confidence")
	private Double confidence;

	public DocumentTable()
	{
		this.columns = new ArrayList<>();
		this.rows = new ArrayList<>();
		this.sourceFragments = new ArrayList<>();
	}

	public String getTableId()
	{
		return tableId;
	}

	public void setTableId(String tableId)
	{
		this.tableId = tableId;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public List<ColumnDef> getColumns()
	{
		return columns;
	}

	public void setColumns(List<ColumnDef> columns)
	{
		this.columns = columns;
	}

	public List<DocumentTableRow> getRows()
	{
		return rows;
	}

	public void setRows(List<DocumentTableRow> rows)
	{
		this.rows = rows;
	}

	public List<String> getSourceFragments()
	{
		return sourceFragments;
	}

	public void setSourceFragments(List<String> sourceFragments)
	{
		this.sourceFragments = sourceFragments;
	}

	public Integer getTotalRows()
	{
		return totalRows;
	}

	public void setTotalRows(Integer totalRows)
	{
		this.totalRows = totalRows;
	}

	public Double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(Double confidence)
	{
		this.confidence = confidence;
	}
}
