/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kind of a link between two provisions
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public enum RelationType
{
	REFINES("refines"),
	DEPENDS_ON("depends_on"),
	EXCEPTION_TO("exception_to"),
	CONFLICTS_WITH("conflicts_with");

	private final String jsonValue;

	RelationType(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static RelationType fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (RelationType item : values())
		{
			if (item.jsonValue.equalsIgnoreCase(value))
			{
				return item;
			}
		}
		throw new IllegalArgumentException("Unknown RelationType: " + value);
	}
}
