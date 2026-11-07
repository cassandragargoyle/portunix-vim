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
 * Single audit-trail entry describing a value change
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"revision", "property", "old_value", "new_value", "actor", "actor_id", "reason", "comment", "timestamp"
})
public class Revision
{
	@JsonProperty("revision")
	private Integer revision;

	@JsonProperty("property")
	private String property;

	@JsonProperty("old_value")
	@JsonInclude(JsonInclude.Include.ALWAYS)
	private Object oldValue;

	@JsonProperty("new_value")
	@JsonInclude(JsonInclude.Include.ALWAYS)
	private Object newValue;

	@JsonProperty("actor")
	private RevisionActor actor;

	@JsonProperty("actor_id")
	private String actorId;

	@JsonProperty("reason")
	private RevisionReason reason;

	@JsonProperty("comment")
	private String comment;

	@JsonProperty("timestamp")
	private String timestamp;

	public Revision()
	{
	}

	public Integer getRevision()
	{
		return revision;
	}

	public void setRevision(Integer revision)
	{
		this.revision = revision;
	}

	public String getProperty()
	{
		return property;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}

	public Object getOldValue()
	{
		return oldValue;
	}

	public void setOldValue(Object oldValue)
	{
		this.oldValue = oldValue;
	}

	public Object getNewValue()
	{
		return newValue;
	}

	public void setNewValue(Object newValue)
	{
		this.newValue = newValue;
	}

	public RevisionActor getActor()
	{
		return actor;
	}

	public void setActor(RevisionActor actor)
	{
		this.actor = actor;
	}

	public String getActorId()
	{
		return actorId;
	}

	public void setActorId(String actorId)
	{
		this.actorId = actorId;
	}

	public RevisionReason getReason()
	{
		return reason;
	}

	public void setReason(RevisionReason reason)
	{
		this.reason = reason;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(String timestamp)
	{
		this.timestamp = timestamp;
	}
}
