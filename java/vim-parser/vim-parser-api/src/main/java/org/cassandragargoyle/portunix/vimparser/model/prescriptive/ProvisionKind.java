/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kind of a provision, following its verbal form
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public enum ProvisionKind
{
	REQUIREMENT("requirement"),
	RECOMMENDATION("recommendation"),
	PERMISSION("permission");

	private final String jsonValue;

	ProvisionKind(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static ProvisionKind fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (ProvisionKind item : values())
		{
			if (item.jsonValue.equalsIgnoreCase(value))
			{
				return item;
			}
		}
		throw new IllegalArgumentException("Unknown ProvisionKind: " + value);
	}
}
