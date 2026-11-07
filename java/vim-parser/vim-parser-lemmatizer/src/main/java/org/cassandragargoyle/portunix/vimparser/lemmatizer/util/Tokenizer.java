/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple text tokenizer for splitting text into words
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class Tokenizer
{
	// Pattern matching word characters including Czech diacritics
	private static final Pattern WORD_PATTERN = Pattern.compile(
		"[\\p{L}\\p{M}]+",
		Pattern.UNICODE_CHARACTER_CLASS
	);

	private Tokenizer()
	{
		// Utility class
	}

	/**
	 * Token with position information
	 */
	public static class Token
	{
		private final String text;
		private final int startOffset;
		private final int endOffset;

		public Token(String text, int startOffset, int endOffset)
		{
			this.text = text;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}

		public String getText()
		{
			return text;
		}

		public int getStartOffset()
		{
			return startOffset;
		}

		public int getEndOffset()
		{
			return endOffset;
		}
	}

	/**
	 * Tokenize text into words with position information
	 * @param text input text
	 * @return list of tokens
	 */
	public static List<Token> tokenize(String text)
	{
		List<Token> tokens = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return tokens;
		}

		Matcher matcher = WORD_PATTERN.matcher(text);
		while (matcher.find())
		{
			tokens.add(new Token(matcher.group(), matcher.start(), matcher.end()));
		}

		return tokens;
	}
}
