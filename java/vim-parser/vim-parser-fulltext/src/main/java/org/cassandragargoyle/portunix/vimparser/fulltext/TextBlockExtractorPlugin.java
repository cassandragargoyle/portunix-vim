/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TextElement;
import org.cassandragargoyle.portunix.vimparser.fulltext.reconstruction.DropCapMerger;
import org.cassandragargoyle.portunix.vimparser.fulltext.reconstruction.TextReconstructionPipeline;
import org.pf4j.Extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Text block extractor plugin using PDFBox
 * Extracts text with spatial clustering into logical blocks
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@Extension
public class TextBlockExtractorPlugin implements ExtractorPlugin, OrchestrationAware
{
	private static final Logger LOG = LogFactory.getLogger(TextBlockExtractorPlugin.class);

	public static final String PLUGIN_ID = "fulltext";
	public static final String RESULT_KEY_BLOCKS = "textBlocks";
	public static final String RESULT_KEY_PAGE_COUNT = "pageCount";

	// Clustering parameters
	private static final float HORIZONTAL_GAP_THRESHOLD = 50.0f;
	private static final float VERTICAL_GAP_THRESHOLD = 15.0f;
	// Font-relative limits (in em) so small drawing labels are not joined across a sheet
	private static final float MAX_WORD_GAP_EM = 3.0f;
	private static final float VERTICAL_GAP_EM = 3.0f;
	private static final float BASELINE_TOLERANCE_EM = 0.5f;
	private static final float GLYPH_OVERLAP_EM = 0.5f;

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "Text Block Extractor";
	}

	@Override
	public String getVersion()
	{
		return "0.1.0";
	}

	@Override
	public int getPriority()
	{
		return 10;  // First to run
	}

	@Override
	public Set<String> getDependencies()
	{
		return Collections.emptySet();  // No dependencies
	}

	@Override
	public Set<String> getPredecessors()
	{
		return Collections.emptySet();  // No predecessors
	}

	@Override
	public boolean isAutoEnabled()
	{
		return true;
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.EXTRACT;
	}

	@Override
	public ExecutionMode[] getSupportedModes()
	{
		return new ExecutionMode[] {
			ExecutionMode.SYNC,
			ExecutionMode.ASYNC_THREAD
		};
	}

	@Override
	public PluginResult extract(PluginContext context)
	{
		long startTime = System.currentTimeMillis();

		try
		{
			byte[] pdfContent = context.getPdfContent();
			if (pdfContent == null || pdfContent.length == 0)
			{
				return PluginResult.failure("No PDF content provided");
			}

			List<TextBlock> allBlocks = new ArrayList<>();
			int pageCount;

			try (PDDocument document = Loader.loadPDF(pdfContent))
			{
				pageCount = document.getNumberOfPages();

				int startPage = context.isAllPages() ? 1 : context.getPageNumber();
				int endPage = context.isAllPages() ? pageCount : context.getPageNumber();

				for (int pageNum = startPage; pageNum <= endPage; pageNum++)
				{
					List<TextElement> elements = extractTextElements(document, pageNum);

					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Page {0}: extracted {1} text elements",
								LogFactory.args(pageNum, elements.size()));
					}

					// Cluster elements into blocks
					List<TextBlock> blocks = clusterIntoBlocks(elements, pageNum);

					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Page {0}: identified {1} text blocks",
								LogFactory.args(pageNum, blocks.size()));
					}

					allBlocks.addAll(blocks);
				}
			}

			// Layer 1.5: Text reconstruction post-processing (drop cap merging)
			// Must run BEFORE analyzeStructure() so merged elements are classified correctly
			Boolean dropCapEnabled = (Boolean) context.getOption("dropCapEnabled");
			if (dropCapEnabled == null || dropCapEnabled)
			{
				TextReconstructionPipeline pipeline = new TextReconstructionPipeline();
				pipeline.addTransform(new DropCapMerger());
				int modifications = pipeline.process(allBlocks, context.isVerbose());
				if (modifications > 0 && context.isVerbose())
				{
					LOG.log(Level.INFO, "Text reconstruction: {0} drop cap(s) merged", modifications);
				}
			}

			// Analyze structure for each block (after reconstruction so merged text is classified correctly)
			for (TextBlock block : allBlocks)
			{
				float baseFontSize = calculateBaseFontSize(block.getTextElements());
				block.analyzeStructure(baseFontSize);
			}

			// Detect page numbers before significance assignment
			PageNumberDetector pageNumberDetector = new PageNumberDetector();
			int pageNumbers = pageNumberDetector.detect(allBlocks);
			if (pageNumbers > 0 && context.isVerbose())
			{
				LOG.log(Level.INFO, "Page number detection: {0} block(s) identified", pageNumbers);
			}

			// Assign default significance based on type
			for (TextBlock block : allBlocks)
			{
				if (block.getSignificance() == null)
				{
					String blockType = block.getType();
					if ("toc".equals(blockType) || "page_number".equals(blockType))
					{
						block.setSignificance("auxiliary");
					}
					else
					{
						block.setSignificance("primary");
					}
				}
			}

			// Store results in context for other plugins
			context.putSharedData(RESULT_KEY_BLOCKS, allBlocks);
			context.putSharedData(RESULT_KEY_PAGE_COUNT, pageCount);

			PluginResult result = PluginResult.success(allBlocks);
			result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			result.addMetadata("block_count", String.valueOf(allBlocks.size()));
			result.addMetadata("page_count", String.valueOf(pageCount));

			return result;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Error extracting text blocks: {0}", e.getMessage());
			return PluginResult.failure("Text extraction failed: " + e.getMessage());
		}
	}

	/**
	 * Extract text elements with positions from a PDF page
	 */
	private List<TextElement> extractTextElements(PDDocument document, int pageNum) throws IOException
	{
		List<TextElement> elements = new ArrayList<>();

		PDFTextStripper stripper = new PDFTextStripper()
		{
			// Runs of the current PDFBox line; one line can hold several labels on different baselines
			private final List<TextRun> openRuns = new ArrayList<>();
			private TextRun lastRun = null;

			@Override
			protected void writeString(String text, List<TextPosition> textPositions) throws IOException
			{
				if (textPositions.isEmpty())
				{
					return;
				}

				for (TextPosition pos : textPositions)
				{
					float x = pos.getXDirAdj();
					float y = pos.getYDirAdj();
					float fontSize = pos.getFontSizeInPt();
					String fontName = pos.getFont().getName();
					String character = pos.getUnicode();

					// Skip whitespace-only characters for position tracking
					if (character.trim().isEmpty())
					{
						if (lastRun != null && lastRun.text.length() > 0)
						{
							lastRun.text.append(character);
						}
						continue;
					}

					TextRun run = findRun(x, y, fontSize);
					if (run == null)
					{
						run = new TextRun(x, y, fontSize, fontName);
						openRuns.add(run);
					}

					run.text.append(character);
					run.endX = x + pos.getWidth();
					run.height = Math.max(run.height, pos.getHeight());
					lastRun = run;
				}
			}

			/**
			 * Find the open run that the glyph continues: same baseline and a small horizontal gap
			 */
			private TextRun findRun(float x, float y, float fontSize)
			{
				TextRun best = null;
				float bestScore = Float.MAX_VALUE;
				// Tolerances follow the glyph's own size, so a large drop cap does not absorb body text
				for (TextRun run : openRuns)
				{
					float baselineDiff = Math.abs(y - run.baseline);
					if (baselineDiff > fontSize * BASELINE_TOLERANCE_EM)
					{
						continue;
					}

					float gap = x - run.endX;
					if (gap < -fontSize * GLYPH_OVERLAP_EM
						|| gap > relativeLimit(HORIZONTAL_GAP_THRESHOLD, fontSize, MAX_WORD_GAP_EM))
					{
						continue;
					}

					float score = Math.abs(gap) + baselineDiff;
					if (score < bestScore)
					{
						bestScore = score;
						best = run;
					}
				}
				return best;
			}

			@Override
			protected void writeLineSeparator() throws IOException
			{
				flushRuns(elements, pageNum);
			}

			@Override
			protected void endPage(PDPage page) throws IOException
			{
				flushRuns(elements, pageNum);
				super.endPage(page);
			}

			private void flushRuns(List<TextElement> elements, int pageNum)
			{
				for (TextRun run : openRuns)
				{
					String text = run.text.toString().trim();
					if (!text.isEmpty())
					{
						BoundingBox bbox = new BoundingBox(
							run.startX,
							run.baseline - run.height,
							run.endX - run.startX,
							run.height
						);
						elements.add(new TextElement(text, bbox, pageNum, run.fontSize, run.fontName));
					}
				}

				// Reset for next line
				openRuns.clear();
				lastRun = null;
			}
		};

		stripper.setSortByPosition(true);
		stripper.setStartPage(pageNum);
		stripper.setEndPage(pageNum);
		stripper.getText(document);

		return elements;
	}

	/**
	 * Cluster text elements into blocks based on spatial proximity
	 */
	private List<TextBlock> clusterIntoBlocks(List<TextElement> elements, int pageNum)
	{
		if (elements.isEmpty())
		{
			return new ArrayList<>();
		}

		// Sort elements by Y position (top to bottom), then by X (left to right)
		elements.sort(Comparator
			.comparingDouble((TextElement e) -> e.getBbox().getY())
			.thenComparingDouble(e -> e.getBbox().getX()));

		List<TextBlock> blocks = new ArrayList<>();
		int blockCounter = 1;

		// Group elements into columns first
		List<List<TextElement>> columns = detectColumns(elements);

		// Process each column
		for (List<TextElement> columnElements : columns)
		{
			// Within each column, group by vertical and horizontal proximity;
			// several blocks stay open so distant labels at the same height do not join
			List<OpenBlock> openBlocks = new ArrayList<>();

			for (TextElement element : columnElements)
			{
				BoundingBox bbox = element.getBbox();
				float y = bbox.getY();
				float left = bbox.getX();
				float right = left + bbox.getWidth();

				OpenBlock target = null;
				float bestGap = Float.MAX_VALUE;
				Iterator<OpenBlock> it = openBlocks.iterator();
				while (it.hasNext())
				{
					OpenBlock open = it.next();
					float gap = y - open.lastY - open.lastHeight;

					// Large vertical gap closes the block; elements are sorted by Y, so it never reopens
					float maxVerticalGap = relativeLimit(VERTICAL_GAP_THRESHOLD + open.lastHeight,
						open.lastFontSize, VERTICAL_GAP_EM);
					if (gap > maxVerticalGap)
					{
						it.remove();
						continue;
					}

					// Element must overlap the block horizontally (with a font-relative tolerance)
					BoundingBox blockBox = open.block.getBbox();
					float tolerance = relativeLimit(HORIZONTAL_GAP_THRESHOLD,
						Math.max(element.getFontSize(), open.lastFontSize), MAX_WORD_GAP_EM);
					if (left > blockBox.getX() + blockBox.getWidth() + tolerance
						|| right < blockBox.getX() - tolerance)
					{
						continue;
					}

					if (gap < bestGap)
					{
						bestGap = gap;
						target = open;
					}
				}

				if (target == null)
				{
					target = new OpenBlock(new TextBlock("block_" + blockCounter++, pageNum));
					openBlocks.add(target);
					blocks.add(target.block);
				}

				target.block.addTextElement(element);
				target.lastY = y;
				target.lastHeight = bbox.getHeight();
				target.lastFontSize = element.getFontSize();
			}
		}

		return blocks;
	}

	/**
	 * Limit an absolute threshold by a multiple of the font size; unknown font size keeps the absolute value
	 */
	private static float relativeLimit(float absolute, float fontSize, float em)
	{
		if (fontSize <= 0)
		{
			return absolute;
		}
		return Math.min(absolute, fontSize * em);
	}

	/**
	 * Glyphs of one label on one baseline, built while a PDFBox line is written
	 */
	private static final class TextRun
	{
		private final StringBuilder text = new StringBuilder();
		private final float startX;
		private final float baseline;
		private final float fontSize;
		private final String fontName;
		private float endX;
		private float height;

		private TextRun(float startX, float baseline, float fontSize, String fontName)
		{
			this.startX = startX;
			this.baseline = baseline;
			this.fontSize = fontSize;
			this.fontName = fontName;
			this.endX = startX;
		}
	}

	/**
	 * Block under construction with the position of its last element
	 */
	private static final class OpenBlock
	{
		private final TextBlock block;
		private float lastY;
		private float lastHeight;
		private float lastFontSize;

		private OpenBlock(TextBlock block)
		{
			this.block = block;
		}
	}

	/**
	 * Detect columns in the page based on horizontal gaps between elements
	 */
	private List<List<TextElement>> detectColumns(List<TextElement> elements)
	{
		if (elements.isEmpty())
		{
			return new ArrayList<>();
		}

		// Group elements by approximate Y position (same line)
		Map<Integer, List<TextElement>> lineGroups = new HashMap<>();
		float lineThreshold = 5.0f;
		for (TextElement element : elements)
		{
			int lineKey = Math.round(element.getBbox().getY() / lineThreshold);
			lineGroups.computeIfAbsent(lineKey, k -> new ArrayList<>()).add(element);
		}

		// Find column boundaries from lines with multiple elements
		Map<Integer, Integer> boundaryVotes = new HashMap<>();
		for (List<TextElement> lineElements : lineGroups.values())
		{
			if (lineElements.size() < 2)
			{
				continue;
			}

			// Sort line elements by X
			lineElements.sort(Comparator.comparingDouble(e -> e.getBbox().getX()));

			// Look for large gaps between consecutive elements on this line
			for (int i = 1; i < lineElements.size(); i++)
			{
				float prevX = lineElements.get(i - 1).getBbox().getX();
				float currX = lineElements.get(i).getBbox().getX();
				float gap = currX - prevX;

				if (gap > HORIZONTAL_GAP_THRESHOLD * 2)
				{
					// Vote for boundary at midpoint (rounded to nearest 10)
					int boundaryKey = Math.round((prevX + currX) / 2 / 10) * 10;
					boundaryVotes.merge(boundaryKey, 1, Integer::sum);
				}
			}
		}

		// Use boundaries that have multiple votes (appear on multiple lines)
		List<Float> columnBoundaries = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : boundaryVotes.entrySet())
		{
			if (entry.getValue() >= 2)
			{
				columnBoundaries.add((float) entry.getKey());
			}
		}
		columnBoundaries.sort(Float::compareTo);

		// Remove boundaries that are too close together
		List<Float> filteredBoundaries = new ArrayList<>();
		for (Float boundary : columnBoundaries)
		{
			if (filteredBoundaries.isEmpty() ||
				boundary - filteredBoundaries.get(filteredBoundaries.size() - 1) > HORIZONTAL_GAP_THRESHOLD)
			{
				filteredBoundaries.add(boundary);
			}
		}

		// Create column lists
		int numColumns = filteredBoundaries.size() + 1;
		List<List<TextElement>> columns = new ArrayList<>();
		for (int i = 0; i < numColumns; i++)
		{
			columns.add(new ArrayList<>());
		}

		// Assign elements to columns based on their left edge X position
		for (TextElement element : elements)
		{
			float x = element.getBbox().getX();
			int columnIndex = 0;
			for (int i = 0; i < filteredBoundaries.size(); i++)
			{
				if (x > filteredBoundaries.get(i))
				{
					columnIndex = i + 1;
				}
			}
			columns.get(columnIndex).add(element);
		}

		// Sort each column by Y position
		for (List<TextElement> column : columns)
		{
			column.sort(Comparator.comparingDouble(e -> e.getBbox().getY()));
		}

		// Remove empty columns
		columns.removeIf(List::isEmpty);

		return columns;
	}

	/**
	 * Calculate the most common (base) font size in the document
	 */
	private float calculateBaseFontSize(List<TextElement> elements)
	{
		if (elements.isEmpty())
		{
			return 12.0f;
		}

		// Count font size occurrences
		Map<Float, Integer> fontSizeCounts = new HashMap<>();
		for (TextElement element : elements)
		{
			float size = Math.round(element.getFontSize() * 10) / 10.0f;
			fontSizeCounts.merge(size, 1, Integer::sum);
		}

		// Find most common font size
		return fontSizeCounts.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse(12.0f);
	}
}
