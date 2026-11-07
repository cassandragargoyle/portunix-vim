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
 * Alternative interpretation of an extracted value (e.g., OCR candidate)
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"value", "confidence"
})
public class Alternative
{
	@JsonProperty("value")
	@JsonInclude(JsonInclude.Include.ALWAYS)
	private Object value;

	@JsonProperty("confidence")
	private Double confidence;

	public Alternative()
	{
	}

	public Alternative(Object value, Double confidence)
	{
		this.value = value;
		this.confidence = confidence;
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
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
