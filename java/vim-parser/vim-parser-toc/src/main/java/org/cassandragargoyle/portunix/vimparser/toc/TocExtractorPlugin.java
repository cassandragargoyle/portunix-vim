/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.toc;

import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.TableOfContents;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;
import org.cassandragargoyle.portunix.vimparser.model.TocEntry;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TOC extraction plugin - detects chapters/headings and maps them to page numbers.
 * Combines two detection strategies:
 * 1. Explicit TOC page detection (parsing printed TOC pages like "Obsah")
 * 2. Heuristic heading detection (font size, weight, position analysis)
 *
 * Explicit entries take priority and are used to validate heuristic results.
 *
 * @author Zdenek
 * @since 2026-03-05
 */
@Extension
public class TocExtractorPlugin implements ExtractorPlugin, OrchestrationAware
{
	private static final Logger LOG = LogFactory.getLogger(TocExtractorPlugin.class);

	public static final String PLUGIN_ID = "toc";
	public static final String RESULT_KEY_TOC = "toc";
	public static final String RESULT_KEY_TOC_BLOCK_IDS = "tocBlockIds";

	private final HeadingDetector headingDetector;
	private final TocPageDetector tocPageDetector;

	public TocExtractorPlugin()
	{
		this.headingDetector = new HeadingDetector();
		this.tocPageDetector = new TocPageDetector();
	}

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "TOC Extractor";
	}

	@Override
	public String getVersion()
	{
		return "0.2.0";
	}

	@Override
	public int getPriority()
	{
		return 50;  // After fulltext (10) and barcode (20)
	}

	@Override
	public Set<String> getDependencies()
	{
		return Set.of("fulltext");
	}

	@Override
	public Set<String> getPredecessors()
	{
		return Set.of("fulltext");
	}

	@Override
	public boolean isAutoEnabled()
	{
		return true;
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.TRANSFORM;
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
	@SuppressWarnings("unchecked")
	public PluginResult extract(PluginContext context)
	{
		long startTime = System.currentTimeMillis();

		try
		{
			List<TextBlock> blocks = context.getSharedData("textBlocks", List.class);
			if (blocks == null || blocks.isEmpty())
			{
				if (context.isVerbose())
				{
					LOG.log(Level.INFO, "No text blocks available, skipping TOC extraction");
				}
				return PluginResult.success(new TableOfContents());
			}

			TableOfContents toc = new TableOfContents();

			// Phase 1: Detect explicit TOC page
			TocPageDetector.TocPageResult explicitResult = tocPageDetector.detect(blocks);
			List<TocEntry> explicitEntries = new ArrayList<>();

			if (explicitResult != null)
			{
				explicitEntries = explicitResult.getEntries();
				toc.setTocPage(explicitResult.getTocPage());

				// Mark TOC blocks for type annotation in structural layer
				context.putSharedData(RESULT_KEY_TOC_BLOCK_IDS, explicitResult.getTocBlockIds());

				// Mark source blocks as TOC type (only on the TOC page)
				int tocPage = explicitResult.getTocPage();
				for (String blockId : explicitResult.getTocBlockIds())
				{
					for (TextBlock block : blocks)
					{
						if (block.getPage() == tocPage && block.getId().equals(blockId))
						{
							block.setType("toc");
						}
					}
				}

				if (context.isVerbose())
				{
					LOG.log(Level.INFO, "Explicit TOC found on page {0}: {1} entries",
							LogFactory.args(explicitResult.getTocPage(), explicitEntries.size()));
				}
			}

			// Phase 2: Heuristic heading detection
			List<TocEntry> heuristicEntries = headingDetector.detect(blocks);

			// Phase 3: Merge results - explicit entries take priority
			List<TocEntry> mergedEntries = mergeEntries(explicitEntries, heuristicEntries);
			toc.setEntries(mergedEntries);

			// Store in context for integration into vim.json
			context.putSharedData(RESULT_KEY_TOC, toc);

			if (context.isVerbose())
			{
				LOG.log(Level.INFO, "TOC extraction: {0} entries ({1} explicit, {2} heuristic)",
						LogFactory.args(mergedEntries.size(), explicitEntries.size(),
								mergedEntries.size() - explicitEntries.size()));
			}

			PluginResult result = PluginResult.success(toc);
			result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			result.addMetadata("entry_count", String.valueOf(mergedEntries.size()));
			if (explicitResult != null)
			{
				result.addMetadata("toc_page", String.valueOf(explicitResult.getTocPage()));
				result.addMetadata("explicit_entries", String.valueOf(explicitEntries.size()));
			}

			return result;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Error extracting TOC: {0}", e.getMessage());
			return PluginResult.failure("TOC extraction failed: " + e.getMessage());
		}
	}

	/**
	 * Merge explicit and heuristic TOC entries.
	 * Explicit entries are primary. Heuristic entries that match an explicit entry
	 * get their confidence boosted. Non-matching heuristic entries are kept as supplementary.
	 */
	List<TocEntry> mergeEntries(List<TocEntry> explicitEntries, List<TocEntry> heuristicEntries)
	{
		if (explicitEntries.isEmpty())
		{
			return heuristicEntries;
		}

		if (heuristicEntries.isEmpty())
		{
			return explicitEntries;
		}

		// Build explicit entries map by target page for cross-reference
		Map<Integer, TocEntry> explicitByPage = new LinkedHashMap<>();
		for (TocEntry entry : explicitEntries)
		{
			// Use first entry per page (primary chapter)
			explicitByPage.putIfAbsent(entry.getPage(), entry);
		}

		// Start with explicit entries
		List<TocEntry> merged = new ArrayList<>(explicitEntries);

		// Add heuristic entries that don't overlap with explicit ones
		for (TocEntry heuristic : heuristicEntries)
		{
			TocEntry explicit = explicitByPage.get(heuristic.getPage());
			if (explicit != null && titlesMatch(explicit.getTitle(), heuristic.getTitle()))
			{
				// Matching entry found - boost explicit entry confidence using heuristic's block_id
				explicit.setBlockId(heuristic.getBlockId());
				explicit.setConfidence(Math.min(1.0, explicit.getConfidence() + 0.05));
			}
			else if (!hasOverlappingTitle(merged, heuristic))
			{
				// No overlap - add as supplementary heuristic entry
				merged.add(heuristic);
			}
		}

		// Sort by page
		merged.sort((a, b) -> Integer.compare(a.getPage(), b.getPage()));

		return merged;
	}

	/**
	 * Check if two titles refer to the same heading (fuzzy match)
	 */
	private boolean titlesMatch(String a, String b)
	{
		if (a == null || b == null)
		{
			return false;
		}

		String normalA = a.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
		String normalB = b.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();

		return normalA.equals(normalB) || normalA.contains(normalB) || normalB.contains(normalA);
	}

	/**
	 * Check if any existing entry has a matching title
	 */
	private boolean hasOverlappingTitle(List<TocEntry> entries, TocEntry candidate)
	{
		for (TocEntry entry : entries)
		{
			if (titlesMatch(entry.getTitle(), candidate.getTitle()))
			{
				return true;
			}
		}
		return false;
	}
}
