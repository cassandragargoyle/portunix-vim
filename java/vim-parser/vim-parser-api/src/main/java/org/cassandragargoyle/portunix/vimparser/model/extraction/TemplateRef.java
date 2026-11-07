/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Reference to the template that drove an extraction
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"template_id", "template_version", "template_source"
})
public class TemplateRef
{
	@JsonProperty("template_id")
	private String templateId;

	@JsonProperty("template_version")
	private String templateVersion;

	@JsonProperty("template_source")
	private String templateSource;

	public TemplateRef()
	{
	}

	public TemplateRef(String templateId, String templateVersion, String templateSource)
	{
		this.templateId = templateId;
		this.templateVersion = templateVersion;
		this.templateSource = templateSource;
	}

	public String getTemplateId()
	{
		return templateId;
	}

	public void setTemplateId(String templateId)
	{
		this.templateId = templateId;
	}

	public String getTemplateVersion()
	{
		return templateVersion;
	}

	public void setTemplateVersion(String templateVersion)
	{
		this.templateVersion = templateVersion;
	}

	public String getTemplateSource()
	{
		return templateSource;
	}

	public void setTemplateSource(String templateSource)
	{
		this.templateSource = templateSource;
	}
}
