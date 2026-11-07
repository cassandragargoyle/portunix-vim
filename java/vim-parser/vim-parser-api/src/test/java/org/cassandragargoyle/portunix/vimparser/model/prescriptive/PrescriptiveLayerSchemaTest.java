/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.cassandragargoyle.portunix.vimparser.model.ContractSchemas;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests: JSON written by the model must validate against the
 * contract schemas in src/test/resources/contract/schemas
 */
class PrescriptiveLayerSchemaTest
{
	private static final String SCHEMA_BASE = "https://cassandragargoyle.org/schemas/";
	private static final String FIXTURE = "fixtures/prescriptive-layer-v2.1.vim.json";
	private static final String CONTRACT_EXAMPLE = "contract/examples/drawing-definition-file.vim.json";

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
	void fullPrescriptiveLayerValidatesAgainstDocumentSchema()
	{
		JsonNode json = mapper.valueToTree(PrescriptiveLayerSamples.fullV21Document());

		assertValid(json);
	}

	@Test
	void emptyPrescriptiveLayerValidates()
	{
		VimDocument doc = new VimDocument();
		doc.setVimVersion("2.1");
		doc.getDocument().setSource("a.pdf");
		doc.getLayers().setPrescriptive(new PrescriptiveLayer());

		assertValid(mapper.valueToTree(doc));
	}

	@Test
	void camelCaseKeysAreRejectedBySchema() throws Exception
	{
		JsonNode json = mapper.readTree("{\"vim_version\":\"2.1\",\"document\":{\"source\":\"a.pdf\"},"
			+ "\"layers\":{\"prescriptive\":{\"provisions\":[{\"provisionId\":\"p-1\"}]}}}");

		assertTrue(!documentSchema.validate(json).isEmpty(), "schema must reject camelCase keys");
	}

	@Test
	void goldenFixtureMatchesModelOutput() throws Exception
	{
		String json = mapper.writeValueAsString(PrescriptiveLayerSamples.fullV21Document())
			.replace("\r\n", "\n") + "\n";

		if (Boolean.getBoolean(UPDATE_PROPERTY))
		{
			Path target = Path.of("src", "test", "resources").resolve(FIXTURE);
			Files.createDirectories(target.getParent());
			Files.writeString(target, json, StandardCharsets.UTF_8);
			return;
		}

		String fixture = readFixture();

		assertValid(mapper.readTree(fixture));
		assertEquals(fixture, json, "Fixture is stale, run with -D" + UPDATE_PROPERTY + "=true");
	}

	@Test
	void goldenFixtureRoundTripsByteEqual() throws Exception
	{
		String fixture = readFixture();

		String json = mapper.writeValueAsString(mapper.readValue(fixture, VimDocument.class))
			.replace("\r\n", "\n") + "\n";

		assertEquals(fixture, json, "Read and write of the fixture changes it");
	}

	@Test
	void contractExampleRoundTripsWithoutLoss() throws Exception
	{
		JsonNode example;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONTRACT_EXAMPLE))
		{
			assertNotNull(in, "Missing contract example " + CONTRACT_EXAMPLE);
			example = mapper.readTree(in);
		}
		assertValid(example);

		VimDocument doc = mapper.treeToValue(example, VimDocument.class);
		PrescriptiveLayer layer = doc.getLayers().getPrescriptive();
		assertEquals("cs", layer.getDefinition().getLanguage());
		assertTrue(layer.getTerms().stream().anyMatch(term -> !term.getReferences().isEmpty()),
			"example must have a term with references");

		// Parsed from text so that float fields compare equal to the parsed example
		JsonNode written = mapper.readTree(mapper.writeValueAsString(layer));
		// BoundingBox adds the computed right and bottom, which the example leaves out
		for (JsonNode bbox : written.findValues("bbox"))
		{
			((ObjectNode) bbox).remove(List.of("right", "bottom"));
		}
		assertEquals(example.at("/layers/prescriptive"), written,
			"Read and write of the contract example changes the prescriptive layer");
	}

	private String readFixture() throws Exception
	{
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(FIXTURE))
		{
			assertNotNull(in, "Missing fixture, run with -D" + UPDATE_PROPERTY + "=true");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
		}
	}

	private static void assertValid(JsonNode json)
	{
		Set<ValidationMessage> errors = documentSchema.validate(json);

		assertTrue(errors.isEmpty(), "Schema violations: " + errors);
	}
}
