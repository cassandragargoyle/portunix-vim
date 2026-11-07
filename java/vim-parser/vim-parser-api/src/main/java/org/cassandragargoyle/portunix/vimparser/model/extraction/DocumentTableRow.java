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
 * Row in a merged document-level table, with reference back to its
 * originating source row
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"row_index", "cells", "source_ref"
})
public class DocumentTableRow
{
	@JsonProperty("row_index")
	private Integer rowIndex;

	@JsonProperty("cells")
	private List<TableCell> cells;

	@JsonProperty("source_ref")
	private TableRowRef sourceRef;

	public DocumentTableRow()
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

	public List<TableCell> getCells()
	{
		return cells;
	}

	public void setCells(List<TableCell> cells)
	{
		this.cells = cells;
	}

	public TableRowRef getSourceRef()
	{
		return sourceRef;
	}

	public void setSourceRef(TableRowRef sourceRef)
	{
		this.sourceRef = sourceRef;
	}
}
