/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.store;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.cassandragargoyle.portunix.vimparser.api.VimStore;

/**
 * Note:
 * What if you use ZipInputStream over network?
 *
 * ⚠️ This is a big difference:
 *
 * ZipInputStream:
 * reads ZIP from the beginning must iterate through all entries before the correct one over network = disaster
 * ➡️ for network drives always use ZipFile, never ZipInputStream.
 *
 * @author Zdenek
 * @since 2026-02-02
 */
public class ZipStore implements VimStore
{
	Path zipPath;

	public ZipStore(String zipPath)
	{
		this.zipPath = Path.of(zipPath);
	}

	public ZipStore(Path zipPath)
	{
		this.zipPath = zipPath;
	}

	public ZipWriter openWriter(String entryName) throws IOException
	{
		return new ZipWriter(entryName);
	}

	public ZipReader openReader(String entryName) throws IOException
	{
		return new ZipReader(entryName);
	}

	/**
	 * Entry is file TODO: Zkontrolovat a případně doplnit popis.
	 * @param entryName
	 * @throws IOException
	 */
	public void ReadOneEntryFromZip(String entryName) throws IOException
	{
		try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8))
		{
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null)
			{
				throw new FileNotFoundException("Not found in ZIP file: " + entryName);
			}

			try (InputStream is = zip.getInputStream(entry); BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = br.readLine()) != null)
				{
					// line = jeden JSON objekt (JSON-ND)
					System.out.println(line);
				}
			}
		}
	}

	public final class ZipWriter implements AutoCloseable
	{
		private final ZipOutputStream zipOutputStream;

		private final BufferedWriter writer;

		public ZipWriter(String entryName) throws IOException
		{
			OutputStream fos = Files.newOutputStream(zipPath);
			this.zipOutputStream = new ZipOutputStream(fos, StandardCharsets.UTF_8);

			ZipEntry entry = new ZipEntry(entryName);
			zipOutputStream.putNextEntry(entry);

			this.writer = new BufferedWriter(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8));
		}

		public void write(String text) throws IOException
		{
			writer.write(text);
		}

		public void newLine() throws IOException
		{
			writer.newLine();
		}

		public void writeLine(String line) throws IOException
		{
			writer.write(line);
			writer.newLine();
		}

		@Override
		public void close() throws IOException
		{
			// Ensure buffered data are flushed
			writer.flush();

			// Finish ZIP entry
			zipOutputStream.closeEntry();

			// Close ZIP (also closes underlying streams)
			zipOutputStream.close();
		}
	}

	/**
	 * TODO:
	 */
	public final class ZipReader implements AutoCloseable
	{
		public ZipReader(String entryName) throws IOException
		{

		}

		@Override
		public void close() throws Exception
		{
			throw new UnsupportedOperationException("ZipReader.close not supported.");
		}
	}
}
