/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Table fragment extracted from one source, possibly part of a multi-source group
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"table_id", "group_id", "page_sequence", "bbox", "confidence", "columns", "rows"
})
public class ExtractionTable
{
	@JsonProperty("table_id")
	private String tableId;

	@JsonProperty("group_id")
	private String groupId;

	@JsonProperty("page_sequence")
	private Integer pageSequence;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	@JsonProperty("confidence")
	private Double confidence;

	@JsonProperty("columns")
	private List<ColumnDef> columns;

	@JsonProperty("rows")
	private List<TableRow> rows;

	public ExtractionTable()
	{
		this.columns = new ArrayList<>();
		this.rows = new ArrayList<>();
	}

	public String getTableId()
	{
		return tableId;
	}

	public void setTableId(String tableId)
	{
		this.tableId = tableId;
	}

	public String getGroupId()
	{
		return groupId;
	}

	public void setGroupId(String groupId)
	{
		this.groupId = groupId;
	}

	public Integer getPageSequence()
	{
		return pageSequence;
	}

	public void setPageSequence(Integer pageSequence)
	{
		this.pageSequence = pageSequence;
	}

	public BoundingBox getBbox()
	{
		return bbox;
	}

	public void setBbox(BoundingBox bbox)
	{
		this.bbox = bbox;
	}

	public Double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(Double confidence)
	{
		this.confidence = confidence;
	}

	public List<ColumnDef> getColumns()
	{
		return columns;
	}

	public void setColumns(List<ColumnDef> columns)
	{
		this.columns = columns;
	}

	public List<TableRow> getRows()
	{
		return rows;
	}

	public void setRows(List<TableRow> rows)
	{
		this.rows = rows;
	}
}
