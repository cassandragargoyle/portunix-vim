/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Fulltext content extracted from document
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"content", "wordCount", "characterCount"})
public class FulltextContent
{
	@JsonProperty("content")
	private String content;

	@JsonProperty("wordCount")
	private int wordCount;

	@JsonProperty("characterCount")
	private int characterCount;

	public FulltextContent()
	{
	}

	public FulltextContent(String content)
	{
		this.content = content;
		if (content != null)
		{
			this.characterCount = content.length();
			this.wordCount = countWords(content);
		}
	}

	private int countWords(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return 0;
		}
		// Split on whitespace and count non-empty parts
		String[] words = text.trim().split("\\s+");
		return words.length;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
		if (content != null)
		{
			this.characterCount = content.length();
			this.wordCount = countWords(content);
		}
	}

	public int getWordCount()
	{
		return wordCount;
	}

	public void setWordCount(int wordCount)
	{
		this.wordCount = wordCount;
	}

	public int getCharacterCount()
	{
		return characterCount;
	}

	public void setCharacterCount(int characterCount)
	{
		this.characterCount = characterCount;
	}
}
