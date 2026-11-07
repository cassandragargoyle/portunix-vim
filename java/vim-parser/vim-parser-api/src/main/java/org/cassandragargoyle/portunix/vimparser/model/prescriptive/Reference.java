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
 * A link from the definition file to another document or to another clause
 * of the same file
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"reference_id", "scope", "binding", "target", "anchors", "confidence", "provenance"
})
public class Reference
{
	@JsonProperty("reference_id")
	private String referenceId;

	@JsonProperty("scope")
	private ReferenceScope scope;

	@JsonProperty("binding")
	private ReferenceBinding binding;

	@JsonProperty("target")
	private Target target;

	@JsonProperty("anchors")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Anchor> anchors;

	@JsonProperty("confidence")
	private Double confidence;

	@JsonProperty("provenance")
	private Provenance provenance;

	public Reference()
	{
		this.anchors = new ArrayList<>();
	}

	public String getReferenceId()
	{
		return referenceId;
	}

	public void setReferenceId(String referenceId)
	{
		this.referenceId = referenceId;
	}

	public ReferenceScope getScope()
	{
		return scope;
	}

	public void setScope(ReferenceScope scope)
	{
		this.scope = scope;
	}

	public ReferenceBinding getBinding()
	{
		return binding;
	}

	public void setBinding(ReferenceBinding binding)
	{
		this.binding = binding;
	}

	public Target getTarget()
	{
		return target;
	}

	public void setTarget(Target target)
	{
		this.target = target;
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
