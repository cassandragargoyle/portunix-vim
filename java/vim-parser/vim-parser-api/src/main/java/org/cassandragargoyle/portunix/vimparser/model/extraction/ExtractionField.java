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
 * Single extracted field value from a source, with confidence, alternatives,
 * validation, and audit trail
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"field_id", "label", "value", "raw_value", "data_type", "confidence",
	"bbox", "zone_id", "extraction_method", "alternatives", "validation", "revisions"
})
public class ExtractionField
{
	@JsonProperty("field_id")
	private String fieldId;

	@JsonProperty("label")
	private String label;

	@JsonProperty("value")
	@JsonInclude(JsonInclude.Include.ALWAYS)
	private Object value;

	@JsonProperty("raw_value")
	private Object rawValue;

	@JsonProperty("data_type")
	private String dataType;

	@JsonProperty("confidence")
	private Double confidence;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	@JsonProperty("zone_id")
	private String zoneId;

	@JsonProperty("extraction_method")
	private String extractionMethod;

	@JsonProperty("alternatives")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<Alternative> alternatives;

	@JsonProperty("validation")
	private Validation validation;

	@JsonProperty("revisions")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<Revision> revisions;

	public ExtractionField()
	{
		this.alternatives = new ArrayList<>();
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

	public Object getRawValue()
	{
		return rawValue;
	}

	public void setRawValue(Object rawValue)
	{
		this.rawValue = rawValue;
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

	public BoundingBox getBbox()
	{
		return bbox;
	}

	public void setBbox(BoundingBox bbox)
	{
		this.bbox = bbox;
	}

	public String getZoneId()
	{
		return zoneId;
	}

	public void setZoneId(String zoneId)
	{
		this.zoneId = zoneId;
	}

	public String getExtractionMethod()
	{
		return extractionMethod;
	}

	public void setExtractionMethod(String extractionMethod)
	{
		this.extractionMethod = extractionMethod;
	}

	public List<Alternative> getAlternatives()
	{
		return alternatives;
	}

	public void setAlternatives(List<Alternative> alternatives)
	{
		this.alternatives = alternatives;
	}

	public Validation getValidation()
	{
		return validation;
	}

	public void setValidation(Validation validation)
	{
		this.validation = validation;
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
