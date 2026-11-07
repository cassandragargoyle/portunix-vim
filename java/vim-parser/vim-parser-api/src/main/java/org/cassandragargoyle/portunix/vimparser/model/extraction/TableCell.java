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
 * Single cell in an extraction-layer table fragment
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"column_id", "value", "raw_value", "confidence", "bbox", "row_span", "col_span", "revisions"
})
public class TableCell
{
	@JsonProperty("column_id")
	private String columnId;

	@JsonProperty("value")
	@JsonInclude(JsonInclude.Include.ALWAYS)
	private Object value;

	@JsonProperty("raw_value")
	private Object rawValue;

	@JsonProperty("confidence")
	private Double confidence;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	@JsonProperty("row_span")
	private Integer rowSpan;

	@JsonProperty("col_span")
	private Integer colSpan;

	@JsonProperty("revisions")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<Revision> revisions;

	public TableCell()
	{
		this.revisions = new ArrayList<>();
	}

	public String getColumnId()
	{
		return columnId;
	}

	public void setColumnId(String columnId)
	{
		this.columnId = columnId;
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}

	public Object getRawValue()
	{
		return rawValue;
	}

	public void setRawValue(Object rawValue)
	{
		this.rawValue = rawValue;
	}

	public Double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(Double confidence)
	{
		this.confidence = confidence;
	}

	public BoundingBox getBbox()
	{
		return bbox;
	}

	public void setBbox(BoundingBox bbox)
	{
		this.bbox = bbox;
	}

	public Integer getRowSpan()
	{
		return rowSpan;
	}

	public void setRowSpan(Integer rowSpan)
	{
		this.rowSpan = rowSpan;
	}

	public Integer getColSpan()
	{
		return colSpan;
	}

	public void setColSpan(Integer colSpan)
	{
		this.colSpan = colSpan;
	}

	public List<Revision> getRevisions()
	{
		return revisions;
	}

	public void setRevisions(List<Revision> revisions)
	{
		this.revisions = revisions;
	}
}
