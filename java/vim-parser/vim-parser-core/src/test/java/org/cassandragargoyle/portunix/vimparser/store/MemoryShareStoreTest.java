/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.store;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Zdenek
 * @since 2026-02-03
 */
public class MemoryShareStoreTest
{

	@Test
	public void testWriteInt() throws IOException
	{
		System.out.println("writeInt");

		int index = 0;
		int value = 0;
		var instance = new MemoryShareStore();
		instance.writeInt(index, value);
	}

	@Test
	public void testReadInt() throws IOException
	{
		System.out.println("readInt");

		int index = 0;

		var instance1 = new MemoryShareStore();
		var instance2 = new MemoryShareStore();
		int expResult = 10;

		instance1.writeInt(index, 10);
		int result = instance2.readInt(index);

		assertEquals(expResult, result);
	}

	@Test
	public void testReadJson() throws IOException
	{
		String json = "{\"status\":\"ok\"}";

		var instance1 = new MemoryShareStore();
		var instance2 = new MemoryShareStore();

		instance1.writeJson(json);

		String result = instance2.readJson();

		String expResult = json;
		assertEquals(expResult, result);
	}

}
