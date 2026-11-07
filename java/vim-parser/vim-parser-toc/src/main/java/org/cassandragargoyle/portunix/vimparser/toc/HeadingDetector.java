/*
 * SPDX-License-Identifier: MIT
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.toc;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.cassandragargoyle.portunix.vimparser.model.TocEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Detects headings from text blocks and assigns hierarchical levels
 * based on font size distribution, font weight, and position heuristics.
 * Form labels, numbers, dimensions, marks, running headers and text on
 * drawing pages are rejected as noise
 *
 * @author Zdenek
 * @since 2026-03-05
 */
public class HeadingDetector
{
	// Minimum font size ratio above base to consider as heading
	private static final float HEADING_SIZE_THRESHOLD = 1.15f;

	// Confidence adjustments
	private static final double BASE_CONFIDENCE = 0.7;
	private static final double BOLD_BONUS = 0.1;
	private static final double SIZE_BONUS = 0.1;
	private static final double POSITION_BONUS = 0.05;

	// A heading title must contain a run of three letters or two CJK characters
	private static final Pattern LETTER_RUN = Pattern.compile(
			"\\p{L}{3,}|[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]{2,}");

	// Max horizontal gap between a form label and its value on the same row (points)
	private static final float FORM_VALUE_MAX_GAP = 200f;

	// Form value is short; longer text after a colon is running text of a heading
	private static final int FORM_VALUE_MAX_WORDS = 6;

	// Position tolerance for repeated titles (points)
	private static final float REPEAT_POSITION_TOLERANCE = 10f;

	// Same title at the same position on this many pages is a running header
	private static final int REPEAT_MIN_PAGES = 3;

	// Text extent above A3 (longer side, points) marks a candidate drawing page
	private static final float DRAWING_PAGE_MIN_EXTENT = 1300f;

	// Block with at least this many words counts as running text
	private static final int RUNNING_TEXT_MIN_WORDS = 8;

	// Drawing page has less than this share of running text blocks
	private static final double DRAWING_MAX_RUNNING_TEXT_RATIO = 0.2;

	// Drawing page has at least this many blocks (labels, marks, title block)
	private static final int DRAWING_MIN_BLOCKS = 10;

	// Heading candidate together with its source block
	private record Candidate(TocEntry entry, TextBlock block)
	{
	}

	/**
	 * Detect headings from text blocks and produce TOC entries
	 * @param blocks text blocks with analyzed structure
	 * @return list of TOC entries sorted by page then position
	 */
	public List<TocEntry> detect(List<TextBlock> blocks)
	{
		if (blocks == null || blocks.isEmpty())
		{
			return new ArrayList<>();
		}

		float baseFontSize = calculateDocumentBaseFontSize(blocks);
		Map<Float, Integer> headingSizeDistribution = collectHeadingFontSizes(blocks, baseFontSize);
		List<Float> levelThresholds = determineLevelThresholds(headingSizeDistribution, baseFontSize);

		Set<Integer> drawingPages = detectDrawingPages(blocks);
		Map<Integer, List<TextBlock>> blocksByPage = new HashMap<>();
		for (TextBlock block : blocks)
		{
			blocksByPage.computeIfAbsent(block.getPage(), p -> new ArrayList<>()).add(block);
		}

		List<Candidate> candidates = new ArrayList<>();

		for (TextBlock block : blocks)
		{
			if (drawingPages.contains(block.getPage()))
			{
				continue;
			}

			TocEntry entry = evaluateBlock(block, baseFontSize, levelThresholds);
			if (entry != null && !isMarkOrNumber(entry.getTitle())
					&& !isFormLabel(entry.getTitle(), block, blocksByPage.get(block.getPage())))
			{
				candidates.add(new Candidate(entry, block));
			}
		}

		// Sort by page, then by vertical position of the source block
		candidates.sort(Comparator
				.comparingInt((Candidate c) -> c.entry().getPage())
				.thenComparing(c -> c.block().getBbox() != null ? c.block().getBbox().getY() : 0f));

		return removeRepeatedTitles(candidates);
	}

	/**
	 * Calculate the most common font size across all blocks in the document
	 */
	float calculateDocumentBaseFontSize(List<TextBlock> blocks)
	{
		Map<Float, Integer> fontSizeCounts = new HashMap<>();

		for (TextBlock block : blocks)
		{
			for (TextElement element : block.getTextElements())
			{
				float size = Math.round(element.getFontSize() * 10) / 10.0f;
				fontSizeCounts.merge(size, 1, Integer::sum);
			}
		}

		return fontSizeCounts.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(12.0f);
	}

