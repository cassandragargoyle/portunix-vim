/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a repeating field group is replicated in a document
 *
 * @author Zdenek
 * @since 2026-05-19
 */
public enum RepeatMode
{
	PER_PAGE("per_page"),
	PER_ITEM("per_item"),
	DYNAMIC("dynamic");

	private final String jsonValue;

	RepeatMode(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static RepeatMode fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (RepeatMode mode : values())
		{
			if (mode.jsonValue.equalsIgnoreCase(value))
			{
				return mode;
			}
		}
		throw new IllegalArgumentException("Unknown RepeatMode: " + value);
	}
}
