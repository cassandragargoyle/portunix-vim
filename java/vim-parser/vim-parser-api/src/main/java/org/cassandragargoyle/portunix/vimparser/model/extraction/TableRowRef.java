/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Reference from a merged table row back to its originating row in a
 * source table fragment
 *
 * @author Zdenek
 * @since 2026-10-10
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"source_id", "table_id", "row_index"
})
public class TableRowRef
{
	@JsonProperty("source_id")
	private String sourceId;

	@JsonProperty("table_id")
	private String tableId;

	@JsonProperty("row_index")
	private Integer rowIndex;

	public TableRowRef()
	{
	}

	public TableRowRef(String sourceId, String tableId, Integer rowIndex)
	{
		this.sourceId = sourceId;
		this.tableId = tableId;
		this.rowIndex = rowIndex;
	}

	public String getSourceId()
	{
		return sourceId;
	}

	public void setSourceId(String sourceId)
	{
		this.sourceId = sourceId;
	}

	public String getTableId()
	{
		return tableId;
	}

	public void setTableId(String tableId)
	{
		this.tableId = tableId;
	}

	public Integer getRowIndex()
	{
		return rowIndex;
	}

	public void setRowIndex(Integer rowIndex)
	{
		this.rowIndex = rowIndex;
	}
}
