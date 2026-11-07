/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cassandragargoyle.portunix.vimparser.store.ZipStore.ZipWriter;
import org.junit.jupiter.api.Test;

/**
 * Class ZipStoreTest
 * @author Zdenek
 */
public class ZipStoreTest
{
	@Test
	public void OpenWriterTest() throws IOException
	{
		System.out.println("openWriter");

		Path tempZip = Files.createTempFile("data-", ".zip");
		ZipStore store = new ZipStore(tempZip);

		try (ZipWriter writer = store.openWriter("data/abc.ndjson"))
		{
			// writing JSON-ND (line by line)
			writer.write("{\"id\":1,\"name\":\"Alice\"}");
			writer.newLine();
			writer.write("{\"id\":2,\"name\":\"Bob\"}");
			writer.newLine();
		}
	}
}
