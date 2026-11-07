/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import org.cassandragargoyle.portunix.vimparser.model.TextBlock;

import java.util.List;

/**
 * Interface for Layer 1.5 text reconstruction transforms
 *
 * Transforms operate on text blocks after physical extraction (Layer 1)
 * and before structural classification (Layer 2). Each transform addresses
 * a specific text layout phenomenon (drop caps, hyphenation, pull quotes, etc.)
 *
 * @author Zdenek
 * @since 2026-02-24
 */
public interface TextReconstructionTransform
{
	/**
	 * Unique identifier for this transform
	 */
	String getId();

	/**
	 * Human-readable name
	 */
	String getName();

	/**
	 * Apply the transform to all text blocks (in-place modification)
	 *
	 * @param blocks list of text blocks to process
	 * @return number of modifications made
	 */
	int apply(List<TextBlock> blocks);
}
