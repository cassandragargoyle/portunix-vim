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
 * A link from a provision to another provision of the layer
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"provision_id", "relation"
})
public class Relation
{
	@JsonProperty("provision_id")
	private String provisionId;

	@JsonProperty("relation")
	private RelationType relation;

	public Relation()
	{
	}

	public String getProvisionId()
	{
		return provisionId;
	}

	public void setProvisionId(String provisionId)
	{
		this.provisionId = provisionId;
	}

	public RelationType getRelation()
	{
		return relation;
	}

	public void setRelation(RelationType relation)
	{
		this.relation = relation;
	}
}