	/**
	 * Collect font sizes of elements that qualify as headings
	 */
	private Map<Float, Integer> collectHeadingFontSizes(List<TextBlock> blocks, float baseFontSize)
	{
		Map<Float, Integer> sizes = new TreeMap<>(Comparator.reverseOrder());

		for (TextBlock block : blocks)
		{
			for (TextElement element : block.getTextElements())
			{
				if (isHeadingCandidate(element, baseFontSize))
				{
					float size = Math.round(element.getFontSize() * 10) / 10.0f;
					sizes.merge(size, 1, Integer::sum);
				}
			}
		}

		return sizes;
	}

	/**
	 * Determine font size thresholds for heading levels 1, 2, 3
	 * based on the distribution of heading font sizes
	 */
	List<Float> determineLevelThresholds(Map<Float, Integer> headingSizes, float baseFontSize)
	{
		List<Float> thresholds = new ArrayList<>();

		if (headingSizes.isEmpty())
		{
			return thresholds;
		}

		// Get distinct heading sizes sorted descending
		List<Float> distinctSizes = new ArrayList<>(headingSizes.keySet());
		distinctSizes.sort(Comparator.reverseOrder());

		// Level 1 = largest heading size
		// Level 2 = second largest (if exists)
		// Level 3 = third largest or anything above base
		for (int i = 0; i < Math.min(3, distinctSizes.size()); i++)
		{
			thresholds.add(distinctSizes.get(i));
		}

		return thresholds;
	}

	/**
	 * Evaluate a single block for heading candidacy
	 * @return TocEntry if block contains a heading, null otherwise
	 */
	TocEntry evaluateBlock(TextBlock block, float baseFontSize, List<Float> levelThresholds)
	{
		// Skip blocks already identified as TOC page content
		if ("toc".equals(block.getType()))
		{
			return null;
		}

		// Use existing structural analysis: blocks with headers detected by fulltext plugin
		if (block.getHeader() != null && !block.getHeader().isEmpty())
		{
			float headingFontSize = getHeaderFontSize(block, baseFontSize);
			boolean isBold = isHeaderBold(block);

			// Only include if font size is significantly above base or font is bold
			if (headingFontSize > baseFontSize * HEADING_SIZE_THRESHOLD || isBold)
			{
				int level = assignLevel(headingFontSize, levelThresholds);
				double confidence = calculateConfidence(headingFontSize, baseFontSize, isBold, block);

				return new TocEntry(
						block.getHeader(),
						block.getPage(),
						level,
						block.getId(),
						confidence,
						TocEntry.SOURCE_HEURISTIC
				);
			}
		}

		// Check for standalone large text elements that might be headings
		// even if analyzeStructure didn't flag them (e.g., content-only blocks with large text)
		if (block.getHeader() == null && !block.getTextElements().isEmpty())
		{
			TextElement firstElement = block.getTextElements().get(0);
			if (isStandaloneHeading(firstElement, baseFontSize, block))
			{
				int level = assignLevel(firstElement.getFontSize(), levelThresholds);
				double confidence = calculateConfidence(
						firstElement.getFontSize(), baseFontSize,
						isBoldFont(firstElement.getFontName()), block) - 0.05;

				return new TocEntry(
						firstElement.getText().trim(),
						block.getPage(),
						level,
						block.getId(),
						Math.max(0.5, confidence),
						TocEntry.SOURCE_HEURISTIC
				);
			}
		}

		return null;
	}

	/**
	 * Assign heading level (1-3) based on font size relative to thresholds
	 */
	int assignLevel(float fontSize, List<Float> levelThresholds)
	{
		if (levelThresholds.isEmpty())
		{
			return 1;
		}

		float rounded = Math.round(fontSize * 10) / 10.0f;

		for (int i = 0; i < levelThresholds.size(); i++)
		{
			if (rounded >= levelThresholds.get(i))
			{
				return i + 1;
			}
		}

		// Smaller than all thresholds but still a heading
		return Math.min(3, levelThresholds.size() + 1);
	}

	/**
	 * Calculate confidence score for heading detection
	 */
	private double calculateConfidence(float fontSize, float baseFontSize, boolean isBold, TextBlock block)
	{
		double confidence = BASE_CONFIDENCE;

		// Font size significantly larger than base
		float sizeRatio = fontSize / baseFontSize;
		if (sizeRatio > 1.5)
		{
			confidence += SIZE_BONUS;
		}

		// Bold font
		if (isBold)
		{
			confidence += BOLD_BONUS;
		}

		// Block at top of page (Y position close to 0)
		if (block.getBbox() != null && block.getBbox().getY() < 150)
		{
			confidence += POSITION_BONUS;
		}

		return Math.min(1.0, confidence);
	}

