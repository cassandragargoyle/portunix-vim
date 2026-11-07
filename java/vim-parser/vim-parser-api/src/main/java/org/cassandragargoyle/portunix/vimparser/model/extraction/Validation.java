/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating an extracted field
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"status", "rules_applied", "messages"
})
public class Validation
{
	@JsonProperty("status")
	private ValidationStatus status;

	@JsonProperty("rules_applied")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<String> rulesApplied;

	@JsonProperty("messages")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<String> messages;

	public Validation()
	{
		this.rulesApplied = new ArrayList<>();
		this.messages = new ArrayList<>();
	}

	public ValidationStatus getStatus()
	{
		return status;
	}

	public void setStatus(ValidationStatus status)
	{
		this.status = status;
	}

	public List<String> getRulesApplied()
	{
		return rulesApplied;
	}

	public void setRulesApplied(List<String> rulesApplied)
	{
		this.rulesApplied = rulesApplied;
	}

	public List<String> getMessages()
	{
		return messages;
	}

	public void setMessages(List<String> messages)
	{
		this.messages = messages;
	}
}
