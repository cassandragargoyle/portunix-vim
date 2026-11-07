/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.DocumentMetadata;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.cassandragargoyle.portunix.vimparser.model.extraction.ExtractionMethod;
import org.cassandragargoyle.portunix.vimparser.model.extraction.Provenance;
import org.cassandragargoyle.portunix.vimparser.model.extraction.Revision;
import org.cassandragargoyle.portunix.vimparser.model.extraction.RevisionActor;
import org.cassandragargoyle.portunix.vimparser.model.extraction.RevisionReason;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sample VIM documents shared by the prescriptive layer tests
 */
final class PrescriptiveLayerSamples
{
	private PrescriptiveLayerSamples()
	{
	}

	/**
	 * VIM v2.1 document that sets every property of the prescriptive layer,
	 * with the content of contract/examples/definition-file.vim.json
	 */
	static VimDocument fullV21Document()
	{
		VimDocument doc = new VimDocument();
		doc.setVimVersion("2.1");
		doc.getDocument().setSource("definition-file.pdf");
		doc.getDocument().setPages(12);
		DocumentMetadata metadata = new DocumentMetadata();
		metadata.setTitle("Drawing title block rules");
		metadata.setProducer("example-producer/0.1.0");
		metadata.setInLanguage("en");
		doc.getDocument().setMetadata(metadata);

		PrescriptiveLayer layer = new PrescriptiveLayer();

		Definition definition = new Definition();
		definition.setIdentifier("DF-1234");
		definition.setEdition("3");
		definition.setIssued("2025-04-01");
		definition.setTitle("Drawing title block rules");
		definition.setPublisher("Example Org");
		definition.setLanguage("en");
		layer.setDefinition(definition);

		Provenance provenance = new Provenance();
		provenance.setExtractionMethod(ExtractionMethod.LLM_EXTRACT);
		provenance.setEngine("model-id");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("prompt_version", "3");
		provenance.setParams(params);
		provenance.setTimestamp("2026-10-09T10:00:00Z");
		layer.setProvenance(provenance);

		// Requirement with verbatim text and one revision
		Provision p1 = provision("p-5.3.1-1", ProvisionKind.REQUIREMENT, "5.3.1", 0.93);
		p1.setText("Every drawing shall have a title block.");
		p1.setTextHash("3b1589fd2fc16eca");
		p1.getAnchors().add(anchor(7, AnchorElementType.BLOCK, "block_40", null));
		p1.getTerms().add("t-title-block");
		p1.setVerification(verification(VerificationMethod.MANUAL, "drawing", null));
		p1.setStatus(ProvisionStatus.CONFIRMED);
		p1.getRevisions().add(revision(1, "status", "proposed", "confirmed", null, "2026-10-09T11:30:00Z"));
		layer.getProvisions().add(p1);

		// Requirement that sets every property except its own provenance
		Provision p2 = provision("p-5.3.2-1", ProvisionKind.REQUIREMENT, "5.3.2", 0.82);
		p2.setText("The title block shall contain the drawing number.");
		p2.setStatement("A title block contains a drawing number.");
		p2.setTextHash("1f77c4a6db023bea");
		p2.getAnchors().add(anchor(7, AnchorElementType.BLOCK, "block_41", new BoundingBox(72.0f, 310.5f, 451.0f, 24.0f)));
		p2.setCondition("Applies to drawings issued for manufacturing.");
		p2.getReferences().add("r-1");
		p2.getTerms().add("t-title-block");
		p2.getRelations().add(relation("p-5.3.1-1", RelationType.REFINES));
		Map<String, Object> rule2 = new LinkedHashMap<>();
		rule2.put("field", "drawing_number");
		rule2.put("check", "present");
		p2.setVerification(verification(VerificationMethod.AUTOMATIC, "drawing", rule2));
		p2.setStatus(ProvisionStatus.CONFIRMED);
		p2.getRevisions().add(revision(1, "kind", "recommendation", "requirement", "The verbal form is shall",
			"2026-10-09T11:31:00Z"));
		p2.getRevisions().add(revision(2, "status", "proposed", "confirmed", null, "2026-10-09T11:32:00Z"));
		layer.getProvisions().add(p2);

		// Recommendation without a status: the property is not written
		Provision p3 = provision("p-5.3.3-1", ProvisionKind.RECOMMENDATION, "5.3.3", 0.77);
		p3.setText("The title block should be placed in the bottom right corner of the sheet.");
		p3.setTextHash("3b3db372a57c76d0");
		p3.getAnchors().add(anchor(8, AnchorElementType.BLOCK, "block_44", null));
		p3.getReferences().add("r-2");
		p3.getTerms().add("t-title-block");
		Map<String, Object> rule3 = new LinkedHashMap<>();
		rule3.put("field", "title_block.position");
		rule3.put("check", "equals");
		rule3.put("value", "bottom-right");
		p3.setVerification(verification(VerificationMethod.AUTOMATIC, "drawing", rule3));
		layer.getProvisions().add(p3);

		// Permission without verbatim text, anchored by page and bbox only, with its own provenance
		Provision p4 = provision("p-5.4.1-1", ProvisionKind.PERMISSION, "5.4.1", 0.64);
		p4.setStatement("A drawing with a single revision does not need a revision table.");
		p4.setTextHash("9fd70967164c9514");
		p4.getAnchors().add(anchor(9, null, null, new BoundingBox(72.0f, 120.0f, 451.0f, 36.0f)));
		p4.getRelations().add(relation("p-5.3.1-1", RelationType.EXCEPTION_TO));
		p4.setVerification(verification(VerificationMethod.MANUAL, null, null));
		Provenance manual = new Provenance();
		manual.setExtractionMethod(ExtractionMethod.MANUAL);
		p4.setProvenance(manual);
		layer.getProvisions().add(p4);

		Reference r1 = new Reference();
		r1.setReferenceId("r-1");
		r1.setScope(ReferenceScope.EXTERNAL);
		r1.setBinding(ReferenceBinding.MANDATORY);
		Target t1 = new Target();
		t1.setIdentifier("DF-0456");
		t1.setEdition("2");
		t1.setTitle("Drawing numbering");
		t1.setClause("4.1");
		t1.setUri("https://example.org/definitions/DF-0456");
		r1.setTarget(t1);
		r1.getAnchors().add(anchor(3, AnchorElementType.BLOCK, "block_12", null));
		r1.setConfidence(0.95);
		r1.setProvenance(manual);
		layer.getReferences().add(r1);

		Reference r2 = new Reference();
		r2.setReferenceId("r-2");
		r2.setScope(ReferenceScope.INTERNAL);
		r2.setBinding(ReferenceBinding.INFORMATIVE);
		Target t2 = new Target();
		t2.setClause("A.2");
		r2.setTarget(t2);
		r2.getAnchors().add(anchor(8, AnchorElementType.GRAPHIC, "graphic_3", null));
		layer.getReferences().add(r2);

		Term term = new Term();
		term.setTermId("t-title-block");
		term.setTerm("title block");
		term.setDefinition("Area of a drawing that holds its identification data.");
		term.setClause("3.4");
		term.getReferences().add("r-1");
		term.getAnchors().add(anchor(2, AnchorElementType.TABLE, "table_1", null));
		term.setConfidence(0.98);
		term.setProvenance(manual);
		layer.getTerms().add(term);

		doc.getLayers().setPrescriptive(layer);
		return doc;
	}

