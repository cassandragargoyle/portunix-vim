/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;

/**
 * A place in the definition file
 *
 * @author Zdenek
 * @since 2026-10-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"page", "element_type", "element_id", "bbox"
})
public class Anchor
{
	@JsonProperty("page")
	private Integer page;

	@JsonProperty("element_type")
	private AnchorElementType elementType;

	@JsonProperty("element_id")
	private String elementId;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	public Anchor()
	{
	}

	public Integer getPage()
	{
		return page;
	}

	public void setPage(Integer page)
	{
		this.page = page;
	}

	public AnchorElementType getElementType()
	{
		return elementType;
	}

	public void setElementType(AnchorElementType elementType)
	{
		this.elementType = elementType;
	}

	public String getElementId()
	{
		return elementId;
	}

	public void setElementId(String elementId)
	{
		this.elementId = elementId;
	}

	public BoundingBox getBbox()
	{
		return bbox;
	}

	public void setBbox(BoundingBox bbox)
	{
		this.bbox = bbox;
	}
}
