/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a final field value is aggregated from multiple source extractions
 *
 * @author Zdenek
 * @since 2026-05-19
 */
public enum CollectMode
{
	SINGLE("single"),
	FIRST("first"),
	LAST("last"),
	ALL("all"),
	CONCAT("concat"),
	SUM("sum"),
	MAJORITY("majority"),
	HIGHEST_CONFIDENCE("highest_confidence");

	private final String jsonValue;

	CollectMode(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static CollectMode fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (CollectMode mode : values())
		{
			if (mode.jsonValue.equalsIgnoreCase(value))
			{
				return mode;
			}
		}
		throw new IllegalArgumentException("Unknown CollectMode: " + value);
	}
}