	/**
	 * Get the font size of the header element in a block
	 */
	private float getHeaderFontSize(TextBlock block, float baseFontSize)
	{
		if (block.getTextElements().isEmpty())
		{
			return baseFontSize;
		}

		// The header is typically the first element that was identified as header
		for (TextElement element : block.getTextElements())
		{
			if (element.isLikelyHeader(baseFontSize))
			{
				return element.getFontSize();
			}
		}

		// Fallback: first element
		return block.getTextElements().get(0).getFontSize();
	}

	/**
	 * Check if the header element uses a bold font
	 */
	private boolean isHeaderBold(TextBlock block)
	{
		if (block.getTextElements().isEmpty())
		{
			return false;
		}

		// Check first element (typically the header)
		return isBoldFont(block.getTextElements().get(0).getFontName());
	}

	/**
	 * Determine if a font name indicates bold weight
	 */
	boolean isBoldFont(String fontName)
	{
		if (fontName == null)
		{
			return false;
		}

		String lower = fontName.toLowerCase();
		return lower.contains("bold") || lower.contains("extrabold")
				|| lower.contains("semibold") || lower.contains("heavy")
				|| lower.contains("black");
	}

	/**
	 * Check if a text element qualifies as a standalone heading
	 * (large font, short text, single-element block or first element)
	 */
	private boolean isStandaloneHeading(TextElement element, float baseFontSize, TextBlock block)
	{
		if (element.getFontSize() <= baseFontSize * 1.3f)
		{
			return false;
		}

		String text = element.getText().trim();
		// Standalone headings are typically short (under 80 chars)
		if (text.length() > 80 || text.isEmpty())
		{
			return false;
		}

		// Block with only 1-2 elements is more likely a heading
		return block.getTextElements().size() <= 2;
	}

	/**
	 * Check if a text element qualifies as heading candidate
	 */
	private boolean isHeadingCandidate(TextElement element, float baseFontSize)
	{
		return element.getFontSize() > baseFontSize * HEADING_SIZE_THRESHOLD
				|| isBoldFont(element.getFontName());
	}

	/**
	 * Check if a title is only a number, dimension, mark or single character
	 * (no run of at least three letters)
	 */
	boolean isMarkOrNumber(String title)
	{
		return title == null || !LETTER_RUN.matcher(title).find();
	}

