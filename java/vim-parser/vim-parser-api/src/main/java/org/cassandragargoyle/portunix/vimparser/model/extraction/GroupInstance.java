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
 * Single instance of a repeating group (e.g., one line item, one page)
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"instance_index", "source_id", "fields"
})
public class GroupInstance
{
	@JsonProperty("instance_index")
	private Integer instanceIndex;

	@JsonProperty("source_id")
	private String sourceId;

	@JsonProperty("fields")
	private List<DocumentField> fields;

	public GroupInstance()
	{
		this.fields = new ArrayList<>();
	}

	public Integer getInstanceIndex()
	{
		return instanceIndex;
	}

	public void setInstanceIndex(Integer instanceIndex)
	{
		this.instanceIndex = instanceIndex;
	}

	public String getSourceId()
	{
		return sourceId;
	}

	public void setSourceId(String sourceId)
	{
		this.sourceId = sourceId;
	}

	public List<DocumentField> getFields()
	{
		return fields;
	}

	public void setFields(List<DocumentField> fields)
	{
		this.fields = fields;
	}
}
