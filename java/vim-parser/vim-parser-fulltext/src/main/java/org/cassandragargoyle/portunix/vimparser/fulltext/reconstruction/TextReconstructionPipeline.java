/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.fulltext.reconstruction;

import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.TextReconstructionTransform;
import org.cassandragargoyle.portunix.vimparser.model.TextBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pipeline for Layer 1.5 text reconstruction transforms
 *
 * Runs registered transforms sequentially on text blocks after physical
 * extraction and before structural classification.
 *
 * @author Zdenek
 * @since 2026-02-24
 */
public class TextReconstructionPipeline
{
	private static final Logger LOG = LogFactory.getLogger(TextReconstructionPipeline.class);

	private final List<TextReconstructionTransform> transforms = new ArrayList<>();

	/**
	 * Register a transform to the pipeline
	 */
	public void addTransform(TextReconstructionTransform transform)
	{
		transforms.add(transform);
	}

	/**
	 * Run all registered transforms on the given blocks
	 *
	 * @param blocks text blocks to process (modified in-place)
	 * @param verbose enable verbose logging
	 * @return total number of modifications made
	 */
	public int process(List<TextBlock> blocks, boolean verbose)
	{
		if (blocks == null || blocks.isEmpty())
		{
			return 0;
		}

		int totalModifications = 0;

		for (TextReconstructionTransform transform : transforms)
		{
			if (verbose)
			{
				LOG.log(Level.INFO, "Running text reconstruction transform: {0}", transform.getName());
			}

			int modifications = transform.apply(blocks);
			totalModifications += modifications;

			if (verbose)
			{
				LOG.log(Level.INFO, "  Transform {0}: {1} modification(s)",
					LogFactory.args(transform.getId(), modifications));
			}
		}

		return totalModifications;
	}

	/**
	 * Get the number of registered transforms
	 */
	public int getTransformCount()
	{
		return transforms.size();
	}
}
