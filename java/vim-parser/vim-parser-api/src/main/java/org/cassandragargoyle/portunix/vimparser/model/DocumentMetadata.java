/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Document metadata following Schema.org vocabulary
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"encodingFormat", "contentSize", "dateCreated", "dateModified", "extraction_date",
	"sha256", "inLanguage", "creator", "producer", "title", "additionalProperties"
})
public class DocumentMetadata
{
	@JsonProperty("encodingFormat")
	private String encodingFormat;

	@JsonProperty("contentSize")
	private Long contentSize;

	@JsonProperty("dateCreated")
	private String dateCreated;

	@JsonProperty("dateModified")
	private String dateModified;

	@JsonProperty("extraction_date")
	private String extractionDate;

	@JsonProperty("sha256")
	private String sha256;

	@JsonProperty("inLanguage")
	private String inLanguage;

	@JsonProperty("creator")
	private String creator;

	@JsonProperty("producer")
	private String producer;

	@JsonProperty("title")
	private String title;

	@JsonProperty("additionalProperties")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String, String> additionalProperties;

	public DocumentMetadata()
	{
		this.additionalProperties = new LinkedHashMap<>();
	}

	public String getEncodingFormat()
	{
		return encodingFormat;
	}

	public void setEncodingFormat(String encodingFormat)
	{
		this.encodingFormat = encodingFormat;
	}

	public Long getContentSize()
	{
		return contentSize;
	}

	public void setContentSize(Long contentSize)
	{
		this.contentSize = contentSize;
	}

	public String getDateCreated()
	{
		return dateCreated;
	}

	public void setDateCreated(String dateCreated)
	{
		this.dateCreated = dateCreated;
	}

	public String getDateModified()
	{
		return dateModified;
	}

	public void setDateModified(String dateModified)
	{
		this.dateModified = dateModified;
	}

	public String getExtractionDate()
	{
		return extractionDate;
	}

	public void setExtractionDate(String extractionDate)
	{
		this.extractionDate = extractionDate;
	}

	public String getSha256()
	{
		return sha256;
	}

	public void setSha256(String sha256)
	{
		this.sha256 = sha256;
	}

	public String getInLanguage()
	{
		return inLanguage;
	}

	public void setInLanguage(String inLanguage)
	{
		this.inLanguage = inLanguage;
	}

	public String getCreator()
	{
		return creator;
	}

	public void setCreator(String creator)
	{
		this.creator = creator;
	}

	public String getProducer()
	{
		return producer;
	}

	public void setProducer(String producer)
	{
		this.producer = producer;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public Map<String, String> getAdditionalProperties()
	{
		return additionalProperties;
	}

	public void setAdditionalProperties(Map<String, String> additionalProperties)
	{
		this.additionalProperties = additionalProperties;
	}

	public void addProperty(String name, String value)
	{
		if (value != null && !value.isEmpty())
		{
			this.additionalProperties.put(name, value);
		}
	}
}
