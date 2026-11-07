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
 * Single row in an extraction-layer table fragment
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"row_index", "is_header", "cells"
})
public class TableRow
{
	@JsonProperty("row_index")
	private Integer rowIndex;

	@JsonProperty("is_header")
	private Boolean isHeader;

	@JsonProperty("cells")
	private List<TableCell> cells;

	public TableRow()
	{
		this.cells = new ArrayList<>();
	}

	public Integer getRowIndex()
	{
		return rowIndex;
	}

	public void setRowIndex(Integer rowIndex)
	{
		this.rowIndex = rowIndex;
	}

	public Boolean getIsHeader()
	{
		return isHeader;
	}

	public void setIsHeader(Boolean isHeader)
	{
		this.isHeader = isHeader;
	}

	public List<TableCell> getCells()
	{
		return cells;
	}

	public void setCells(List<TableCell> cells)
	{
		this.cells = cells;
	}
}
