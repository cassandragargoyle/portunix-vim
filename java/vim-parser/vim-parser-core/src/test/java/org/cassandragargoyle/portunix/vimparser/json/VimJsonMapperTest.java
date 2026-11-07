/*
 * SPDX-License-Identifier: MIT
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VimJsonMapper
 *
 * @author Zdenek
 * @since 2026-10-26
 */
class VimJsonMapperTest
{
	// Fixtures are owned by vim-parser-api, tests run with the module directory as working directory
	private static final Path API_RESOURCES = Path.of("..", "vim-parser-api", "src", "test", "resources");
	private static final Path PRESCRIPTIVE_FIXTURE = API_RESOURCES.resolve("fixtures/prescriptive-layer-v2.1.vim.json");
	private static final Path EXTRACTION_FIXTURE = API_RESOURCES.resolve("fixtures/extraction-layer-v2.vim.json");
	private static final Path CONTRACT_EXAMPLE = API_RESOURCES.resolve("contract/examples/drawing-definition-file.vim.json");
	private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "org", "cassandragargoyle", "portunix",
		"vimparser", "Main.java");

	private final ObjectMapper tree = new ObjectMapper();
	private final ObjectMapper strict = VimJsonMapper.strictReader();

	@Test
	void numberInStringArrayFails() throws Exception
	{
		assertRejected("references", root -> firstTerm(root).putArray("references").add(1));
	}

	@Test
	void numberInStringFieldFails() throws Exception
	{
		assertRejected("language", root -> definition(root).put("language", 5));
	}

	@Test
	void floatInStringFieldFails() throws Exception
	{
		assertRejected("language", root -> definition(root).put("language", 5.5));
	}

	@Test
	void booleanInStringFieldFails() throws Exception
	{
		assertRejected("language", root -> definition(root).put("language", true));
	}

	@Test
	void arrayInStringFieldFails() throws Exception
	{
		assertRejected("language", root -> definition(root).putArray("language").add("en"));
	}

	@Test
	void objectInStringFieldFails() throws Exception
	{
		assertRejected("language", root -> definition(root).putObject("language").put("code", "en"));
	}

	@Test
	void stringInDoubleFieldFails() throws Exception
	{
		assertRejected("confidence", root -> firstTerm(root).put("confidence", "0.9"));
	}

	@Test
	void emptyStringInDoubleFieldFails() throws Exception
	{
		assertRejected("confidence", root -> firstTerm(root).put("confidence", ""));
	}

	@Test
	void stringInIntegerFieldFails() throws Exception
	{
		assertRejected("page", root -> firstTermAnchor(root).put("page", "2"));
	}

	@Test
	void fractionalNumberInIntegerFieldFails() throws Exception
	{
		assertRejected("page", root -> firstTermAnchor(root).put("page", 2.5));
	}

	@Test
	void fractionalNumberInLongFieldFails() throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(EXTRACTION_FIXTURE.toFile());
		((ObjectNode) root.at("/layers/extraction/sources/0/provenance")).put("duration_ms", 245.5);
		String json = tree.writeValueAsString(root);

		MismatchedInputException e = assertThrows(MismatchedInputException.class,
			() -> strict.readValue(json, VimDocument.class));
		assertTrue(e.getMessage().contains("\"duration_ms\""), e.getMessage());
	}

	@Test
	void integralFloatInIntegerFieldIsAccepted() throws Exception
	{
		assertEquals(2, readTermAnchorPage("2.0"));
		assertEquals(100, readTermAnchorPage("1e2"));
	}

	@Test
	void nanStringInDoubleFieldFails() throws Exception
	{
		assertRejected("confidence", root -> firstTerm(root).put("confidence", "NaN"));
	}

	@Test
	void infinityStringInDoubleFieldFails() throws Exception
	{
		assertRejected("confidence", root -> firstTerm(root).put("confidence", "-Infinity"));
	}

	@Test
	void nanStringInPrimitiveFloatFieldFails()
	{
		assertGraphicRejected("stroke_width", "{\"stroke_width\":\"NaN\"}");
	}

	@Test
	void overflowToInfinityInDoubleFieldFails() throws Exception
	{
		assertRejected("confidence", root -> firstTerm(root).set("confidence", readNumber("1e400")));
	}

	@Test
	void overflowToInfinityInPrimitiveFloatFieldFails()
	{
		assertGraphicRejected("stroke_width", "{\"stroke_width\":1e39}");
	}

	@Test
	void underflowInDoubleFieldIsAcceptedAsZero() throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(PRESCRIPTIVE_FIXTURE.toFile());
		firstTerm(root).set("confidence", readNumber("1e-400"));

		VimDocument doc = strict.treeToValue(root, VimDocument.class);

		assertEquals(0.0, doc.getLayers().getPrescriptive().getTerms().get(0).getConfidence());
	}

	@Test
	void overflowInIntegerFieldFails() throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(PRESCRIPTIVE_FIXTURE.toFile());
		firstTermAnchor(root).set("page", readNumber("12345678901234567890"));
		String json = tree.writeValueAsString(root);

		// Range check of the default deserializer, reported as a general mapping error
		JsonMappingException e = assertThrows(JsonMappingException.class,
			() -> strict.readValue(json, VimDocument.class));
		assertTrue(e.getMessage().contains("\"page\""), e.getMessage());
	}

	@Test
	void integralFloatInLongFieldKeepsPrecision() throws Exception
	{
		String json = Files.readString(EXTRACTION_FIXTURE, StandardCharsets.UTF_8)
			.replace("\"duration_ms\" : 245", "\"duration_ms\" : 9007199254740993.0");

		VimDocument doc = strict.readValue(json, VimDocument.class);

		assertEquals(9007199254740993L,
			doc.getLayers().getExtraction().getSources().get(0).getProvenance().getDurationMs());
	}

	@Test
	void nullInDoubleFieldIsAccepted() throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(PRESCRIPTIVE_FIXTURE.toFile());
		firstTerm(root).putNull("confidence");

		VimDocument doc = strict.treeToValue(root, VimDocument.class);

		assertNull(doc.getLayers().getPrescriptive().getTerms().get(0).getConfidence());
	}

	@Test
	void numberInObjectFieldKeepsItsType() throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(EXTRACTION_FIXTURE.toFile());
		ObjectNode field = (ObjectNode) root.at("/layers/extraction/sources/0/fields/0");
		field.put("value", 5);
		field.put("raw_value", "5");

		JsonNode written = tree.readTree(VimJsonMapper.writer(false)
			.writeValueAsString(strict.treeToValue(root, VimDocument.class)));

		JsonNode writtenField = written.at("/layers/extraction/sources/0/fields/0");
		assertTrue(writtenField.get("value").isInt(), writtenField.toString());
		assertTrue(writtenField.get("raw_value").isTextual(), writtenField.toString());
	}

	@Test
	void overflowInObjectFieldStaysNumber() throws Exception
	{
		String written = readAndWriteExtraction("\"value\" : \"INV-001\"", "\"value\" : 1e400");

		assertTrue(written.contains("\"value\":1E+400"), written);
		assertFalse(written.contains("Infinity"), written);
	}

	@Test
	void overflowInObjectMapStaysNumber() throws Exception
	{
		String written = readAndWriteExtraction("\"psm\" : 6", "\"psm\" : -1e400");

		assertTrue(written.contains("\"psm\":-1E+400"), written);
		assertFalse(written.contains("Infinity"), written);
	}

	@Test
	void decimalInObjectFieldStaysExact() throws Exception
	{
		String written = readAndWriteExtraction("\"value\" : \"INV-001\"", "\"value\" : 0.1");

		assertTrue(written.contains("\"value\":0.1,"), written);
	}

	private String readAndWriteExtraction(String target, String replacement) throws Exception
	{
		String json = Files.readString(EXTRACTION_FIXTURE, StandardCharsets.UTF_8);
		assertTrue(json.contains(target), "Fixture no longer contains " + target);

		VimDocument doc = strict.readValue(json.replace(target, replacement), VimDocument.class);

		return VimJsonMapper.writer(false).writeValueAsString(doc);
	}

	@Test
	void stringInBooleanFieldFails()
	{
		assertGraphicRejected("filled", "{\"filled\":\"true\"}");
	}

	@Test
	void numberInBooleanFieldFails()
	{
		assertGraphicRejected("filled", "{\"filled\":1}");
	}

	private void assertGraphicRejected(String field, String graphic)
	{
		String json = "{\"vim_version\":\"2.1\",\"document\":{\"source\":\"a.pdf\"},"
			+ "\"layers\":{\"physical\":{\"graphics\":[" + graphic + "]}}}";

		MismatchedInputException e = assertThrows(MismatchedInputException.class,
			() -> strict.readValue(json, VimDocument.class));
		assertTrue(e.getMessage().contains("\"" + field + "\""), e.getMessage());
	}

	private JsonNode readNumber(String literal)
	{
		try
		{
			// Exact decimal node, a double node would turn 1e400 into Infinity before the test
			return JsonMapper.builder().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
				.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS).build().readTree(literal);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(literal, e);
		}
	}

	private int readTermAnchorPage(String page) throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(PRESCRIPTIVE_FIXTURE.toFile());
		firstTermAnchor(root).set("page", tree.readTree(page));

		VimDocument doc = strict.treeToValue(root, VimDocument.class);

		return doc.getLayers().getPrescriptive().getTerms().get(0).getAnchors().get(0).getPage();
	}

	@Test
	void unknownPropertiesAreTolerated() throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(PRESCRIPTIVE_FIXTURE.toFile());
		root.put("future_field", 1);

		assertNotNull(strict.treeToValue(root, VimDocument.class));
	}

	@Test
	void prescriptiveFixtureRoundTripsByteEqual() throws Exception
	{
		assertRoundTripsByteEqual(PRESCRIPTIVE_FIXTURE);
	}

	@Test
	void extractionFixtureRoundTripsByteEqual() throws Exception
	{
		assertRoundTripsByteEqual(EXTRACTION_FIXTURE);
	}

	@Test
	void contractExampleReadsAsWithLenientMapper() throws Exception
	{
		// Model derives bbox right/bottom, so the reference is the lenient read, not the input
		String example = Files.readString(CONTRACT_EXAMPLE, StandardCharsets.UTF_8);
		ObjectMapper writer = VimJsonMapper.writer(false);

		String lenientJson = writer.writeValueAsString(tree.readValue(example, VimDocument.class));
		String strictJson = writer.writeValueAsString(strict.readValue(example, VimDocument.class));

		assertEquals(lenientJson, strictJson);
		assertEquals("cs", strict.readValue(example, VimDocument.class)
			.getLayers().getPrescriptive().getDefinition().getLanguage());
	}

	@Test
	void mainUsesSharedMappersOnly() throws Exception
	{
		String source = Files.readString(MAIN_SOURCE, StandardCharsets.UTF_8);

		assertFalse(source.contains("new ObjectMapper("), "Main must create mappers through VimJsonMapper");
		assertFalse(source.contains("JsonMapper.builder("), "Main must create mappers through VimJsonMapper");
	}

	private void assertRejected(String field, Consumer<ObjectNode> mutation) throws Exception
	{
		ObjectNode root = (ObjectNode) tree.readTree(PRESCRIPTIVE_FIXTURE.toFile());
		mutation.accept(root);
		String json = tree.writeValueAsString(root);

		MismatchedInputException e = assertThrows(MismatchedInputException.class,
			() -> strict.readValue(json, VimDocument.class));
		assertTrue(e.getMessage().contains("\"" + field + "\""), e.getMessage());
	}

	private void assertRoundTripsByteEqual(Path fixture) throws Exception
	{
		String expected = Files.readString(fixture, StandardCharsets.UTF_8);

		String json = VimJsonMapper.writer(true)
			.writeValueAsString(strict.readValue(expected, VimDocument.class))
			.replace("\r\n", "\n") + "\n";

		assertEquals(expected, json, "Strict read and write of " + fixture.getFileName() + " changes it");
	}

	private static ObjectNode prescriptive(ObjectNode root)
	{
		return (ObjectNode) root.get("layers").get("prescriptive");
	}

	private static ObjectNode definition(ObjectNode root)
	{
		return (ObjectNode) prescriptive(root).get("definition");
	}

	private static ObjectNode firstTerm(ObjectNode root)
	{
		return (ObjectNode) prescriptive(root).get("terms").get(0);
	}

	private static ObjectNode firstTermAnchor(ObjectNode root)
	{
		return (ObjectNode) firstTerm(root).get("anchors").get(0);
	}
}
