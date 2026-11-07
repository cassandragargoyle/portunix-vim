/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.toc;

import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.cassandragargoyle.portunix.vimparser.model.TocEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects explicit Table of Contents pages in documents and parses their entries.
 * Looks for pages with TOC keywords (e.g., "Obsah", "Contents") and extracts
 * chapter titles with their page numbers.
 *
 * @author Zdenek
 * @since 2026-03-07
 */
public class TocPageDetector
{
	// Keywords that indicate a TOC page header (case-insensitive)
	static final Set<String> TOC_KEYWORDS = Set.of(
			"obsah", "table of contents", "contents", "inhalt", "sommaire",
			"inhaltsverzeichnis", "índice", "indice"
	);

	// Pattern: title followed by dots/spaces and a page number
	// e.g., "Úvod.........5" or "I. ..................6" or "Chapter 1 ... 42"
	static final Pattern TOC_LINE_PATTERN = Pattern.compile(
			"^(.+?)[.\\s·…]{3,}(\\d{1,4})\\s*$"
	);

	// Pattern for simple "Title  123" format (at least 2 spaces before number)
	static final Pattern TOC_LINE_SIMPLE = Pattern.compile(
			"^(.+?)\\s{2,}(\\d{1,4})\\s*$"
	);

	// Minimum number of TOC-like entries on a page to consider it a TOC page
	private static final int MIN_TOC_ENTRIES = 3;

	/**
	 * Result of TOC page detection
	 */
	public static class TocPageResult
	{
		private final int tocPage;
		private final List<TocEntry> entries;
		private final List<String> tocBlockIds;

		public TocPageResult(int tocPage, List<TocEntry> entries, List<String> tocBlockIds)
		{
			this.tocPage = tocPage;
			this.entries = entries;
			this.tocBlockIds = tocBlockIds;
		}

		public int getTocPage()
		{
			return tocPage;
		}

		public List<TocEntry> getEntries()
		{
			return entries;
		}

		public List<String> getTocBlockIds()
		{
			return tocBlockIds;
		}
	}

	/**
	 * Detect explicit TOC pages in the document and parse their entries
	 * @param blocks all text blocks in the document
	 * @return result with parsed entries, or null if no TOC page found
	 */
	public TocPageResult detect(List<TextBlock> blocks)
	{
		if (blocks == null || blocks.isEmpty())
		{
			return null;
		}

		// Group blocks by page
		Map<Integer, List<TextBlock>> blocksByPage = new HashMap<>();
		for (TextBlock block : blocks)
		{
			blocksByPage.computeIfAbsent(block.getPage(), k -> new ArrayList<>()).add(block);
		}

		// Check each page for TOC pattern
		for (Map.Entry<Integer, List<TextBlock>> entry : blocksByPage.entrySet())
		{
			int page = entry.getKey();
			List<TextBlock> pageBlocks = entry.getValue();

			TocPageResult result = evaluatePage(page, pageBlocks);
			if (result != null)
			{
				return result;
			}
		}

		return null;
	}

	/**
	 * Evaluate a single page for TOC content
	 */
	TocPageResult evaluatePage(int page, List<TextBlock> pageBlocks)
	{
		boolean hasTocKeyword = false;
		List<TocEntry> entries = new ArrayList<>();
		List<String> tocBlockIds = new ArrayList<>();

		for (TextBlock block : pageBlocks)
		{
			// Check header for TOC keyword
			if (isTocKeyword(block.getHeader()))
			{
				hasTocKeyword = true;
				tocBlockIds.add(block.getId());
			}

			// Check first text element for TOC keyword (standalone heading)
			if (!hasTocKeyword && !block.getTextElements().isEmpty())
			{
				String firstText = block.getTextElements().get(0).getText().trim();
				if (isTocKeyword(firstText))
				{
					hasTocKeyword = true;
					tocBlockIds.add(block.getId());
				}
			}

			// Try to parse TOC entries from block content
			List<TocEntry> blockEntries = parseBlockEntries(block);
			if (!blockEntries.isEmpty())
			{
				entries.addAll(blockEntries);
				if (!tocBlockIds.contains(block.getId()))
				{
					tocBlockIds.add(block.getId());
				}
			}
		}

		// A valid TOC page needs the keyword AND enough entries
		if (hasTocKeyword && entries.size() >= MIN_TOC_ENTRIES)
		{
			return new TocPageResult(page, entries, tocBlockIds);
		}

		// Even without keyword, a page with many TOC-like entries is likely a TOC
		if (entries.size() >= MIN_TOC_ENTRIES * 2)
		{
			return new TocPageResult(page, entries, tocBlockIds);
		}

		return null;
	}

