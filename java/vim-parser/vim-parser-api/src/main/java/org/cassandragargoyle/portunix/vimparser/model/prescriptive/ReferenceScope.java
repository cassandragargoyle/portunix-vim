/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Scope of a reference: another document or another clause of the same file
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public enum ReferenceScope
{
	EXTERNAL("external"),
	INTERNAL("internal");

	private final String jsonValue;

	ReferenceScope(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static ReferenceScope fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (ReferenceScope item : values())
		{
			if (item.jsonValue.equalsIgnoreCase(value))
			{
				return item;
			}
		}
		throw new IllegalArgumentException("Unknown ReferenceScope: " + value);
	}
}
