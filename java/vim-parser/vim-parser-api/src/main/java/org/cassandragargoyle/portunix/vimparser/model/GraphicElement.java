/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Represents a graphic element extracted from PDF content stream.
 * Replaces PhysicalImage with support for images, paths, lines, rectangles, and shadings.
 *
 * @author Zdenek
 * @since 2026-02-03
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
	"id", "page", "bbox", "element_type",
	"format", "width_px", "height_px", "color_space", "bits_per_component", "image_type",
	"points", "stroke_width", "filled", "stroked", "winding_rule",
	"shading_name"
})
public class GraphicElement
{
	// Common fields
	@JsonProperty("id")
	private String id;

	@JsonProperty("page")
	private int page;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	@JsonProperty("element_type")
	private GraphicElementType elementType;

	// Image-specific fields
	@JsonProperty("format")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String format;

	@JsonProperty("width_px")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private int widthPx;

	@JsonProperty("height_px")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private int heightPx;

	@JsonProperty("color_space")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String colorSpace;

	@JsonProperty("bits_per_component")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private int bitsPerComponent;

	@JsonProperty("image_type")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String imageType;

	// Path-specific fields
	@JsonProperty("points")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<PathPoint> points;

	@JsonProperty("stroke_width")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private float strokeWidth;

	@JsonProperty("filled")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private boolean filled;

	@JsonProperty("stroked")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private boolean stroked;

	@JsonProperty("winding_rule")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String windingRule;

	// Shading-specific fields
	@JsonProperty("shading_name")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String shadingName;

	public GraphicElement()
	{
	}

	public GraphicElement(String id, int page)
	{
		this.id = id;
		this.page = page;
	}

	// Common getters/setters

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public int getPage()
	{
		return page;
	}

	public void setPage(int page)
	{
		this.page = page;
	}

	public BoundingBox getBbox()
	{
		return bbox;
	}

	public void setBbox(BoundingBox bbox)
	{
		this.bbox = bbox;
	}

	public GraphicElementType getElementType()
	{
		return elementType;
	}

	public void setElementType(GraphicElementType elementType)
	{
		this.elementType = elementType;
	}

	// Image-specific getters/setters

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

	public int getWidthPx()
	{
		return widthPx;
	}

	public void setWidthPx(int widthPx)
	{
		this.widthPx = widthPx;
	}

	public int getHeightPx()
	{
		return heightPx;
	}

	public void setHeightPx(int heightPx)
	{
		this.heightPx = heightPx;
	}

	public String getColorSpace()
	{
		return colorSpace;
	}

	public void setColorSpace(String colorSpace)
	{
		this.colorSpace = colorSpace;
	}

	public int getBitsPerComponent()
	{
		return bitsPerComponent;
	}

	public void setBitsPerComponent(int bitsPerComponent)
	{
		this.bitsPerComponent = bitsPerComponent;
	}

	public String getImageType()
	{
		return imageType;
	}

	public void setImageType(String imageType)
	{
		this.imageType = imageType;
	}

	// Path-specific getters/setters

	public List<PathPoint> getPoints()
	{
		return points;
	}

	public void setPoints(List<PathPoint> points)
	{
		this.points = points;
	}

	public float getStrokeWidth()
	{
		return strokeWidth;
	}

	public void setStrokeWidth(float strokeWidth)
	{
		this.strokeWidth = strokeWidth;
	}

	public boolean isFilled()
	{
		return filled;
	}

	public void setFilled(boolean filled)
	{
		this.filled = filled;
	}

	public boolean isStroked()
	{
		return stroked;
	}

	public void setStroked(boolean stroked)
	{
		this.stroked = stroked;
	}

	public String getWindingRule()
	{
		return windingRule;
	}

	public void setWindingRule(String windingRule)
	{
		this.windingRule = windingRule;
	}

	// Shading-specific getters/setters

	public String getShadingName()
	{
		return shadingName;
	}

	public void setShadingName(String shadingName)
	{
		this.shadingName = shadingName;
	}

	@Override
	public String toString()
	{
		return String.format("GraphicElement[id=%s, page=%d, type=%s]",
			id, page, elementType);
	}
}
