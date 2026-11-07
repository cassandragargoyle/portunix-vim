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
 * Top-level extraction layer for VIM v2.0 — template-driven results
 * produced by reco, OCR engines, or other tools
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"template_ref", "sources", "document"
})
public class ExtractionLayer
{
	@JsonProperty("template_ref")
	private TemplateRef templateRef;

	@JsonProperty("sources")
	private List<ExtractionSource> sources;

	@JsonProperty("document")
	private ExtractionDocument document;

	public ExtractionLayer()
	{
		this.sources = new ArrayList<>();
	}

	public TemplateRef getTemplateRef()
	{
		return templateRef;
	}

	public void setTemplateRef(TemplateRef templateRef)
	{
		this.templateRef = templateRef;
	}

	public List<ExtractionSource> getSources()
	{
		return sources;
	}

	public void setSources(List<ExtractionSource> sources)
	{
		this.sources = sources;
	}

	public ExtractionDocument getDocument()
	{
		return document;
	}

	public void setDocument(ExtractionDocument document)
	{
		this.document = document;
	}
}
