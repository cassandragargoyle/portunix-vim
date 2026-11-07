/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.cassandragargoyle.portunix.vimparser.model.DocumentSource;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip and structural tests for the VIM v2.0 extraction layer
 */
class ExtractionLayerJsonTest
{
	private ObjectMapper mapper;

	@BeforeEach
	void setUp()
	{
		mapper = new ObjectMapper();
	}

	@Test
	void enumsSerializeAsLowerSnakeCase() throws Exception
	{
		Provenance prov = new Provenance();
		prov.setExtractionMethod(ExtractionMethod.ZONE_OCR);

		String json = mapper.writeValueAsString(prov);
		assertTrue(json.contains("\"extraction_method\":\"zone_ocr\""),
			"Expected lowercase snake_case enum value, got: " + json);
	}

	@Test
	void enumsDeserializeFromLowerSnakeCase() throws Exception
	{
		String json = "{\"extraction_method\":\"template_match\",\"engine\":\"reco\"}";
		Provenance prov = mapper.readValue(json, Provenance.class);

		assertEquals(ExtractionMethod.TEMPLATE_MATCH, prov.getExtractionMethod());
		assertEquals("reco", prov.getEngine());
	}

	@Test
	void unknownPropertiesAreIgnored() throws Exception
	{
		String json = "{\"source_id\":\"s1\",\"unknownField\":\"x\",\"future\":42}";
		ExtractionSource src = mapper.readValue(json, ExtractionSource.class);

		assertEquals("s1", src.getSourceId());
	}

	@Test
	void nullAndEmptyFieldsOmittedInOutput() throws Exception
	{
		ExtractionField field = new ExtractionField();
		field.setFieldId("invoice_number");
		field.setValue("INV-001");

		String json = mapper.writeValueAsString(field);
		assertTrue(json.contains("\"field_id\":\"invoice_number\""));
		assertTrue(json.contains("\"value\":\"INV-001\""));
		// confidence, bbox, validation are null → should be omitted
		assertTrue(!json.contains("\"confidence\""), "null confidence should be omitted: " + json);
		assertTrue(!json.contains("\"bbox\""), "null bbox should be omitted: " + json);
		assertTrue(!json.contains("\"validation\""), "null validation should be omitted: " + json);
		// empty lists also omitted
		assertTrue(!json.contains("\"alternatives\""), "empty alternatives should be omitted: " + json);
		assertTrue(!json.contains("\"revisions\""), "empty revisions should be omitted: " + json);
	}

	@Test
	void provenanceParamsAreOpenMap() throws Exception
	{
		Provenance prov = new Provenance();
		prov.setExtractionMethod(ExtractionMethod.ZONE_OCR);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("lang", "ces");
		params.put("threshold", 0.85);
		params.put("retries", 3);
		prov.setParams(params);

		String json = mapper.writeValueAsString(prov);
		Provenance restored = mapper.readValue(json, Provenance.class);

		assertEquals("ces", restored.getParams().get("lang"));
		assertEquals(0.85, ((Number) restored.getParams().get("threshold")).doubleValue(), 1e-9);
		assertEquals(3, ((Number) restored.getParams().get("retries")).intValue());
	}

	@Test
	void fullExtractionLayerRoundTrip() throws Exception
	{
		VimDocument doc = ExtractionLayerSamples.fullV2Document();

		String json = mapper.writeValueAsString(doc);
		VimDocument restored = mapper.readValue(json, VimDocument.class);
		String json2 = mapper.writeValueAsString(restored);

		// Round-trip must be byte-stable
		assertEquals(json, json2, "Round-trip JSON differs");

		// Spot-check the structure survives
		ExtractionLayer ex = restored.getLayers().getExtraction();
		assertNotNull(ex);
		assertEquals("invoice-v1", ex.getTemplateRef().getTemplateId());
		assertEquals(1, ex.getSources().size());

		ExtractionSource src = ex.getSources().get(0);
		assertEquals("s1", src.getSourceId());
		assertEquals(ExtractionMethod.ZONE_OCR, src.getProvenance().getExtractionMethod());
		assertEquals(3, src.getFields().size());

		ExtractionField first = src.getFields().get(0);
		assertEquals("invoice_number", first.getFieldId());
		assertEquals(1, first.getAlternatives().size());
		assertEquals(ValidationStatus.VALID, first.getValidation().getStatus());
		assertEquals(1, first.getRevisions().size());
		assertEquals(RevisionActor.OPERATOR, first.getRevisions().get(0).getActor());

		ExtractionDocument exDoc = ex.getDocument();
		assertEquals(0.95, exDoc.getCompleteness(), 1e-9);
		assertEquals(CollectMode.HIGHEST_CONFIDENCE, exDoc.getFields().get(0).getCollectMode());
		assertEquals(1, exDoc.getGroups().size());
		assertEquals(RepeatMode.PER_ITEM, exDoc.getGroups().get(0).getRepeatMode());
	}

