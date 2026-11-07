/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import org.cassandragargoyle.portunix.vimparser.model.extraction.Provenance;

import java.util.ArrayList;
import java.util.List;

/**
 * A term that provisions use, defined in the definition file or in a
 * referenced document (references)
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"term_id", "term", "definition", "clause", "references", "anchors", "confidence", "provenance"
})
public class Term
{
	@JsonProperty("term_id")
	private String termId;

	@JsonProperty("term")
	private String term;

	@JsonProperty("definition")
	private String definition;

	@JsonProperty("clause")
	private String clause;

	// reference_id values of the layer that point to the document that defines the term
	@JsonProperty("references")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<String> references;

	@JsonProperty("anchors")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Anchor> anchors;

	@JsonProperty("confidence")
	private Double confidence;

	@JsonProperty("provenance")
	private Provenance provenance;

	public Term()
	{
		this.references = new ArrayList<>();
		this.anchors = new ArrayList<>();
	}

	public String getTermId()
	{
		return termId;
	}

	public void setTermId(String termId)
	{
		this.termId = termId;
	}

	public String getTerm()
	{
		return term;
	}

	public void setTerm(String term)
	{
		this.term = term;
	}

	public String getDefinition()
	{
		return definition;
	}

	public void setDefinition(String definition)
	{
		this.definition = definition;
	}

	public String getClause()
	{
		return clause;
	}

	public void setClause(String clause)
	{
		this.clause = clause;
	}

	public List<String> getReferences()
	{
		return references;
	}

	public void setReferences(List<String> references)
	{
		this.references = references;
	}

	public List<Anchor> getAnchors()
	{
		return anchors;
	}

	public void setAnchors(List<Anchor> anchors)
	{
		this.anchors = anchors;
	}

	public Double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(Double confidence)
	{
		this.confidence = confidence;
	}

	public Provenance getProvenance()
	{
		return provenance;
	}

	public void setProvenance(Provenance provenance)
	{
		this.provenance = provenance;
	}
}
