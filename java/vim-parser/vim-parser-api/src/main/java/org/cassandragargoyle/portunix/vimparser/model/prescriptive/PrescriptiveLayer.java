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
 * Top-level prescriptive layer for VIM v2.1 — what a definition file prescribes:
 * provisions, references to other documents and clauses, and defined terms
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"definition", "provenance", "provisions", "references", "terms"
})
public class PrescriptiveLayer
{
	@JsonProperty("definition")
	private Definition definition;

	@JsonProperty("provenance")
	private Provenance provenance;

	@JsonProperty("provisions")
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Provision> provisions;

	@JsonProperty("references")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Reference> references;

	@JsonProperty("terms")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Term> terms;

	public PrescriptiveLayer()
	{
		this.provisions = new ArrayList<>();
		this.references = new ArrayList<>();
		this.terms = new ArrayList<>();
	}

	public Definition getDefinition()
	{
		return definition;
	}

	public void setDefinition(Definition definition)
	{
		this.definition = definition;
	}

	public Provenance getProvenance()
	{
		return provenance;
	}

	public void setProvenance(Provenance provenance)
	{
		this.provenance = provenance;
	}

	public List<Provision> getProvisions()
	{
		return provisions;
	}

	public void setProvisions(List<Provision> provisions)
	{
		this.provisions = provisions;
	}

	public List<Reference> getReferences()
	{
		return references;
	}

	public void setReferences(List<Reference> references)
	{
		this.references = references;
	}

	public List<Term> getTerms()
	{
		return terms;
	}

	public void setTerms(List<Term> terms)
	{
		this.terms = terms;
	}
}
