/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BoundingBox model
 */
class BoundingBoxTest
{
	@Test
	void testConstructor()
	{
		BoundingBox bbox = new BoundingBox(10, 20, 100, 50);

		assertEquals(10, bbox.getX());
		assertEquals(20, bbox.getY());
		assertEquals(100, bbox.getWidth());
		assertEquals(50, bbox.getHeight());
	}

	@Test
	void testGetRight()
	{
		BoundingBox bbox = new BoundingBox(10, 20, 100, 50);
		assertEquals(110, bbox.getRight());
	}

	@Test
	void testGetBottom()
	{
		BoundingBox bbox = new BoundingBox(10, 20, 100, 50);
		assertEquals(70, bbox.getBottom());
	}

	@Test
	void testHorizontallyOverlaps_overlapping()
	{
		BoundingBox bbox1 = new BoundingBox(10, 20, 100, 50);
		BoundingBox bbox2 = new BoundingBox(50, 100, 100, 50);

		assertTrue(bbox1.horizontallyOverlaps(bbox2));
		assertTrue(bbox2.horizontallyOverlaps(bbox1));
	}

	@Test
	void testHorizontallyOverlaps_notOverlapping()
	{
		BoundingBox bbox1 = new BoundingBox(10, 20, 30, 50);
		BoundingBox bbox2 = new BoundingBox(100, 20, 30, 50);

		assertFalse(bbox1.horizontallyOverlaps(bbox2));
		assertFalse(bbox2.horizontallyOverlaps(bbox1));
	}

	@Test
	void testIsInSameColumn()
	{
		// bbox1: x=100, w=50 → center = 125
		BoundingBox bbox1 = new BoundingBox(100, 20, 50, 20);
		// bbox2: x=105, w=40 → center = 125 (same as bbox1)
		BoundingBox bbox2 = new BoundingBox(105, 50, 40, 20);

		assertTrue(bbox1.isInSameColumn(bbox2, 20));
		assertTrue(bbox1.isInSameColumn(bbox2, 5));  // centers are identical

		// bbox3: x=200, w=50 → center = 225 (75 apart from bbox1)
		BoundingBox bbox3 = new BoundingBox(200, 50, 50, 20);
		assertFalse(bbox1.isInSameColumn(bbox3, 50));
	}

	@Test
	void testExpandToInclude()
	{
		BoundingBox bbox1 = new BoundingBox(10, 20, 30, 40);
		BoundingBox bbox2 = new BoundingBox(50, 10, 20, 100);

		bbox1.expandToInclude(bbox2);

		assertEquals(10, bbox1.getX());
		assertEquals(10, bbox1.getY());
		assertEquals(60, bbox1.getWidth());  // 70 - 10
		assertEquals(100, bbox1.getHeight()); // 110 - 10
	}

	@Test
	void testToString()
	{
		BoundingBox bbox = new BoundingBox(10, 20, 100, 50);
		String str = bbox.toString();

		assertTrue(str.contains("BoundingBox"));
		assertTrue(str.contains("x="));
		assertTrue(str.contains("y="));
		assertTrue(str.contains("w="));
		assertTrue(str.contains("h="));
	}
}
