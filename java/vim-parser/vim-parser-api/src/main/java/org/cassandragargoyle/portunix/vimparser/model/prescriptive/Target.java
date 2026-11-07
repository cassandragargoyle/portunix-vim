/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Target of a reference
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"identifier", "edition", "title", "clause", "uri"
})
public class Target
{
	@JsonProperty("identifier")
	private String identifier;

	@JsonProperty("edition")
	private String edition;

	@JsonProperty("title")
	private String title;

	@JsonProperty("clause")
	private String clause;

	@JsonProperty("uri")
	private String uri;

	public Target()
	{
	}

	public String getIdentifier()
	{
		return identifier;
	}

	public void setIdentifier(String identifier)
	{
		this.identifier = identifier;
	}

	public String getEdition()
	{
		return edition;
	}

	public void setEdition(String edition)
	{
		this.edition = edition;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getClause()
	{
		return clause;
	}

	public void setClause(String clause)
	{
		this.clause = clause;
	}

	public String getUri()
	{
		return uri;
	}

	public void setUri(String uri)
	{
		this.uri = uri;
	}
}
