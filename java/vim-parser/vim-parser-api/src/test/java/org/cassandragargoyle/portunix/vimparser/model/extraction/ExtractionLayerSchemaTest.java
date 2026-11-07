/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.cassandragargoyle.portunix.vimparser.model.ContractSchemas;
import org.cassandragargoyle.portunix.vimparser.model.DocumentMetadata;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests: JSON written by the model must validate against the
 * contract schemas in src/test/resources/contract/schemas
 */
class ExtractionLayerSchemaTest
{
	private static final String SCHEMA_BASE = "https://cassandragargoyle.org/schemas/";
	private static final String FIXTURE = "fixtures/extraction-layer-v2.vim.json";

	// Set -Dvim.fixture.update=true to regenerate the golden fixture from the model
	private static final String UPDATE_PROPERTY = "vim.fixture.update";

	private static JsonSchema documentSchema;
	private static ObjectMapper mapper;

	@BeforeAll
	static void loadSchema()
	{
		String schemaLocation = ContractSchemas.location();
		JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
			builder -> builder.schemaMappers(mappers -> mappers.mapPrefix(SCHEMA_BASE, schemaLocation)));
		documentSchema = factory.getSchema(SchemaLocation.of(SCHEMA_BASE + "vim-document.schema.json"));
		// Same settings as the production mapper, only with pretty printing
		mapper = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.build();
	}

	@Test
	void fullExtractionLayerValidatesAgainstDocumentSchema()
	{
		JsonNode json = mapper.valueToTree(ExtractionLayerSamples.fullV2Document());

		assertValid(json);
	}

	@Test
	void documentWithoutExtractionLayerValidates()
	{
		VimDocument doc = new VimDocument();
		doc.setVimVersion("1.5");
		doc.getDocument().setSource("plain.pdf");
		doc.getDocument().setPages(1);

		JsonNode json = mapper.valueToTree(doc);

		assertTrue(!json.get("layers").has("extraction"), "extraction layer must be omitted");
		assertValid(json);
	}

	@Test
	void metadataWithExtractionDateValidates()
	{
		VimDocument doc = new VimDocument();
		doc.setVimVersion("2.0");
		doc.getDocument().setSource("a.pdf");
		DocumentMetadata metadata = new DocumentMetadata();
		metadata.setTitle("t");
		metadata.setExtractionDate("2026-01-01T00:00:00Z");
		doc.getDocument().setMetadata(metadata);

		JsonNode json = mapper.valueToTree(doc);

		assertEquals("2026-01-01T00:00:00Z",
			json.get("document").get("metadata").get("extraction_date").asText());
		assertValid(json);
	}

	@Test
	void camelCaseKeysAreRejectedBySchema() throws Exception
	{
		JsonNode json = mapper.readTree("{\"vim_version\":\"2.0\",\"document\":{\"source\":\"a.pdf\"},"
			+ "\"layers\":{\"extraction\":{\"sources\":[{\"sourceId\":\"s1\"}]}}}");

		assertTrue(!documentSchema.validate(json).isEmpty(), "schema must reject camelCase keys");
	}

	@Test
	void goldenFixtureMatchesModelOutput() throws Exception
	{
		String json = mapper.writeValueAsString(ExtractionLayerSamples.fullV2Document())
			.replace("\r\n", "\n") + "\n";

		if (Boolean.getBoolean(UPDATE_PROPERTY))
		{
			Path target = Path.of("src", "test", "resources").resolve(FIXTURE);
			Files.createDirectories(target.getParent());
			Files.writeString(target, json, StandardCharsets.UTF_8);
			return;
		}

		String fixture;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(FIXTURE))
		{
			assertNotNull(in, "Missing fixture, run with -D" + UPDATE_PROPERTY + "=true");
			fixture = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
		}

		assertValid(mapper.readTree(fixture));
		assertEquals(fixture, json, "Fixture is stale, run with -D" + UPDATE_PROPERTY + "=true");
	}

	private static void assertValid(JsonNode json)
	{
		Set<ValidationMessage> errors = documentSchema.validate(json);

		assertTrue(errors.isEmpty(), "Schema violations: " + errors);
	}
}
