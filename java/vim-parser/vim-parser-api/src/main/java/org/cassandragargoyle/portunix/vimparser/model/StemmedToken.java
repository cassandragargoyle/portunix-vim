/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a stemmed token with original surface form and stem
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "surface", "stem", "offset", "page", "blockId"})
public class StemmedToken
{
	@JsonProperty("id")
	private String id;

	@JsonProperty("surface")
	private String surface;

	@JsonProperty("stem")
	private String stem;

	@JsonProperty("offset")
	private TokenOffset offset;

	@JsonProperty("page")
	private Integer page;

	@JsonProperty("blockId")
	private String blockId;

	public StemmedToken()
	{
	}

	public StemmedToken(String id, String surface, String stem, int startOffset, int endOffset)
	{
		this.id = id;
		this.surface = surface;
		this.stem = stem;
		this.offset = new TokenOffset(startOffset, endOffset);
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getSurface()
	{
		return surface;
	}

	public void setSurface(String surface)
	{
		this.surface = surface;
	}

	public String getStem()
	{
		return stem;
	}

	public void setStem(String stem)
	{
		this.stem = stem;
	}

	public TokenOffset getOffset()
	{
		return offset;
	}

	public void setOffset(TokenOffset offset)
	{
		this.offset = offset;
	}

	public Integer getPage()
	{
		return page;
	}

	public void setPage(Integer page)
	{
		this.page = page;
	}

	public String getBlockId()
	{
		return blockId;
	}

	public void setBlockId(String blockId)
	{
		this.blockId = blockId;
	}

	@Override
	public String toString()
	{
		return String.format("StemmedToken[%s -> %s]", surface, stem);
	}

	/**
	 * Token offset in source text
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonPropertyOrder({"start", "end"})
	public static class TokenOffset
	{
		@JsonProperty("start")
		private int start;

		@JsonProperty("end")
		private int end;

		public TokenOffset()
		{
		}

		public TokenOffset(int start, int end)
		{
			this.start = start;
			this.end = end;
		}

		public int getStart()
		{
			return start;
		}

		public void setStart(int start)
		{
			this.start = start;
		}

		public int getEnd()
		{
			return end;
		}

		public void setEnd(int end)
		{
			this.end = end;
		}
	}
}
