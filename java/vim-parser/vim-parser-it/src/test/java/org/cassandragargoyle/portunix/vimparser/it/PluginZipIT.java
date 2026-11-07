/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.it;

import org.cassandragargoyle.portunix.vimparser.core.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying that the packaged table plugin ZIP exposes its extension.
 *
 * <p>The table plugin bundles dependency JARs that carry their own META-INF/extensions.idx.
 * PF4J reads only the first index it finds, so the ZIP has to put the index of the plugin
 * itself on the classes path, which is searched before lib/.
 */
class PluginZipIT
{
	private static final String ZIP_PROPERTY = "vimparser.table.plugin.zip";

	private static final String OWN_INDEX = "classes/META-INF/extensions.idx";

	private static final String TABLE_EXTENSION = "org.cassandragargoyle.portunix.vimparser.table.TableExtractorPlugin";

	@Test
	void tablePluginZipCarriesOwnExtensionIndexOnClassesPath() throws IOException
	{
		try (ZipFile zip = new ZipFile(tablePluginZip().toFile()))
		{
			ZipEntry entry = zip.getEntry(OWN_INDEX);
			assertNotNull(entry, OWN_INDEX + " missing in " + zip.getName());

			try (InputStream in = zip.getInputStream(entry))
			{
				String index = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				assertTrue(index.contains(TABLE_EXTENSION), "table extension not listed in " + OWN_INDEX);
			}
		}
	}

	@Test
	void tableExtractorIsDiscoveredFromZip(@TempDir Path pluginsDir) throws IOException
	{
		Path zip = tablePluginZip();
		Files.copy(zip, pluginsDir.resolve(zip.getFileName()));

		PluginManager manager = new PluginManager(pluginsDir);
		try
		{
			manager.discoverPlugins();

			assertNotNull(manager.getExtractorPlugin("table"), "table extractor not discovered from ZIP");
		}
		finally
		{
			manager.shutdown();
		}
	}

	private static Path tablePluginZip()
	{
		String location = System.getProperty(ZIP_PROPERTY);
		assertNotNull(location, "system property " + ZIP_PROPERTY + " not set");

		Path zip = Paths.get(location);
		assertTrue(Files.isRegularFile(zip), "table plugin ZIP not found: " + zip);
		return zip;
	}
}
