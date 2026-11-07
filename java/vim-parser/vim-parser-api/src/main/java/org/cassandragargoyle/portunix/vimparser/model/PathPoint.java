/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a point in a PDF path with its type (move, line, curve control point, etc.)
 *
 * @author Zdenek
 * @since 2026-02-03
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"x", "y", "type"})
public class PathPoint
{
	@JsonProperty("x")
	private float x;

	@JsonProperty("y")
	private float y;

	@JsonProperty("type")
	private String type;

	public PathPoint()
	{
	}

	public PathPoint(float x, float y, String type)
	{
		this.x = x;
		this.y = y;
		this.type = type;
	}

	public float getX()
	{
		return x;
	}

	public void setX(float x)
	{
		this.x = x;
	}

	public float getY()
	{
		return y;
	}

	public void setY(float y)
	{
		this.y = y;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	@Override
	public String toString()
	{
		return String.format("PathPoint[x=%.1f, y=%.1f, type=%s]", x, y, type);
	}
}
