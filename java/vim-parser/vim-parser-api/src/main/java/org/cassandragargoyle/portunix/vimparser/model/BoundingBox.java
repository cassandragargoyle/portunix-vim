/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a bounding box with coordinates in PDF coordinate system
 *
 * @author Zdenek
 * @since 2026-01-30
 */
@JsonPropertyOrder({"right", "bottom", "x", "y", "width", "height"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoundingBox
{
	@JsonProperty("x")
	private float x;

	@JsonProperty("y")
	private float y;

	@JsonProperty("width")
	private float width;

	@JsonProperty("height")
	private float height;

	public BoundingBox()
	{
	}

	public BoundingBox(float x, float y, float width, float height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
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

	public float getWidth()
	{
		return width;
	}

	public void setWidth(float width)
	{
		this.width = width;
	}

	public float getHeight()
	{
		return height;
	}

	public void setHeight(float height)
	{
		this.height = height;
	}

	public float getRight()
	{
		return x + width;
	}

	public float getBottom()
	{
		return y + height;
	}

	/**
	 * Check if this bounding box horizontally overlaps with another
	 */
	public boolean horizontallyOverlaps(BoundingBox other)
	{
		return !(this.getRight() < other.getX() || other.getRight() < this.getX());
	}

	/**
	 * Check if this bounding box is in the same column as another (with tolerance)
	 */
	public boolean isInSameColumn(BoundingBox other, float tolerance)
	{
		float thisCenter = this.x + this.width / 2;
		float otherCenter = other.x + other.width / 2;
		return Math.abs(thisCenter - otherCenter) < tolerance;
	}

	/**
	 * Expand this bounding box to include another
	 */
	public void expandToInclude(BoundingBox other)
	{
		float newX = Math.min(this.x, other.x);
		float newY = Math.min(this.y, other.y);
		float newRight = Math.max(this.getRight(), other.getRight());
		float newBottom = Math.max(this.getBottom(), other.getBottom());

		this.x = newX;
		this.y = newY;
		this.width = newRight - newX;
		this.height = newBottom - newY;
	}

	@Override
	public String toString()
	{
		return String.format("BoundingBox[x=%.1f, y=%.1f, w=%.1f, h=%.1f]", x, y, width, height);
	}
}
