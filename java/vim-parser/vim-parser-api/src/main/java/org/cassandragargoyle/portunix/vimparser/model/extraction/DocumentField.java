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
 * Final merged field value at the document level, with references
 * back to contributing sources
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"field_id", "label", "value", "data_type", "confidence", "collect_mode", "source_refs", "revisions"
})
public class DocumentField
{
	@JsonProperty("field_id")
	private String fieldId;

	@JsonProperty("label")
	private String label;

	@JsonProperty("value")
	@JsonInclude(JsonInclude.Include.ALWAYS)
	private Object value;

	@JsonProperty("data_type")
	private String dataType;

	@JsonProperty("confidence")
	private Double confidence;

	@JsonProperty("collect_mode")
	private CollectMode collectMode;

	@JsonProperty("source_refs")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<SourceRef> sourceRefs;

	@JsonProperty("revisions")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<Revision> revisions;

	public DocumentField()
	{
		this.sourceRefs = new ArrayList<>();
		this.revisions = new ArrayList<>();
	}

	public String getFieldId()
	{
		return fieldId;
	}

	public void setFieldId(String fieldId)
	{
		this.fieldId = fieldId;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}

	public String getDataType()
	{
		return dataType;
	}

	public void setDataType(String dataType)
	{
		this.dataType = dataType;
	}

	public Double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(Double confidence)
	{
		this.confidence = confidence;
	}

	public CollectMode getCollectMode()
	{
		return collectMode;
	}

	public void setCollectMode(CollectMode collectMode)
	{
		this.collectMode = collectMode;
	}

	public List<SourceRef> getSourceRefs()
	{
		return sourceRefs;
	}

	public void setSourceRefs(List<SourceRef> sourceRefs)
	{
		this.sourceRefs = sourceRefs;
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
