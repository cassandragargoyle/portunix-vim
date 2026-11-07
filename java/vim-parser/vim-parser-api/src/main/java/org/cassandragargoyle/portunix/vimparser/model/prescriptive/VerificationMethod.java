/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Way a provision can be checked
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public enum VerificationMethod
{
	AUTOMATIC("automatic"),
	TEST("test"),
	MANUAL("manual");

	private final String jsonValue;

	VerificationMethod(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static VerificationMethod fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (VerificationMethod item : values())
		{
			if (item.jsonValue.equalsIgnoreCase(value))
			{
				return item;
			}
		}
		throw new IllegalArgumentException("Unknown VerificationMethod: " + value);
	}
}
