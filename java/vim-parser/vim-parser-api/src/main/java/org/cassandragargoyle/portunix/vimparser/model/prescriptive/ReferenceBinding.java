/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Binding force of a reference
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public enum ReferenceBinding
{
	MANDATORY("mandatory"),
	INFORMATIVE("informative");

	private final String jsonValue;

	ReferenceBinding(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static ReferenceBinding fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (ReferenceBinding item : values())
		{
			if (item.jsonValue.equalsIgnoreCase(value))
			{
				return item;
			}
		}
		throw new IllegalArgumentException("Unknown ReferenceBinding: " + value);
	}
}
