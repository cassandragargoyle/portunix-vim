/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Backward-compatibility tests for VimDocument across VIM v1.5 and v2.0
 */
class VimDocumentBackwardCompatTest
{
	private ObjectMapper mapper;

	@BeforeEach
	void setUp()
	{
		mapper = new ObjectMapper();
	}

	@Test
	void v15DocumentParsesWithoutExtraction() throws Exception
	{
		String v15Json = "{"
			+ "\"vim_version\":\"1.5\","
			+ "\"document\":{\"source\":\"a.pdf\",\"pages\":3},"
			+ "\"layers\":{\"physical\":{\"blocks\":[]},\"structural\":{\"blocks\":[]},\"semantic\":{\"entities\":[]}}"
			+ "}";

		VimDocument doc = mapper.readValue(v15Json, VimDocument.class);

		assertEquals("1.5", doc.getVimVersion());
		assertEquals(3, doc.getDocument().getPages());
		assertEquals("a.pdf", doc.getDocument().getSource());
		assertNull(doc.getLayers().getExtraction(), "v1.5 must not have extraction layer");
		assertNotNull(doc.getLayers().getPhysical());
	}

	@Test
	void v20DocumentWithoutExtractionParses() throws Exception
	{
		String v20Json = "{"
			+ "\"vim_version\":\"2.0\","
			+ "\"document\":{\"source\":\"b.pdf\",\"pages\":1},"
			+ "\"layers\":{\"physical\":{\"blocks\":[]},\"structural\":{\"blocks\":[]},\"semantic\":{\"entities\":[]}}"
			+ "}";

		VimDocument doc = mapper.readValue(v20Json, VimDocument.class);

		assertEquals("2.0", doc.getVimVersion());
		assertEquals(1, doc.getDocument().getPages());
		assertNull(doc.getLayers().getExtraction(), "v2.0 without extraction must parse");
	}

	@Test
	void v20DocumentWithNullPagesParses() throws Exception
	{
		String v20Json = "{"
			+ "\"vim_version\":\"2.0\","
			+ "\"document\":{\"source\":\"scan.png\"},"
			+ "\"layers\":{\"physical\":{\"blocks\":[]},\"structural\":{\"blocks\":[]},\"semantic\":{\"entities\":[]}}"
			+ "}";

		VimDocument doc = mapper.readValue(v20Json, VimDocument.class);

		assertEquals("2.0", doc.getVimVersion());
		assertNull(doc.getDocument().getPages(), "pages must be nullable in v2.0");
	}

	@Test
	void v15OutputUnchangedWhenExtractionAbsent() throws Exception
	{
		VimDocument doc = new VimDocument();
		doc.getDocument().setSource("c.pdf");
		doc.getDocument().setPages(5);

		String json = mapper.writeValueAsString(doc);

		// extraction layer must NOT appear when null
		assertEquals(false, json.contains("\"extraction\""),
			"extraction should be omitted when null: " + json);
		// sources should not appear when empty
		assertEquals(false, json.contains("\"sources\""),
			"empty sources should be omitted: " + json);
		// pages should be present and equal to 5
		assertEquals(true, json.contains("\"pages\":5"),
			"pages should serialize: " + json);
	}

	@Test
	void structuralBlockWithProvenanceFieldIsIgnored() throws Exception
	{
		// Regression for #074: extraction-layer pipeline emits a `provenance`
		// field on structural blocks; the model must tolerate it (visualizer
		// previously failed with UnrecognizedPropertyException)
		String json = "{"
			+ "\"vim_version\":\"2.0\","
			+ "\"document\":{\"source\":\"d.pdf\",\"pages\":1},"
			+ "\"layers\":{"
			+ "\"physical\":{\"blocks\":[]},"
			+ "\"structural\":{\"blocks\":[{"
			+ "\"id\":\"sb-1\",\"type\":\"paragraph\",\"page\":1,\"header\":\"H\",\"content\":[\"text\"],"
			+ "\"provenance\":{\"sourceId\":\"src-1\",\"sourcePage\":1}"
			+ "}]},"
			+ "\"semantic\":{\"entities\":[]}"
			+ "}}";

		VimDocument doc = mapper.readValue(json, VimDocument.class);

		assertEquals("2.0", doc.getVimVersion());
		assertNotNull(doc.getLayers().getStructural());
		assertEquals(1, doc.getLayers().getStructural().getBlocks().size());
		VimDocument.StructuralBlock block = doc.getLayers().getStructural().getBlocks().get(0);
		assertEquals("sb-1", block.getId());
		assertEquals("paragraph", block.getType());
	}

	@Test
	void unknownTopLevelFieldsAreIgnored() throws Exception
	{
		String json = "{"
			+ "\"vim_version\":\"2.1\","
			+ "\"document\":{\"source\":\"x.pdf\",\"pages\":1,\"futureField\":\"X\"},"
			+ "\"layers\":{\"physical\":{\"blocks\":[]},\"structural\":{\"blocks\":[]},\"semantic\":{\"entities\":[]}},"
			+ "\"futureRoot\":42"
			+ "}";

		VimDocument doc = mapper.readValue(json, VimDocument.class);
		assertEquals("2.1", doc.getVimVersion());
		assertEquals(1, doc.getDocument().getPages());
	}
}
