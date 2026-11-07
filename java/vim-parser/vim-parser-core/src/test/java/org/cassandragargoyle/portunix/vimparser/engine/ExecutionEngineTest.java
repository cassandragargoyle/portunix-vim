/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.engine;

import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExecutionEngine
 *
 * @author Zdenek
 * @since 2026-10-11
 */
public class ExecutionEngineTest
{
	private static final float DELTA = 0.001f;

	private ExecutionEngine engine;

	private PluginContext context;

	private VimDocument vimDoc;

	@BeforeEach
	void setUp()
	{
		// Building the document does not touch the plugin manager
		engine = new ExecutionEngine(null);
		context = new PluginContext();
		vimDoc = new VimDocument();
	}

	@Test
	void testTableWithForeignBoundingBoxIsConverted()
	{
		ForeignTable table = new ForeignTable("table-1", new ForeignBoundingBox(10f, 20f, 300f, 150f), List.of(
			new ForeignCell(0, 0, "Item", true, new ForeignBoundingBox(10f, 20f, 150f, 75f)),
			new ForeignCell(1, 1, "42", false, new ForeignBoundingBox(160f, 95f, 150f, 75f))
		));
		context.putSharedData("tables", List.of(table));

		engine.buildVimDocumentFromContext(vimDoc, context);

		List<VimDocument.PhysicalTable> tables = vimDoc.getLayers().getPhysical().getTables();
		assertEquals(1, tables.size());

		VimDocument.PhysicalTable physTable = tables.get(0);
		assertEquals("table-1", physTable.getId());
		assertEquals(1, physTable.getPage());
		assertEquals(2, physTable.getRows());
		assertEquals(2, physTable.getColumns());
		assertEquals(0.9f, physTable.getConfidence(), DELTA);
		assertBbox(10f, 20f, 300f, 150f, physTable.getBbox());

		assertEquals(2, physTable.getCells().size());

		VimDocument.PhysicalTableCell header = physTable.getCells().get(0);
		assertEquals(0, header.getRow());
		assertEquals(0, header.getColumn());
		assertEquals("Item", header.getText());
		assertTrue(header.isHeader());
		assertBbox(10f, 20f, 150f, 75f, header.getBbox());

		VimDocument.PhysicalTableCell value = physTable.getCells().get(1);
		assertEquals(1, value.getRow());
		assertEquals(1, value.getColumn());
		assertEquals("42", value.getText());
		assertFalse(value.isHeader());
		assertBbox(160f, 95f, 150f, 75f, value.getBbox());
	}

	@Test
	void testTableWithoutBoundingBoxIsKept()
	{
		ForeignTable table = new ForeignTable("table-1", null, List.of(
			new ForeignCell(0, 0, "Item", true, null)
		));
		context.putSharedData("tables", List.of(table));

		engine.buildVimDocumentFromContext(vimDoc, context);

		List<VimDocument.PhysicalTable> tables = vimDoc.getLayers().getPhysical().getTables();
		assertEquals(1, tables.size());
		assertNull(tables.get(0).getBbox());
		assertEquals(1, tables.get(0).getCells().size());
		assertNull(tables.get(0).getCells().get(0).getBbox());
	}

	@Test
	void testBrokenTableDoesNotDropOtherTables()
	{
		ForeignTable good = new ForeignTable("table-good", new ForeignBoundingBox(1f, 2f, 3f, 4f), List.of());
		context.putSharedData("tables", List.of(new Object(), good));

		engine.buildVimDocumentFromContext(vimDoc, context);

		List<VimDocument.PhysicalTable> tables = vimDoc.getLayers().getPhysical().getTables();
		assertEquals(1, tables.size());
		assertEquals("table-good", tables.get(0).getId());
	}

	private static void assertBbox(float x, float y, float width, float height, BoundingBox bbox)
	{
		assertNotNull(bbox);
		assertEquals(x, bbox.getX(), DELTA);
		assertEquals(y, bbox.getY(), DELTA);
		assertEquals(width, bbox.getWidth(), DELTA);
		assertEquals(height, bbox.getHeight(), DELTA);
	}

	/**
	 * Stand-in for the bounding box of the table plugin, unrelated to the core model class
	 */
	public static class ForeignBoundingBox
	{
		private final float x;

		private final float y;

		private final float width;

		private final float height;

		public ForeignBoundingBox(float x, float y, float width, float height)
		{
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public float getX()
		{
			return x;
		}

		public float getY()
		{
			return y;
		}

		public float getWidth()
		{
			return width;
		}

		public float getHeight()
		{
			return height;
		}
	}

	/**
	 * Stand-in for the table cell of the table plugin
	 */
	public static class ForeignCell
	{
		private final int row;

		private final int column;

		private final String text;

		private final boolean header;

		private final ForeignBoundingBox boundingBox;

		public ForeignCell(int row, int column, String text, boolean header, ForeignBoundingBox boundingBox)
		{
			this.row = row;
			this.column = column;
			this.text = text;
			this.header = header;
			this.boundingBox = boundingBox;
		}

		public int getRow()
		{
			return row;
		}

		public int getColumn()
		{
			return column;
		}

		public String getText()
		{
			return text;
		}

		public boolean isHeader()
		{
			return header;
		}

		public ForeignBoundingBox getBoundingBox()
		{
			return boundingBox;
		}
	}

	/**
	 * Stand-in for the table model of the table plugin
	 */
	public static class ForeignTable
	{
		private final String id;

		private final ForeignBoundingBox boundingBox;

		private final List<ForeignCell> cells;

		public ForeignTable(String id, ForeignBoundingBox boundingBox, List<ForeignCell> cells)
		{
			this.id = id;
			this.boundingBox = boundingBox;
			this.cells = cells;
		}

		public String getId()
		{
			return id;
		}

		public int getPage()
		{
			return 1;
		}

		public int getRowCount()
		{
			return 2;
		}

		public int getColumnCount()
		{
			return 2;
		}

		public float getConfidence()
		{
			return 0.9f;
		}

		public ForeignBoundingBox getBoundingBox()
		{
			return boundingBox;
		}

		public List<ForeignCell> getCells()
		{
			return cells;
		}
	}
}
