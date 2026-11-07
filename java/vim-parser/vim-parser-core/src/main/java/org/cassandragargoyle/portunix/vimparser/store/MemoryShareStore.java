/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.store;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Class MemoryShareStore
 * @author Zdenek
 * @since 2026-02-03
 */
public class MemoryShareStore
{
	private FileChannel channel;

	private MappedByteBuffer buffer;

	public MemoryShareStore() throws IOException
	{
		channel = FileChannel.open(
			Path.of("shared.mem"),
			StandardOpenOption.CREATE,
			StandardOpenOption.READ,
			StandardOpenOption.WRITE
		);
		buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 1024);
	}

	public void writeInt(int index, int value)
	{
		buffer.putInt(index, value);
	}

	public int readInt(int index)
	{
		// čtení (v jiném procesu)
		return buffer.getInt(index);
	}

	public void writeJson(String json)
	{
		buffer.position(0);
		buffer.put(json.getBytes(StandardCharsets.UTF_8));
		buffer.force();
	}

	public String readJson()
	{
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);

		String json = new String(data, StandardCharsets.UTF_8).trim();
		return json;
	}

}
