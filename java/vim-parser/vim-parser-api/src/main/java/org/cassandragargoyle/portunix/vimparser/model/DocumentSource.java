/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes a single input source contributing to a VIM v2.0 document
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"file", "page", "type", "format", "dimensions", "size_bytes", "sha256"
})
public class DocumentSource
{
	@JsonProperty("file")
	private String file;

	@JsonProperty("page")
	private Integer page;

	@JsonProperty("type")
	private String type;

	@JsonProperty("format")
	private String format;

	@JsonProperty("dimensions")
	private Dimensions dimensions;

	@JsonProperty("size_bytes")
	private Long sizeBytes;

	@JsonProperty("sha256")
	private String sha256;

	public DocumentSource()
	{
	}

	public String getFile()
	{
		return file;
	}

	public void setFile(String file)
	{
		this.file = file;
	}

	public Integer getPage()
	{
		return page;
	}

	public void setPage(Integer page)
	{
		this.page = page;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

	public Dimensions getDimensions()
	{
		return dimensions;
	}

	public void setDimensions(Dimensions dimensions)
	{
		this.dimensions = dimensions;
	}

	public Long getSizeBytes()
	{
		return sizeBytes;
	}

	public void setSizeBytes(Long sizeBytes)
	{
		this.sizeBytes = sizeBytes;
	}

	public String getSha256()
	{
		return sha256;
	}

	public void setSha256(String sha256)
	{
		this.sha256 = sha256;
	}

	/**
	 * Pixel dimensions of an image-based source
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonPropertyOrder({
		"width", "height"
	})
	public static class Dimensions
	{
		@JsonProperty("width")
		private Integer width;

		@JsonProperty("height")
		private Integer height;

		public Dimensions()
		{
		}

		public Dimensions(Integer width, Integer height)
		{
			this.width = width;
			this.height = height;
		}

		public Integer getWidth()
		{
			return width;
		}

		public void setWidth(Integer width)
		{
			this.width = width;
		}

		public Integer getHeight()
		{
			return height;
		}

		public void setHeight(Integer height)
		{
			this.height = height;
		}
	}
}
