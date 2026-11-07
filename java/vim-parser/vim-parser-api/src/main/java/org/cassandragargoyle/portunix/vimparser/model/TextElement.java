/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a single text element extracted from a PDF
 *
 * @author Zdenek
 * @since 2026-01-30
 */
@JsonPropertyOrder({"text", "bbox", "page", "font_size", "font_name", "confidence", "drop_cap_merged", "drop_cap_bbox"})
public class TextElement
{
	@JsonProperty("text")
	private String text;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	@JsonProperty("page")
	private int page;

	@JsonProperty("font_size")
	private float fontSize;

	@JsonProperty("font_name")
	private String fontName;

	@JsonProperty("confidence")
	private double confidence;

	@JsonProperty("drop_cap_merged")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private boolean dropCapMerged;

	@JsonProperty("drop_cap_bbox")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private BoundingBox dropCapBbox;

	public TextElement()
	{
		this.confidence = 1.0;
	}

	public TextElement(String text, BoundingBox bbox, int page, float fontSize, String fontName)
	{
		this();
		this.text = text;
		this.bbox = bbox;
		this.page = page;
		this.fontSize = fontSize;
		this.fontName = fontName;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public BoundingBox getBbox()
	{
		return bbox;
	}

	public void setBbox(BoundingBox bbox)
	{
		this.bbox = bbox;
	}

	public int getPage()
	{
		return page;
	}

	public void setPage(int page)
	{
		this.page = page;
	}

	public float getFontSize()
	{
		return fontSize;
	}

	public void setFontSize(float fontSize)
	{
		this.fontSize = fontSize;
	}

	public String getFontName()
	{
		return fontName;
	}

	public void setFontName(String fontName)
	{
		this.fontName = fontName;
	}

	public double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(double confidence)
	{
		this.confidence = confidence;
	}

	public boolean isDropCapMerged()
	{
		return dropCapMerged;
	}

	public void setDropCapMerged(boolean dropCapMerged)
	{
		this.dropCapMerged = dropCapMerged;
	}

	public BoundingBox getDropCapBbox()
	{
		return dropCapBbox;
	}

	public void setDropCapBbox(BoundingBox dropCapBbox)
	{
		this.dropCapBbox = dropCapBbox;
	}

	/**
	 * Check if this element is likely a header based on font size comparison
	 */
	@JsonIgnore
	public boolean isLikelyHeader(float baseFontSize)
	{
		// Element is likely header if font is significantly larger than base
		return fontSize > baseFontSize * 1.2;
	}

	@Override
	public String toString()
	{
		return String.format("TextElement[text='%s', page=%d, fontSize=%.1f]",
							 text != null && text.length() > 20 ? text.substring(0, 20) + "..." : text,
							 page, fontSize);
	}
}
