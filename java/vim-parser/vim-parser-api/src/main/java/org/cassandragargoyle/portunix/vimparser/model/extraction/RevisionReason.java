/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Reason for a revision entry in the audit trail
 *
 * @author Zdenek
 * @since 2026-05-19
 */
public enum RevisionReason
{
	MANUAL_CORRECTION("manual_correction"),
	CHARACTER_REPLACEMENT("character_replacement"),
	NORMALIZATION("normalization"),
	REEXTRACTION("reextraction"),
	TYPE_COERCION("type_coercion"),
	VALIDATION_FIX("validation_fix"),
	LLM_SUGGESTION("llm_suggestion"),
	MERGE_RESOLUTION("merge_resolution"),
	OTHER("other");

	private final String jsonValue;

	RevisionReason(String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String getJsonValue()
	{
		return jsonValue;
	}

	@JsonCreator
	public static RevisionReason fromJson(String value)
	{
		if (value == null)
		{
			return null;
		}
		for (RevisionReason reason : values())
		{
			if (reason.jsonValue.equalsIgnoreCase(value))
			{
				return reason;
			}
		}
		throw new IllegalArgumentException("Unknown RevisionReason: " + value);
	}
}
