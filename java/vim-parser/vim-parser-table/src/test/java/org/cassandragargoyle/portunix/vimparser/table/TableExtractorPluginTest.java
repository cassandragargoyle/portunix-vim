/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.table;

import org.cassandragargoyle.portunix.tabledetector.model.TableCell;
import org.cassandragargoyle.portunix.tabledetector.model.TableModel;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TableExtractorPlugin
 *
 * @author Zdenek
 * @since 2026-01-31
 */
class TableExtractorPluginTest
{
	private TableExtractorPlugin plugin;

	@BeforeEach
	void setUp()
	{
		plugin = new TableExtractorPlugin();
		plugin.initialize();
	}

	@Test
	void testPluginMetadata()
	{
		assertEquals("table", plugin.getId());
		assertEquals("Table Extractor", plugin.getName());
		assertEquals("0.2.0", plugin.getVersion());
		assertEquals(30, plugin.getPriority());
	}

	@Test
	void testSupportedModes()
	{
		ExecutionMode[] modes = plugin.getSupportedModes();
		assertNotNull(modes);
		assertEquals(2, modes.length);
		assertEquals(ExecutionMode.SYNC, modes[0]);
		assertEquals(ExecutionMode.ASYNC_THREAD, modes[1]);
	}

	@Test
	void testOrchestrationAware()
	{
		assertTrue(plugin.isAutoEnabled());
		assertEquals(ExecutionPhase.EXTRACT, plugin.getPhase());
		assertTrue(plugin.getPredecessors().isEmpty());
		assertTrue(plugin.getDependencies().isEmpty());
	}

	@Test
	void testTableModelBasics()
	{
		TableModel table = new TableModel("test-table", 1);
		table.setRowCount(3);
		table.setColumnCount(2);
		table.setConfidence(0.85f);
		table.setHasBorders(true);

		assertEquals("test-table", table.getId());
		assertEquals(1, table.getPage());
		assertEquals(3, table.getRowCount());
		assertEquals(2, table.getColumnCount());
		assertEquals(0.85f, table.getConfidence(), 0.01f);
		assertTrue(table.isHasBorders());
	}

	@Test
	void testTableCellBasics()
	{
		TableCell cell = new TableCell(0, 1, "Header");
		cell.setHeader(true);
		cell.setRowSpan(2);
		cell.setColSpan(1);

		assertEquals(0, cell.getRow());
		assertEquals(1, cell.getColumn());
		assertEquals("Header", cell.getText());
		assertTrue(cell.isHeader());
		assertEquals(2, cell.getRowSpan());
		assertEquals(1, cell.getColSpan());
	}

	@Test
	void testTableAddCell()
	{
		TableModel table = new TableModel("t1", 1);
		TableCell cell1 = new TableCell(0, 0, "A");
		TableCell cell2 = new TableCell(0, 1, "B");

		table.addCell(cell1);
		table.addCell(cell2);

		assertEquals(2, table.getCells().size());
		assertEquals(cell1, table.getCell(0, 0));
		assertEquals(cell2, table.getCell(0, 1));
		assertNull(table.getCell(1, 0));
	}
}
