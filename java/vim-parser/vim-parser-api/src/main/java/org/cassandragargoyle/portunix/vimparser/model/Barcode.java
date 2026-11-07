/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a barcode detected in a PDF document
 *
 * @author Zdenek
 * @since 2026-01-30
 */
@JsonPropertyOrder(
{
	"id", "bbox", "page", "format", "raw_content", "content_type", "confidence"
})
public class Barcode
{
	@JsonProperty("id")
	private String id;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	@JsonProperty("page")
	private int page;

	@JsonProperty("format")
	private String format;

	@JsonProperty("raw_content")
	private String rawContent;

	@JsonProperty("content_type")
	private String contentType;

	@JsonProperty("confidence")
	private double confidence;

	public Barcode()
	{
		this.confidence = 1.0;
	}

	public Barcode(String id, int page)
	{
		this();
		this.id = id;
		this.page = page;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
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

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

	public String getRawContent()
	{
		return rawContent;
	}

	public void setRawContent(String rawContent)
	{
		this.rawContent = rawContent;
	}

	public String getContentType()
	{
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(double confidence)
	{
		this.confidence = confidence;
	}

	/**
	 * Check if this barcode contains SPAYD payment data
	 */
	@JsonIgnore
	public boolean isSpaydPayment()
	{
		return rawContent != null && rawContent.startsWith("SPD*");
	}

	/**
	 * Check if this barcode contains a URL
	 */
	@JsonIgnore
	public boolean isUrl()
	{
		return rawContent != null && (rawContent.startsWith("http://") || rawContent.startsWith("https://"));
	}

	@Override
	public String toString()
	{
		return String.format("Barcode[id=%s, page=%d, format=%s, content='%s']",
							 id, page, format,
							 rawContent != null && rawContent.length() > 50
							 ? rawContent.substring(0, 50) + "..."
							 : rawContent);
	}
}
