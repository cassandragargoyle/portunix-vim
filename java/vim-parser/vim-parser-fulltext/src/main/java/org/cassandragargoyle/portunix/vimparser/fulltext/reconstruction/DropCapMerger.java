/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext.reconstruction;

import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.TextReconstructionTransform;
import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects and merges drop cap (initial) letters with adjacent body text
 *
 * Drop caps are oversized first letters of a paragraph that span multiple
 * body text lines. PDF extraction produces them as separate text elements.
 * This transform detects the pattern and reconstructs the original word.
 *
 * @author Zdenek
 * @since 2026-02-24
 */
public class DropCapMerger implements TextReconstructionTransform
{
	private static final Logger LOG = LogFactory.getLogger(DropCapMerger.class);

	// Drop cap font size must be at least this factor of median body font size
	static final float FONT_SIZE_RATIO_THRESHOLD = 2.5f;

	// Drop cap bbox height must span at least this many body line heights
	static final float MIN_LINE_SPAN = 2.0f;

	// Tolerance for left-edge alignment (in PDF units)
	static final float LEFT_EDGE_TOLERANCE = 10.0f;

	// Maximum vertical gap between drop cap and body text (in PDF units)
	// Body text first line aligns with top of drop cap, may be slightly above
	static final float MAX_VERTICAL_GAP = 12.0f;

	@Override
	public String getId()
	{
		return "drop-cap-merger";
	}

	@Override
	public String getName()
	{
		return "Drop Cap Merger";
	}

	@Override
	public int apply(List<TextBlock> blocks)
	{
		int mergeCount = 0;

		for (TextBlock block : blocks)
		{
			if (processBlock(block))
			{
				mergeCount++;
			}
		}

		return mergeCount;
	}

	/**
	 * Process a single block for drop cap detection and merging
	 *
	 * @return true if a drop cap was detected and merged
	 */
	boolean processBlock(TextBlock block)
	{
		List<TextElement> elements = block.getTextElements();
		if (elements == null || elements.size() < 2)
		{
			return false;
		}

		// Calculate median font size of body text elements
		float medianFontSize = calculateMedianFontSize(elements);
		if (medianFontSize <= 0)
		{
			return false;
		}

		// Calculate typical body line height
		float bodyLineHeight = estimateBodyLineHeight(elements, medianFontSize);
		if (bodyLineHeight <= 0)
		{
			return false;
		}

		// Find drop cap candidate using local left edge
		TextElement dropCapCandidate = null;
		int candidateIndex = -1;

		for (int i = 0; i < elements.size(); i++)
		{
			TextElement el = elements.get(i);
			if (el.getBbox() == null || el.getText() == null)
			{
				continue;
			}

			// Calculate left edge from nearby elements (within ±50 Y units)
			// to avoid skew from distant elements like footers
			float localLeftEdge = calculateLocalLeftEdge(el, elements);

			if (isDropCapCandidate(el, medianFontSize, bodyLineHeight, localLeftEdge, elements))
			{
				dropCapCandidate = el;
				candidateIndex = i;
				break;
			}
		}

		if (dropCapCandidate == null)
		{
			return false;
		}

		// Find the adjacent body text element to merge with
		TextElement bodyTextElement = findAdjacentBodyText(dropCapCandidate, elements, candidateIndex, medianFontSize);
		if (bodyTextElement == null)
		{
			return false;
		}

		// Validate merge: concatenation must form a plausible word beginning
		String merged = dropCapCandidate.getText() + bodyTextElement.getText();
		if (!isPlausibleWordBeginning(dropCapCandidate.getText(), bodyTextElement.getText()))
		{
			LOG.log(Level.FINE, "Drop cap merge rejected: '{0}' would not form plausible word", merged);
			return false;
		}

		// Perform the merge
		BoundingBox dropCapBbox = new BoundingBox(
			dropCapCandidate.getBbox().getX(),
			dropCapCandidate.getBbox().getY(),
			dropCapCandidate.getBbox().getWidth(),
			dropCapCandidate.getBbox().getHeight()
		);

		bodyTextElement.setText(dropCapCandidate.getText() + bodyTextElement.getText());
		bodyTextElement.setDropCapMerged(true);
		bodyTextElement.setDropCapBbox(dropCapBbox);

		// Update bounding box to union of both elements
		bodyTextElement.getBbox().expandToInclude(dropCapBbox);

		// Remove the drop cap element from the block
		elements.remove(candidateIndex);

		LOG.log(Level.FINE, "Drop cap merged: '{0}' in block {1}",
			LogFactory.args(bodyTextElement.getText(), block.getId()));

		return true;
	}

