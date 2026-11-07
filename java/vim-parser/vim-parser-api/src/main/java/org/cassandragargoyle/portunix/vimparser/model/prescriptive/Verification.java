/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

/**
 * How a provision can be checked
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"method", "subject", "rule"
})
public class Verification
{
	@JsonProperty("method")
	private VerificationMethod method;

	@JsonProperty("subject")
	private String subject;

	@JsonProperty("rule")
	private Map<String, Object> rule;

	public Verification()
	{
	}

	public VerificationMethod getMethod()
	{
		return method;
	}

	public void setMethod(VerificationMethod method)
	{
		this.method = method;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public Map<String, Object> getRule()
	{
		return rule;
	}

	public void setRule(Map<String, Object> rule)
	{
		this.rule = rule;
	}
}
