/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of the element that an anchor points to
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public enum AnchorElementType
{
	BLOCK("block"),
	TABLE("table"),
	GRAPHIC("graphic");

	private final String jsonValue;

	AnchorElementType(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static AnchorElementType fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (AnchorElementType item : values())
		{
			if (item.jsonValue.equalsIgnoreCase(value))
			{
				return item;
			}
		}
		throw new IllegalArgumentException("Unknown AnchorElementType: " + value);
	}
}
