/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Detects page number blocks based on content patterns and spatial position.
 * A block is classified as a page number if it contains short numeric content
 * matching common page number formats and is positioned in the top or bottom
 * 10% of the page. Page height is derived from actual block positions per page,
 * making detection independent of page format (A4, letter, ebook, etc.).
 *
 * @author Zdenek
 * @since 2026-03-08
 */
public class PageNumberDetector
{
	// Page number content patterns
	private static final Pattern SIMPLE_NUMBER = Pattern.compile("^\\d{1,4}$");
	private static final Pattern DASHED_NUMBER = Pattern.compile("^-\\s*\\d+\\s*-$");
	private static final Pattern FRACTION_NUMBER = Pattern.compile("^\\d+\\s*/\\s*\\d+$");

	// Position threshold - top/bottom 10% of page
	private static final float POSITION_THRESHOLD = 0.10f;

	// Maximum text length for a page number block
	private static final int MAX_TEXT_LENGTH = 10;

	/**
	 * Detect and classify page number blocks in the given list.
	 * Page height is derived from actual block positions per page.
	 * Sets type to "page_number" and significance to "auxiliary" for detected blocks.
	 *
	 * @return number of page number blocks detected
	 */
	public int detect(List<TextBlock> blocks)
	{
		if (blocks == null || blocks.isEmpty())
		{
			return 0;
		}

		// Calculate effective page height per page from actual block positions
		Map<Integer, float[]> pageBounds = calculatePageBounds(blocks);

		int detected = 0;

		for (TextBlock block : blocks)
		{
			// Skip blocks already classified as toc
			if ("toc".equals(block.getType()))
			{
				continue;
			}

			if (isPageNumber(block, pageBounds))
			{
				block.setType("page_number");
				block.setSignificance("auxiliary");
				detected++;
			}
		}

		return detected;
	}

	/**
	 * Calculate min/max Y for each page from block positions.
	 * Returns map of page -> [minY, maxY, blockCount]
	 */
	private Map<Integer, float[]> calculatePageBounds(List<TextBlock> blocks)
	{
		Map<Integer, float[]> bounds = new HashMap<>();

		for (TextBlock block : blocks)
		{
			BoundingBox bbox = block.getBbox();
			if (bbox == null)
			{
				continue;
			}

			int page = block.getPage();
			float blockTop = bbox.getY();
			float blockBottom = bbox.getY() + bbox.getHeight();

			float[] pageBound = bounds.get(page);
			if (pageBound == null)
			{
				bounds.put(page, new float[]{blockTop, blockBottom, 1});
			}
			else
			{
				pageBound[0] = Math.min(pageBound[0], blockTop);
				pageBound[1] = Math.max(pageBound[1], blockBottom);
				pageBound[2]++;
			}
		}

		return bounds;
	}

	/**
	 * Check if a block looks like a page number
	 */
	boolean isPageNumber(TextBlock block, Map<Integer, float[]> pageBounds)
	{
		String text = block.getFullText().trim();

		// Must be short text
		if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH)
		{
			return false;
		}

		// Must match a page number pattern
		if (!matchesPageNumberPattern(text))
		{
			return false;
		}

		// Must be at top or bottom of page
		BoundingBox bbox = block.getBbox();
		if (bbox == null)
		{
			return false;
		}

		float[] bounds = pageBounds.get(block.getPage());
		if (bounds == null || bounds[2] < 2)
		{
			// Need at least 2 blocks on page to determine relative position
			return false;
		}

		return isAtPageEdge(bbox, bounds[0], bounds[1]);
	}

	/**
	 * Check if text matches any known page number pattern
	 */
	boolean matchesPageNumberPattern(String text)
	{
		return SIMPLE_NUMBER.matcher(text).matches()
			|| DASHED_NUMBER.matcher(text).matches()
			|| FRACTION_NUMBER.matcher(text).matches();
	}

	/**
	 * Check if the bounding box is in the top or bottom 10% of the page content area
	 */
	private boolean isAtPageEdge(BoundingBox bbox, float pageMinY, float pageMaxY)
	{
		float pageSpan = pageMaxY - pageMinY;
		if (pageSpan <= 0)
		{
			return false;
		}

		float blockY = bbox.getY();
		float relativePosition = (blockY - pageMinY) / pageSpan;

		return relativePosition < POSITION_THRESHOLD || relativePosition > (1.0f - POSITION_THRESHOLD);
	}
}
