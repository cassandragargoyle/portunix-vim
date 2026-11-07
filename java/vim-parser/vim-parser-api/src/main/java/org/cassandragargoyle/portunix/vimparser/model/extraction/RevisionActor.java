/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Actor that produced a revision in the audit trail
 *
 * @author Zdenek
 * @since 2026-05-19
 */
public enum RevisionActor
{
	ENGINE("engine"),
	OPERATOR("operator"),
	RULE("rule"),
	LLM("llm");

	private final String jsonValue;

	RevisionActor(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static RevisionActor fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (RevisionActor actor : values())
		{
			if (actor.jsonValue.equalsIgnoreCase(value))
			{
				return actor;
			}
		}
		throw new IllegalArgumentException("Unknown RevisionActor: " + value);
	}
}
