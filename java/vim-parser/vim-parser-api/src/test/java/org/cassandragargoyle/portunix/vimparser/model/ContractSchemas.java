/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Location of the contract schemas used by the tests: the canonical schemas
 * of the api repository when PTX_CONTRACT_SCHEMAS is set, the committed
 * copies in src/test/resources/contract/schemas otherwise
 */
public final class ContractSchemas
{
	public static final String DIR_VARIABLE = "PTX_CONTRACT_SCHEMAS";

	private static final String CLASSPATH_DIR = "contract/schemas/";

	private ContractSchemas()
	{
	}

	// Prefix that the schema ids are mapped to, with a trailing slash
	public static String location()
	{
		Path dir = directory();
		return dir == null ? "classpath:" + CLASSPATH_DIR : dir.toUri().toString();
	}

	public static InputStream open(String fileName) throws IOException
	{
		Path dir = directory();
		if (dir != null)
		{
			return Files.newInputStream(dir.resolve(fileName));
		}
		InputStream in = ContractSchemas.class.getClassLoader().getResourceAsStream(CLASSPATH_DIR + fileName);
		assertNotNull(in, "Missing schema " + CLASSPATH_DIR + fileName);
		return in;
	}

	private static Path directory()
	{
		String dir = System.getenv(DIR_VARIABLE);
		if (dir == null || dir.isBlank())
		{
			return null;
		}
		Path path = Path.of(dir);
		assertTrue(Files.isDirectory(path), DIR_VARIABLE + " is not a directory: " + dir);
		return path;
	}
}
