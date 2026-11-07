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
import org.cassandragargoyle.portunix.vimparser.model.extraction.Revision;

import java.util.ArrayList;
import java.util.List;

/**
 * One provision of a definition file — a statement that prescribes something,
 * with its place in the file, its verification and its audit trail
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"provision_id", "kind", "clause", "text", "statement", "text_hash", "anchors",
	"condition", "references", "terms", "relations", "verification", "status", "confidence",
	"provenance", "revisions"
})
public class Provision
{
	@JsonProperty("provision_id")
	private String provisionId;

	@JsonProperty("kind")
	private ProvisionKind kind;

	@JsonProperty("clause")
	private String clause;

	@JsonProperty("text")
	private String text;

	@JsonProperty("statement")
	private String statement;

	@JsonProperty("text_hash")
	private String textHash;

	@JsonProperty("anchors")
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Anchor> anchors;

	@JsonProperty("condition")
	private String condition;

	@JsonProperty("references")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<String> references;

	@JsonProperty("terms")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<String> terms;

	@JsonProperty("relations")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Relation> relations;

	@JsonProperty("verification")
	private Verification verification;

	@JsonProperty("status")
	private ProvisionStatus status;

	@JsonProperty("confidence")
	private Double confidence;

	@JsonProperty("provenance")
	private Provenance provenance;

	@JsonProperty("revisions")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Revision> revisions;

	public Provision()
	{
		this.anchors = new ArrayList<>();
		this.references = new ArrayList<>();
		this.terms = new ArrayList<>();
		this.relations = new ArrayList<>();
		this.revisions = new ArrayList<>();
	}

	public String getProvisionId()
	{
		return provisionId;
	}

	public void setProvisionId(String provisionId)
	{
		this.provisionId = provisionId;
	}

	public ProvisionKind getKind()
	{
		return kind;
	}

	public void setKind(ProvisionKind kind)
	{
		this.kind = kind;
	}

	public String getClause()
	{
		return clause;
	}

	public void setClause(String clause)
	{
		this.clause = clause;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getStatement()
	{
		return statement;
	}

	public void setStatement(String statement)
	{
		this.statement = statement;
	}

	public String getTextHash()
	{
		return textHash;
	}

	public void setTextHash(String textHash)
	{
		this.textHash = textHash;
	}

	public List<Anchor> getAnchors()
	{
		return anchors;
	}

	public void setAnchors(List<Anchor> anchors)
	{
		this.anchors = anchors;
	}

	public String getCondition()
	{
		return condition;
	}

	public void setCondition(String condition)
	{
		this.condition = condition;
	}

	public List<String> getReferences()
	{
		return references;
	}

	public void setReferences(List<String> references)
	{
		this.references = references;
	}

	public List<String> getTerms()
	{
		return terms;
	}

	public void setTerms(List<String> terms)
	{
		this.terms = terms;
	}

	public List<Relation> getRelations()
	{
		return relations;
	}

	public void setRelations(List<Relation> relations)
	{
		this.relations = relations;
	}

	public Verification getVerification()
	{
		return verification;
	}

	public void setVerification(Verification verification)
	{
		this.verification = verification;
	}

	public ProvisionStatus getStatus()
	{
		return status;
	}

	public void setStatus(ProvisionStatus status)
	{
		this.status = status;
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

	public List<Revision> getRevisions()
	{
		return revisions;
	}

	public void setRevisions(List<Revision> revisions)
	{
		this.revisions = revisions;
	}
}