	/**
	 * Parse TOC entries from a single block's text elements and content
	 */
	List<TocEntry> parseBlockEntries(TextBlock block)
	{
		List<TocEntry> entries = new ArrayList<>();

		// Try parsing from text elements (raw text with dots and page numbers)
		for (TextElement element : block.getTextElements())
		{
			String text = element.getText().trim();
			TocEntry entry = parseTocLine(text, block.getPage(), block.getId());
			if (entry != null)
			{
				entries.add(entry);
			}
		}

		// Also try parsing from content lines
		if (entries.isEmpty() && block.getContent() != null)
		{
			for (String line : block.getContent())
			{
				TocEntry entry = parseTocLine(line.trim(), block.getPage(), block.getId());
				if (entry != null)
				{
					entries.add(entry);
				}
			}
		}

		return entries;
	}

	/**
	 * Parse a single text line as a TOC entry
	 * @return TocEntry if line matches TOC pattern, null otherwise
	 */
	TocEntry parseTocLine(String line, int sourcePage, String blockId)
	{
		if (line == null || line.isEmpty())
		{
			return null;
		}

		// Skip the TOC keyword itself
		if (isTocKeyword(line))
		{
			return null;
		}

		// Try dotted pattern first: "Title.......42"
		Matcher matcher = TOC_LINE_PATTERN.matcher(line);
		if (matcher.matches())
		{
			return createExplicitEntry(matcher.group(1).trim(), matcher.group(2), blockId);
		}

		// Try simple pattern: "Title    42"
		matcher = TOC_LINE_SIMPLE.matcher(line);
		if (matcher.matches())
		{
			String title = matcher.group(1).trim();
			// Avoid matching regular text that happens to end with a number
			if (title.length() > 1 && !title.matches(".*\\d$"))
			{
				return createExplicitEntry(title, matcher.group(2), blockId);
			}
		}

		return null;
	}

	/**
	 * Create a TocEntry from parsed explicit TOC data
	 */
	private TocEntry createExplicitEntry(String title, String pageStr, String blockId)
	{
		// Clean title: remove trailing dots, spaces
		title = title.replaceAll("[.·…\\s]+$", "").trim();
		if (title.isEmpty())
		{
			return null;
		}

		int targetPage;
		try
		{
			targetPage = Integer.parseInt(pageStr.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}

		// Determine level from title pattern
		int level = detectLevel(title);

		return new TocEntry(title, targetPage, level, blockId, 0.95, TocEntry.SOURCE_EXPLICIT);
	}

	/**
	 * Detect heading level from title pattern
	 * Roman numerals (I, II, III...) or numbered chapters are level 1,
	 * sub-entries with indentation or different patterns are level 2+
	 */
	int detectLevel(String title)
	{
		// Roman numeral chapter pattern: "I.", "II.", "XIV." etc.
		if (title.matches("^[IVXLCDM]+\\.?\\s*$") || title.matches("^[IVXLCDM]+\\.\\s+.*"))
		{
			return 1;
		}

		// Numbered chapter pattern: "1.", "1.1", "Chapter 1"
		if (title.matches("^\\d+\\.\\s+.*") || title.matches("^Chapter\\s+\\d+.*"))
		{
			return 1;
		}

		// Sub-section numbering: "1.1", "1.1.1"
		if (title.matches("^\\d+\\.\\d+.*"))
		{
			return 2;
		}

		// Special entries like "Úvod" (Introduction), "Závěr" (Conclusion)
		String lower = title.toLowerCase();
		if (lower.equals("úvod") || lower.equals("závěr") || lower.equals("introduction")
				|| lower.equals("conclusion") || lower.equals("přílohy") || lower.equals("appendix"))
		{
			return 1;
		}

		// Default: level 1 for explicit TOC entries
		return 1;
	}

	/**
	 * Check if a text string is a TOC keyword/header
	 */
	boolean isTocKeyword(String text)
	{
		if (text == null || text.isEmpty())
		{
			return false;
		}

		String normalized = text.trim().toLowerCase();
		return TOC_KEYWORDS.contains(normalized);
	}
}
