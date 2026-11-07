/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Validation result status for an extracted field
 *
 * @author Zdenek
 * @since 2026-05-19
 */
public enum ValidationStatus
{
	VALID("valid"),
	WARNING("warning"),
	INVALID("invalid");

	private final String jsonValue;

	ValidationStatus(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static ValidationStatus fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (ValidationStatus status : values())
		{
			if (status.jsonValue.equalsIgnoreCase(value))
			{
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown ValidationStatus: " + value);
	}
}
