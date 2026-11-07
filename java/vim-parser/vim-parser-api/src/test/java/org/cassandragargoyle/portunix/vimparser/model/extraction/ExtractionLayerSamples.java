/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.DocumentSource;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sample VIM documents shared by the extraction layer tests
 */
final class ExtractionLayerSamples
{
	private ExtractionLayerSamples()
	{
	}

	/**
	 * VIM v2.0 document that sets every property of the extraction layer
	 */
	static VimDocument fullV2Document()
	{
		VimDocument doc = new VimDocument();
		doc.setVimVersion("2.0");
		doc.getDocument().setSource("invoice.pdf");
		doc.getDocument().setPages(2);

		DocumentSource docSource = new DocumentSource();
		docSource.setFile("invoice.pdf");
		docSource.setPage(1);
		docSource.setType("pdf");
		docSource.setFormat("application/pdf");
		docSource.setDimensions(new DocumentSource.Dimensions(2480, 3508));
		docSource.setSizeBytes(123456L);
		docSource.setSha256("abc123");
		doc.getDocument().setSources(List.of(docSource));

		ExtractionLayer ex = new ExtractionLayer();
		ex.setTemplateRef(new TemplateRef("invoice-v1", "1.2.3", "templates/invoice-v1.json"));

		// Source 1
		ExtractionSource src = new ExtractionSource();
		src.setSourceId("s1");
		src.setSourceFile("invoice.pdf");
		src.setSourcePage(1);

		Provenance prov = new Provenance();
		prov.setExtractionMethod(ExtractionMethod.ZONE_OCR);
		prov.setEngine("tesseract");
		prov.setEngineVersion("5.3.0");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("lang", "ces");
		params.put("psm", 6);
		prov.setParams(params);
		prov.setTimestamp("2026-05-19T10:00:00Z");
		prov.setDurationMs(245L);
		prov.setPipelineStep("ocr-zones");
		src.setProvenance(prov);

		// Field with alternatives, validation, revisions
		ExtractionField f1 = new ExtractionField();
		f1.setFieldId("invoice_number");
		f1.setLabel("Invoice number");
		f1.setValue("INV-001");
		f1.setRawValue("INV-OO1");
		f1.setDataType("string");
		f1.setConfidence(0.91);
		f1.setBbox(new BoundingBox(100, 200, 80, 20));
		f1.setZoneId("zone_invoice_no");
		f1.setExtractionMethod("zone_ocr");
		f1.getAlternatives().add(new Alternative("INV-OO1", 0.42));

		Validation val = new Validation();
		val.setStatus(ValidationStatus.VALID);
		val.getRulesApplied().add("regex:^INV-\\d+$");
		f1.setValidation(val);

		Revision rev = new Revision();
		rev.setRevision(1);
		rev.setOldValue("INV-OO1");
		rev.setNewValue("INV-001");
		rev.setActor(RevisionActor.OPERATOR);
		rev.setActorId("user:zk");
		rev.setReason(RevisionReason.CHARACTER_REPLACEMENT);
		rev.setComment("OCR O→0");
		rev.setTimestamp("2026-05-19T10:05:00Z");
		f1.getRevisions().add(rev);
		src.getFields().add(f1);

		ExtractionField f2 = new ExtractionField();
		f2.setFieldId("amount");
		f2.setValue(1234.56);
		f2.setDataType("decimal");
		f2.setConfidence(0.98);
		src.getFields().add(f2);

		// Field that was looked for and not found: value is null and still written
		ExtractionField f3 = new ExtractionField();
		f3.setFieldId("note");
		f3.setValue(null);
		f3.setDataType("string");
		f3.setConfidence(0.0);
		src.getFields().add(f3);

		// Table fragment
		ExtractionTable table = new ExtractionTable();
		table.setTableId("items-p1");
		table.setGroupId("items");
		table.setPageSequence(1);
		table.setBbox(new BoundingBox(50, 400, 300, 120));
		table.setConfidence(0.9);
		table.getColumns().add(new ColumnDef("c1", "Description", "string"));
		table.getColumns().add(new ColumnDef("c2", "Qty", "number"));

		TableRow row = new TableRow();
		row.setRowIndex(0);
		row.setIsHeader(Boolean.FALSE);
		TableCell cell = new TableCell();
		cell.setColumnId("c1");
		cell.setValue("Widget");
		cell.setRawValue("Widget");
		cell.setConfidence(0.88);
		cell.setRowSpan(1);
		cell.setColSpan(1);
		row.getCells().add(cell);
		table.getRows().add(row);
		src.getTables().add(table);

		ex.getSources().add(src);

		// Merged document
		ExtractionDocument exDoc = new ExtractionDocument();
		exDoc.setCompleteness(0.95);
		exDoc.setFieldCount(2);

		DocumentField df = new DocumentField();
		df.setFieldId("invoice_number");
		df.setLabel("Invoice number");
		df.setValue("INV-001");
		df.setDataType("string");
		df.setConfidence(0.95);
		df.setCollectMode(CollectMode.HIGHEST_CONFIDENCE);
		df.getSourceRefs().add(new SourceRef("s1", "invoice_number"));
		Revision statusRev = new Revision();
		statusRev.setRevision(1);
		statusRev.setProperty("status");
		statusRev.setOldValue(null);
		statusRev.setNewValue("confirmed");
		statusRev.setActor(RevisionActor.RULE);
		statusRev.setReason(RevisionReason.OTHER);
		df.getRevisions().add(statusRev);
		exDoc.getFields().add(df);

		DocumentTable dt = new DocumentTable();
		dt.setTableId("items");
		dt.setLabel("Line items");
		dt.getColumns().add(new ColumnDef("c1", "Description", "string"));
		DocumentTableRow dtRow = new DocumentTableRow();
		dtRow.setRowIndex(0);
		TableCell dtCell = new TableCell();
		dtCell.setColumnId("c1");
		dtCell.setValue("Widget");
		dtRow.getCells().add(dtCell);
		dtRow.setSourceRef(new TableRowRef("s1", "items-p1", 0));
		dt.getRows().add(dtRow);
		dt.setTotalRows(1);
		dt.setConfidence(0.88);
		dt.getSourceFragments().add("items-p1");
		exDoc.getTables().add(dt);

		FieldGroup group = new FieldGroup();
		group.setGroupId("items");
		group.setLabel("Line items");
		group.setRepeatMode(RepeatMode.PER_ITEM);
		GroupInstance gi = new GroupInstance();
		gi.setInstanceIndex(0);
		gi.setSourceId("s1");
		gi.getFields().add(df);
		group.getInstances().add(gi);
		exDoc.getGroups().add(group);

		ex.setDocument(exDoc);
		doc.getLayers().setExtraction(ex);

		return doc;
	}
}
