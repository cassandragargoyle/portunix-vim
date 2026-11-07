/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The method used to extract a value from a source
 *
 * @author Zdenek
 * @since 2026-05-19
 */
public enum ExtractionMethod
{
	ZONE_OCR("zone_ocr"),
	ZONE_BARCODE("zone_barcode"),
	ZONE_CHECKBOX("zone_checkbox"),
	TEMPLATE_MATCH("template_match"),
	LLM_EXTRACT("llm_extract"),
	MANUAL("manual"),
	HYBRID("hybrid");

	private final String jsonValue;

	ExtractionMethod(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static ExtractionMethod fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (ExtractionMethod method : values())
		{
			if (method.jsonValue.equalsIgnoreCase(value))
			{
				return method;
			}
		}
		throw new IllegalArgumentException("Unknown ExtractionMethod: " + value);
	}
}
