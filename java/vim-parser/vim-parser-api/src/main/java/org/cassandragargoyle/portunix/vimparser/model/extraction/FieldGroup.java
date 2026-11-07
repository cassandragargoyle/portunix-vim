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
 * Repeating field group, expanded to one or more instances
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"group_id", "label", "repeat_mode", "instances"
})
public class FieldGroup
{
	@JsonProperty("group_id")
	private String groupId;

	@JsonProperty("label")
	private String label;

	@JsonProperty("repeat_mode")
	private RepeatMode repeatMode;

	@JsonProperty("instances")
	private List<GroupInstance> instances;

	public FieldGroup()
	{
		this.instances = new ArrayList<>();
	}

	public String getGroupId()
	{
		return groupId;
	}

	public void setGroupId(String groupId)
	{
		this.groupId = groupId;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public RepeatMode getRepeatMode()
	{
		return repeatMode;
	}

	public void setRepeatMode(RepeatMode repeatMode)
	{
		this.repeatMode = repeatMode;
	}

	public List<GroupInstance> getInstances()
	{
		return instances;
	}

	public void setInstances(List<GroupInstance> instances)
	{
		this.instances = instances;
	}
}