	/**
	 * Check if a text element is a drop cap candidate
	 */
	boolean isDropCapCandidate(TextElement element, float medianFontSize, float bodyLineHeight,
		float blockLeftEdge, List<TextElement> allElements)
	{
		if (element == null || element.getText() == null || element.getBbox() == null)
		{
			return false;
		}

		String text = element.getText().trim();

		// Condition 1: Single alphabetic character
		if (text.length() != 1 || !Character.isLetter(text.charAt(0)))
		{
			return false;
		}

		// Negative: digit
		if (Character.isDigit(text.charAt(0)))
		{
			return false;
		}

		// Condition 2: Font size >= 2.5x median
		if (element.getFontSize() < medianFontSize * FONT_SIZE_RATIO_THRESHOLD)
		{
			return false;
		}

		// Condition 3: Bbox height spans >= 2 body line heights
		if (element.getBbox().getHeight() < bodyLineHeight * MIN_LINE_SPAN)
		{
			return false;
		}

		// Condition 4: Positioned at or near left edge
		if (element.getBbox().getX() > blockLeftEdge + LEFT_EDGE_TOLERANCE)
		{
			return false;
		}

		// Condition 5: Among first elements by Y position
		if (!isAmongFirstElements(element, allElements))
		{
			return false;
		}

		return true;
	}

	/**
	 * Check if element is among the first elements by Y position
	 */
	private boolean isAmongFirstElements(TextElement element, List<TextElement> allElements)
	{
		float elementY = element.getBbox().getY();
		float minY = Float.MAX_VALUE;
		float maxY = Float.MIN_VALUE;

		for (TextElement el : allElements)
		{
			if (el.getBbox() != null)
			{
				minY = Math.min(minY, el.getBbox().getY());
				maxY = Math.max(maxY, el.getBbox().getY());
			}
		}

		// Element should be within the first third of the block vertically
		float blockHeight = maxY - minY;
		if (blockHeight <= 0)
		{
			return true;
		}

		return elementY <= minY + blockHeight * 0.35f;
	}

	/**
	 * Calculate left edge from elements near the candidate (within ±50 Y units)
	 * to avoid skew from distant elements like page footers
	 */
	private float calculateLocalLeftEdge(TextElement candidate, List<TextElement> elements)
	{
		float candidateY = candidate.getBbox().getY();
		float localLeftEdge = candidate.getBbox().getX();

		for (TextElement el : elements)
		{
			if (el.getBbox() != null && Math.abs(el.getBbox().getY() - candidateY) < 50.0f)
			{
				localLeftEdge = Math.min(localLeftEdge, el.getBbox().getX());
			}
		}

		return localLeftEdge;
	}

