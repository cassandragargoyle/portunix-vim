/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a text block - a spatially grouped collection of text elements
 *
 * @author Zdenek
 * @since 2026-01-30
 */
@JsonPropertyOrder({"id", "bbox", "page", "text_elements", "header", "content", "type", "significance", "confidence"})
public class TextBlock
{
	@JsonProperty("id")
	private String id;

	@JsonProperty("bbox")
	private BoundingBox bbox;

	@JsonProperty("page")
	private int page;

	@JsonProperty("text_elements")
	private List<TextElement> textElements;

	@JsonProperty("header")
	private String header;

	@JsonProperty("content")
	private List<String> content;

	@JsonProperty("type")
	private String type;

	@JsonProperty("significance")
	private String significance;

	@JsonProperty("confidence")
	private double confidence;

	public TextBlock()
	{
		this.textElements = new ArrayList<>();
		this.content = new ArrayList<>();
		this.confidence = 1.0;
	}

	public TextBlock(String id, int page)
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

	public List<TextElement> getTextElements()
	{
		return textElements;
	}

	public void setTextElements(List<TextElement> textElements)
	{
		this.textElements = textElements;
	}

	public String getHeader()
	{
		return header;
	}

	public void setHeader(String header)
	{
		this.header = header;
	}

	public List<String> getContent()
	{
		return content;
	}

	public void setContent(List<String> content)
	{
		this.content = content;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getSignificance()
	{
		return significance;
	}

	public void setSignificance(String significance)
	{
		this.significance = significance;
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
	 * Add a text element to this block and update the bounding box
	 */
	public void addTextElement(TextElement element)
	{
		textElements.add(element);
		if (bbox == null)
		{
			bbox = new BoundingBox(
				element.getBbox().getX(),
				element.getBbox().getY(),
				element.getBbox().getWidth(),
				element.getBbox().getHeight()
			);
		}
		else
		{
			bbox.expandToInclude(element.getBbox());
		}
	}

	/**
	 * Get the full text content of this block as a single string
	 */
	@JsonIgnore
	public String getFullText()
	{
		StringBuilder sb = new StringBuilder();
		for (TextElement element : textElements)
		{
			if (sb.length() > 0)
			{
				sb.append(" ");
			}
			sb.append(element.getText());
		}
		return sb.toString();
	}

	/**
	 * Extract header and content from text elements based on font analysis
	 */
	public void analyzeStructure(float baseFontSize)
	{
		if (textElements.isEmpty())
		{
			return;
		}

		// First element that looks like a header becomes the header
		for (int i = 0; i < textElements.size(); i++)
		{
			TextElement element = textElements.get(i);
			if (element.isLikelyHeader(baseFontSize) && header == null)
			{
				header = element.getText().trim();
				type = "labeled_content";
			}
			else
			{
				String text = element.getText().trim();
				if (!text.isEmpty())
				{
					content.add(text);
				}
			}
		}

		// If no header found, use first line as header if it's short
		if (header == null && !textElements.isEmpty())
		{
			String firstText = textElements.get(0).getText().trim();
			if (firstText.length() < 30 && !firstText.contains(" "))
			{
				header = firstText;
				type = "labeled_content";
				if (!content.isEmpty() && content.get(0).equals(firstText))
				{
					content.remove(0);
				}
			}
			else
			{
				type = "content";
			}
		}
	}

	@Override
	public String toString()
	{
		return String.format("TextBlock[id=%s, page=%d, header='%s', elements=%d]", id, page, header, textElements.size());
	}
}