	@Test
	void camelCaseKeysAreNotWritten() throws Exception
	{
		String json = mapper.writeValueAsString(ExtractionLayerSamples.fullV2Document());

		for (String key : List.of("templateRef", "sourceId", "fieldId", "rawValue", "dataType",
			"rowIndex", "isHeader", "collectMode", "sourceRefs", "sizeBytes"))
		{
			assertTrue(!json.contains("\"" + key + "\""), "camelCase key written: " + key);
		}
	}

	@Test
	void schemaOnlyPropertiesRoundTrip() throws Exception
	{
		VimDocument doc = ExtractionLayerSamples.fullV2Document();
		VimDocument restored = mapper.readValue(mapper.writeValueAsString(doc), VimDocument.class);

		ExtractionSource src = restored.getLayers().getExtraction().getSources().get(0);
		assertEquals("ocr-zones", src.getProvenance().getPipelineStep());

		ExtractionField field = src.getFields().get(0);
		assertEquals("Invoice number", field.getLabel());
		assertEquals("zone_ocr", field.getExtractionMethod());

		ExtractionTable table = src.getTables().get(0);
		assertEquals(0.9, table.getConfidence(), 1e-9);
		assertEquals(300f, table.getBbox().getWidth());

		ExtractionDocument exDoc = restored.getLayers().getExtraction().getDocument();
		assertEquals("Invoice number", exDoc.getFields().get(0).getLabel());
		assertEquals("status", exDoc.getFields().get(0).getRevisions().get(0).getProperty());
		assertEquals("Line items", exDoc.getTables().get(0).getLabel());
	}

	@Test
	void documentTableRowReferencesSourceRow() throws Exception
	{
		DocumentTableRow row = new DocumentTableRow();
		row.setRowIndex(0);
		row.setSourceRef(new TableRowRef("s1", "items-p1", 3));

		String json = mapper.writeValueAsString(row);
		assertEquals("{\"row_index\":0,\"cells\":[],"
			+ "\"source_ref\":{\"source_id\":\"s1\",\"table_id\":\"items-p1\",\"row_index\":3}}", json);

		TableRowRef ref = mapper.readValue(json, DocumentTableRow.class).getSourceRef();
		assertEquals("items-p1", ref.getTableId());
		assertEquals(3, ref.getRowIndex());
	}

	@Test
	void untypedValuesKeepJsonType() throws Exception
	{
		String json = "{\"field_id\":\"paid\",\"value\":true,\"raw_value\":1,"
			+ "\"alternatives\":[{\"value\":0,\"confidence\":0.1}],"
			+ "\"revisions\":[{\"revision\":1,\"old_value\":false,\"new_value\":true}]}";
		ExtractionField field = mapper.readValue(json, ExtractionField.class);

		assertEquals(1, field.getRawValue());
		assertEquals(0, field.getAlternatives().get(0).getValue());
		assertEquals(Boolean.FALSE, field.getRevisions().get(0).getOldValue());
		assertEquals(Boolean.TRUE, field.getRevisions().get(0).getNewValue());
		assertEquals(json, mapper.writeValueAsString(field));
	}