	/**
	 * Find the adjacent body text element for merging
	 */
	TextElement findAdjacentBodyText(TextElement dropCap, List<TextElement> elements,
		int dropCapIndex, float medianFontSize)
	{
		TextElement bestCandidate = null;
		float bestY = Float.MAX_VALUE;

		BoundingBox dcBox = dropCap.getBbox();

		for (int i = 0; i < elements.size(); i++)
		{
			if (i == dropCapIndex)
			{
				continue;
			}

			TextElement el = elements.get(i);
			if (el.getBbox() == null || el.getText() == null || el.getText().trim().isEmpty())
			{
				continue;
			}

			// Body text should have font size close to median
			if (el.getFontSize() > medianFontSize * 1.5f)
			{
				continue;
			}

			BoundingBox elBox = el.getBbox();

			// Check vertical overlap or proximity
			boolean verticallyClose = elBox.getY() >= dcBox.getY() - MAX_VERTICAL_GAP
				&& elBox.getY() <= dcBox.getBottom() + MAX_VERTICAL_GAP;

			if (!verticallyClose)
			{
				continue;
			}

			// Check the element is to the right of the drop cap
			boolean horizontallyAdjacent = elBox.getX() > dcBox.getX();

			if (!horizontallyAdjacent)
			{
				continue;
			}

			// Pick the topmost (smallest Y) candidate - this is the first line
			// of body text next to the drop cap
			if (elBox.getY() < bestY)
			{
				bestY = elBox.getY();
				bestCandidate = el;
			}
		}

		// Check for significant vertical gap (standalone heading check)
		if (bestCandidate != null)
		{
			float gap = Math.abs(bestCandidate.getBbox().getY() - dcBox.getY());
			if (gap > dcBox.getHeight() * 2)
			{
				return null;
			}
		}

		return bestCandidate;
	}

	/**
	 * Validate that concatenation forms a plausible word beginning
	 */
	boolean isPlausibleWordBeginning(String dropCapChar, String bodyText)
	{
		if (bodyText == null || bodyText.isEmpty())
		{
			return false;
		}

		char firstBodyChar = bodyText.charAt(0);

		// Adjacent text starts with space — not a word continuation
		if (Character.isWhitespace(firstBodyChar))
		{
			return false;
		}

		// Adjacent text starts with uppercase — usually a separate word/heading
		// Exception: single uppercase letter followed by space forms a 2-letter
		// abbreviation (e.g., drop cap "A" + "I se nachází" → "AI se nachází")
		if (Character.isUpperCase(firstBodyChar))
		{
			if (bodyText.length() > 1 && Character.isWhitespace(bodyText.charAt(1)))
			{
				return true;
			}
			return false;
		}

		// Adjacent text starts with a digit — likely not a word
		if (Character.isDigit(firstBodyChar))
		{
			return false;
		}

		// Adjacent text starts with lowercase letter — plausible word continuation
		return Character.isLetter(firstBodyChar);
	}

	/**
	 * Calculate median font size from text elements, excluding outliers
	 */
	float calculateMedianFontSize(List<TextElement> elements)
	{
		List<Float> fontSizes = elements.stream()
			.map(TextElement::getFontSize)
			.filter(size -> size > 0)
			.sorted()
			.toList();

		if (fontSizes.isEmpty())
		{
			return 0;
		}

		int mid = fontSizes.size() / 2;
		if (fontSizes.size() % 2 == 0)
		{
			return (fontSizes.get(mid - 1) + fontSizes.get(mid)) / 2.0f;
		}
		return fontSizes.get(mid);
	}

	/**
	 * Estimate body line height from elements with font size close to median
	 */
	float estimateBodyLineHeight(List<TextElement> elements, float medianFontSize)
	{
		// Use elements with font size within 20% of median
		float tolerance = medianFontSize * 0.2f;

		List<TextElement> bodyElements = elements.stream()
			.filter(el -> el.getBbox() != null)
			.filter(el -> Math.abs(el.getFontSize() - medianFontSize) <= tolerance)
			.sorted(Comparator.comparingDouble(el -> el.getBbox().getY()))
			.toList();

		if (bodyElements.size() < 2)
		{
			// Fallback: estimate from font size (typical line height ~ 1.2x font size)
			return medianFontSize * 1.2f;
		}

		// Calculate average line height from consecutive Y positions
		float totalGap = 0;
		int gapCount = 0;

		for (int i = 1; i < bodyElements.size(); i++)
		{
			float gap = bodyElements.get(i).getBbox().getY() - bodyElements.get(i - 1).getBbox().getY();
			if (gap > 0 && gap < medianFontSize * 3)
			{
				totalGap += gap;
				gapCount++;
			}
		}

		if (gapCount > 0)
		{
			return totalGap / gapCount;
		}

		return medianFontSize * 1.2f;
	}
}
