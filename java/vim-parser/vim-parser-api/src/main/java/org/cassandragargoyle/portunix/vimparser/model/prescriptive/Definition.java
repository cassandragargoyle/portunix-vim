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
 * Identity of the definition file
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"identifier", "edition", "issued", "title", "publisher", "language"
})
public class Definition
{
	@JsonProperty("identifier")
	private String identifier;

	@JsonProperty("edition")
	private String edition;

	// ISO 8601 date of issue: a year (2008), a year and month (2008-11), or a full date (2008-11-01)
	@JsonProperty("issued")
	private String issued;

	@JsonProperty("title")
	private String title;

	@JsonProperty("publisher")
	private String publisher;

	// BCP 47 tag (cs, en) of the verbatim text in the layer; a statement can be in another language
	@JsonProperty("language")
	private String language;

	public Definition()
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

	public String getIssued()
	{
		return issued;
	}

	public void setIssued(String issued)
	{
		this.issued = issued;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getPublisher()
	{
		return publisher;
	}

	public void setPublisher(String publisher)
	{
		this.publisher = publisher;
	}

	public String getLanguage()
	{
		return language;
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}
}
