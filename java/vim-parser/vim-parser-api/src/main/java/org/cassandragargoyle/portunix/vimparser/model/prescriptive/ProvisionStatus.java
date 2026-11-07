/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Quality mark of a provision, not the state of a review process
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public enum ProvisionStatus
{
	PROPOSED("proposed"),
	CONFIRMED("confirmed"),
	REJECTED("rejected");

	private final String jsonValue;

	ProvisionStatus(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static ProvisionStatus fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (ProvisionStatus item : values())
		{
			if (item.jsonValue.equalsIgnoreCase(value))
			{
				return item;
			}
		}
		throw new IllegalArgumentException("Unknown ProvisionStatus: " + value);
	}
}
