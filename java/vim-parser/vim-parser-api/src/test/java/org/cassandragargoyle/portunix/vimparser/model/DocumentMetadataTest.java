/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for DocumentMetadata model
 */
class DocumentMetadataTest
{
	private static final String SCHEMA = "vim-document.schema.json";
	private static final String EXTRACTION_DATE = "2026-01-01T00:00:00Z";

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void documentWithExtractionDateLoads() throws Exception
	{
		String json = "{\"vim_version\":\"2.0\",\"document\":{\"source\":\"a.pdf\","
			+ "\"metadata\":{\"title\":\"t\",\"extraction_date\":\"" + EXTRACTION_DATE + "\"}},\"layers\":{}}";

		VimDocument doc = mapper.readValue(json, VimDocument.class);

		assertEquals("t", doc.getDocument().getMetadata().getTitle());
		assertEquals(EXTRACTION_DATE, doc.getDocument().getMetadata().getExtractionDate());
	}

	@Test
	void extractionDateRoundTrips() throws Exception
	{
		String json = "{\"dateModified\":\"2025-12-31T00:00:00Z\",\"extraction_date\":\"" + EXTRACTION_DATE
			+ "\",\"sha256\":\"abc\",\"title\":\"t\"}";

		DocumentMetadata metadata = mapper.readValue(json, DocumentMetadata.class);

		assertEquals(EXTRACTION_DATE, metadata.getExtractionDate());
		assertEquals(json, mapper.writeValueAsString(metadata));
	}

	@Test
	void outputWithoutExtractionDateIsUnchanged() throws Exception
	{
		String json = "{\"encodingFormat\":\"application/pdf\",\"contentSize\":42,"
			+ "\"dateCreated\":\"2025-01-01T00:00:00Z\",\"dateModified\":\"2025-12-31T00:00:00Z\","
			+ "\"sha256\":\"abc\",\"inLanguage\":\"cs\",\"creator\":\"c\",\"producer\":\"p\",\"title\":\"t\","
			+ "\"additionalProperties\":{\"k\":\"v\"}}";

		DocumentMetadata metadata = mapper.readValue(json, DocumentMetadata.class);

		assertNull(metadata.getExtractionDate());
		assertEquals(json, mapper.writeValueAsString(metadata));
	}

	@Test
	void unknownPropertyIsIgnored() throws Exception
	{
		DocumentMetadata metadata = mapper.readValue("{\"title\":\"t\",\"future_key\":1}", DocumentMetadata.class);

		assertEquals("t", metadata.getTitle());
		assertFalse(mapper.valueToTree(metadata).has("future_key"));
	}

	@Test
	void propertyNamesEqualSchemaProperties() throws Exception
	{
		JsonNode schema;
		try (InputStream in = ContractSchemas.open(SCHEMA))
		{
			schema = mapper.readTree(in);
		}
		Set<String> expected = new TreeSet<>();
		schema.get("$defs").get("DocumentMetadata").get("properties").fieldNames().forEachRemaining(expected::add);

		Set<String> actual = new TreeSet<>();
		mapper.getSerializationConfig().introspect(mapper.constructType(DocumentMetadata.class))
			.findProperties().forEach(property -> actual.add(property.getName()));

		assertEquals(expected, actual);
	}
}