	@Test
	void requiredUntypedKeysAreWrittenWhenNull() throws Exception
	{
		assertEquals("{\"field_id\":\"f\",\"value\":null}", mapper.writeValueAsString(fieldWithId()));
		assertEquals("{\"value\":null}", mapper.writeValueAsString(new DocumentField()));
		assertEquals("{\"value\":null}", mapper.writeValueAsString(new TableCell()));
		assertEquals("{\"value\":null}", mapper.writeValueAsString(new Alternative()));
		assertEquals("{\"old_value\":null,\"new_value\":null}", mapper.writeValueAsString(new Revision()));

		ExtractionField restored = mapper.readValue("{\"field_id\":\"f\",\"value\":null}", ExtractionField.class);
		assertNull(restored.getValue());
		assertEquals("{\"field_id\":\"f\",\"value\":null}", mapper.writeValueAsString(restored));
	}

	private static ExtractionField fieldWithId()
	{
		ExtractionField field = new ExtractionField();
		field.setFieldId("f");
		return field;
	}

	@Test
	void requiredArraysAreWrittenWhenEmpty() throws Exception
	{
		ExtractionTable table = new ExtractionTable();
		table.setTableId("t1");

		assertEquals("{\"table_id\":\"t1\",\"columns\":[],\"rows\":[]}",
			mapper.writeValueAsString(table));
		assertEquals("{\"sources\":[]}", mapper.writeValueAsString(new ExtractionLayer()));
		assertEquals("{\"cells\":[]}", mapper.writeValueAsString(new TableRow()));
		assertEquals("{\"columns\":[],\"rows\":[]}", mapper.writeValueAsString(new DocumentTable()));
		assertEquals("{\"cells\":[]}", mapper.writeValueAsString(new DocumentTableRow()));
		assertEquals("{\"instances\":[]}", mapper.writeValueAsString(new FieldGroup()));
		assertEquals("{\"fields\":[]}", mapper.writeValueAsString(new GroupInstance()));
	}

	@Test
	void tableCellValuesKeepJsonType() throws Exception
	{
		String json = "{\"column_id\":\"c1\",\"value\":12,\"raw_value\":false}";
		TableCell cell = mapper.readValue(json, TableCell.class);

		assertEquals(12, cell.getValue());
		assertEquals(Boolean.FALSE, cell.getRawValue());
		assertEquals(json, mapper.writeValueAsString(cell));
	}

	@Test
	void preciseDecimalsNeedBigDecimalMapper() throws Exception
	{
		String json = "{\"field_id\":\"total\",\"value\":0.1234567890123456789012345,\"raw_value\":1E+400}";

		// Default mapper reads fractions as double: digits are lost, overflow becomes "Infinity"
		String lossy = mapper.writeValueAsString(mapper.readValue(json, ExtractionField.class));
		assertTrue(lossy.contains("\"value\":0.12345678901234568"), lossy);
		assertTrue(lossy.contains("\"raw_value\":\"Infinity\""), lossy);

		ObjectMapper precise = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		assertEquals(json, precise.writeValueAsString(precise.readValue(json, ExtractionField.class)));
	}

	@Test
	void documentSourcesAndNullablePagesRoundTrip() throws Exception
	{
		VimDocument doc = new VimDocument();
		doc.setVimVersion("2.0");
		doc.getDocument().setSource("multi.pdf");
		// pages intentionally left null (scan-only document)

		DocumentSource s1 = new DocumentSource();
		s1.setFile("page-001.png");
		s1.setPage(1);
		s1.setType("image");
		s1.setFormat("image/png");
		s1.setDimensions(new DocumentSource.Dimensions(2480, 3508));
		s1.setSizeBytes(123456L);
		s1.setSha256("abc123");
		doc.getDocument().setSources(List.of(s1));

		String json = mapper.writeValueAsString(doc);
		VimDocument restored = mapper.readValue(json, VimDocument.class);

		assertNull(restored.getDocument().getPages(), "pages should round-trip as null");
		assertEquals(1, restored.getDocument().getSources().size());
		assertEquals("page-001.png", restored.getDocument().getSources().get(0).getFile());
		assertEquals(2480, restored.getDocument().getSources().get(0).getDimensions().getWidth());
		assertTrue(json.contains("\"size_bytes\":123456"), "Expected size_bytes key, got: " + json);
		assertEquals(123456L, restored.getDocument().getSources().get(0).getSizeBytes());
		assertEquals("2.0", restored.getVimVersion());
	}
}