	/**
	 * Check if a title is a form label: ends with a colon and is followed by a
	 * value in the same block, next to it on the same row, or right below it.
	 * Only the first value line is judged, as blocks of forms often join
	 * several fields
	 * @param pageBlocks blocks on the same page as the label
	 */
	boolean isFormLabel(String title, TextBlock block, List<TextBlock> pageBlocks)
	{
		if (title == null || !title.trim().endsWith(":"))
		{
			return false;
		}

		// Label element is not always the first one (section caption above it)
		List<TextElement> elements = block.getTextElements();
		int labelIndex = 0;
		for (int i = 0; i < elements.size(); i++)
		{
			if (title.trim().equals(elements.get(i).getText().trim()))
			{
				labelIndex = i;
				break;
			}
		}

		// Value in the same block: the element after the label
		if (labelIndex < elements.size() - 1)
		{
			return isFormValue(elements.get(labelIndex + 1).getText());
		}

		BoundingBox label = elements.isEmpty() ? block.getBbox() : elements.get(labelIndex).getBbox();
		if (label == null || pageBlocks == null)
		{
			return false;
		}

		for (TextBlock other : pageBlocks)
		{
			if (other == block || other.getPage() != block.getPage())
			{
				continue;
			}

			for (TextElement element : other.getTextElements())
			{
				BoundingBox value = element.getBbox();

				// Value to the right on the same row
				boolean sameRow = value.getY() < label.getBottom() && label.getY() < value.getBottom();
				float gapRight = value.getX() - label.getRight();
				boolean right = sameRow && gapRight >= -REPEAT_POSITION_TOLERANCE && gapRight <= FORM_VALUE_MAX_GAP;

				// Value right below, aligned to the left edge of the label
				float gapBelow = value.getY() - label.getBottom();
				boolean below = gapBelow >= -REPEAT_POSITION_TOLERANCE && gapBelow <= label.getHeight()
						&& Math.abs(value.getX() - label.getX()) <= REPEAT_POSITION_TOLERANCE;

				if ((right || below) && isFormValue(element.getText()))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Remove candidates whose title repeats at the same position on several pages
	 * (page or table headers). Candidates with the same title are clustered
	 * transitively, so a small drift of position from page to page keeps them in
	 * one cluster. Order of the remaining candidates is kept
	 */
	List<TocEntry> removeRepeatedTitles(List<Candidate> candidates)
	{
		// Group candidate indexes by normalized title, clustering runs only within a group
		Map<String, List<Integer>> byTitle = new HashMap<>();
		for (int i = 0; i < candidates.size(); i++)
		{
			byTitle.computeIfAbsent(normalizeTitle(candidates.get(i).entry().getTitle()),
					t -> new ArrayList<>()).add(i);
		}

		boolean[] repeated = new boolean[candidates.size()];
		for (List<Integer> group : byTitle.values())
		{
			if (group.size() < REPEAT_MIN_PAGES)
			{
				continue;
			}

			boolean[] visited = new boolean[group.size()];
			for (int start = 0; start < group.size(); start++)
			{
				if (visited[start])
				{
					continue;
				}

				// Flood fill cluster of the same title at the same position
				List<Integer> members = new ArrayList<>();
				List<Integer> queue = new ArrayList<>(List.of(start));
				visited[start] = true;
				while (!queue.isEmpty())
				{
					int current = queue.remove(queue.size() - 1);
					members.add(group.get(current));
					BoundingBox a = candidates.get(group.get(current)).block().getBbox();
					for (int j = 0; j < group.size(); j++)
					{
						if (!visited[j] && isSamePosition(a, candidates.get(group.get(j)).block().getBbox()))
						{
							visited[j] = true;
							queue.add(j);
						}
					}
				}

				Set<Integer> pages = new HashSet<>();
				for (int member : members)
				{
					pages.add(candidates.get(member).block().getPage());
				}
				if (pages.size() >= REPEAT_MIN_PAGES)
				{
					for (int member : members)
					{
						repeated[member] = true;
					}
				}
			}
		}

		List<TocEntry> entries = new ArrayList<>();
		for (int i = 0; i < candidates.size(); i++)
		{
			if (!repeated[i])
			{
				entries.add(candidates.get(i).entry());
			}
		}

		return entries;
	}

	/**
	 * Detect pages that look like drawings: many blocks spread over a sheet
	 * larger than A3 with little running text
	 */
	Set<Integer> detectDrawingPages(List<TextBlock> blocks)
	{
		Map<Integer, float[]> extents = new HashMap<>();
		Map<Integer, int[]> counts = new HashMap<>();

		for (TextBlock block : blocks)
		{
			int[] count = counts.computeIfAbsent(block.getPage(), p -> new int[2]);
			count[0]++;
			if (countWords(block.getFullText()) >= RUNNING_TEXT_MIN_WORDS)
			{
				count[1]++;
			}

			BoundingBox bbox = block.getBbox();
			if (bbox != null)
			{
				float[] extent = extents.computeIfAbsent(block.getPage(), p -> new float[2]);
				extent[0] = Math.max(extent[0], bbox.getRight());
				extent[1] = Math.max(extent[1], bbox.getBottom());
			}
		}

		Set<Integer> drawingPages = new HashSet<>();
		for (Map.Entry<Integer, float[]> e : extents.entrySet())
		{
			float longerSide = Math.max(e.getValue()[0], e.getValue()[1]);
			int[] count = counts.get(e.getKey());
			double runningTextRatio = (double) count[1] / count[0];
			if (longerSide > DRAWING_PAGE_MIN_EXTENT && count[0] >= DRAWING_MIN_BLOCKS
					&& runningTextRatio < DRAWING_MAX_RUNNING_TEXT_RATIO)
			{
				drawingPages.add(e.getKey());
			}
		}

		return drawingPages;
	}

	private String normalizeTitle(String title)
	{
		return title == null ? "" : title.trim().replaceAll("\\s+", " ").toLowerCase();
	}

	private boolean isSamePosition(BoundingBox a, BoundingBox b)
	{
		if (a == null || b == null)
		{
			return false;
		}
		return Math.abs(a.getX() - b.getX()) <= REPEAT_POSITION_TOLERANCE
				&& Math.abs(a.getY() - b.getY()) <= REPEAT_POSITION_TOLERANCE;
	}

	/**
	 * Form value is a short text, including another label of a label column;
	 * a long line is running text of a heading
	 */
	private boolean isFormValue(String text)
	{
		int words = countWords(text);
		return words > 0 && words <= FORM_VALUE_MAX_WORDS;
	}

	private int countWords(String text)
	{
		String trimmed = text == null ? "" : text.trim();
		return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
	}
}