	private static Provision provision(String id, ProvisionKind kind, String clause, double confidence)
	{
		Provision provision = new Provision();
		provision.setProvisionId(id);
		provision.setKind(kind);
		provision.setClause(clause);
		provision.setConfidence(confidence);
		return provision;
	}

	private static Anchor anchor(int page, AnchorElementType elementType, String elementId, BoundingBox bbox)
	{
		Anchor anchor = new Anchor();
		anchor.setPage(page);
		anchor.setElementType(elementType);
		anchor.setElementId(elementId);
		anchor.setBbox(bbox);
		return anchor;
	}

	private static Relation relation(String provisionId, RelationType type)
	{
		Relation relation = new Relation();
		relation.setProvisionId(provisionId);
		relation.setRelation(type);
		return relation;
	}

	private static Verification verification(VerificationMethod method, String subject, Map<String, Object> rule)
	{
		Verification verification = new Verification();
		verification.setMethod(method);
		verification.setSubject(subject);
		verification.setRule(rule);
		return verification;
	}

	private static Revision revision(int number, String property, String oldValue, String newValue, String comment,
		String timestamp)
	{
		Revision revision = new Revision();
		revision.setRevision(number);
		revision.setProperty(property);
		revision.setOldValue(oldValue);
		revision.setNewValue(newValue);
		revision.setActor(RevisionActor.OPERATOR);
		revision.setActorId("user:reviewer");
		revision.setReason(RevisionReason.MANUAL_CORRECTION);
		revision.setComment(comment);
		revision.setTimestamp(timestamp);
		return revision;
	}
}
