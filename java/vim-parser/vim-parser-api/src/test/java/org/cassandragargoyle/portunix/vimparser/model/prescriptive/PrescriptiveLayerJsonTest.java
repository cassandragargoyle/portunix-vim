/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.cassandragargoyle.portunix.vimparser.model.extraction.ExtractionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip and structural tests for the VIM v2.1 prescriptive layer
 */
class PrescriptiveLayerJsonTest
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
		Relation relation = new Relation();
		relation.setProvisionId("p-1");
		relation.setRelation(RelationType.CONFLICTS_WITH);

		assertEquals("{\"provision_id\":\"p-1\",\"relation\":\"conflicts_with\"}",
			mapper.writeValueAsString(relation));
	}

	@Test
	void enumsDeserializeFromLowerSnakeCase() throws Exception
	{
		String json = "{\"provision_id\":\"p-1\",\"kind\":\"recommendation\",\"status\":\"rejected\","
			+ "\"anchors\":[{\"page\":1,\"element_type\":\"graphic\"}],"
			+ "\"relations\":[{\"provision_id\":\"p-2\",\"relation\":\"depends_on\"}],"
			+ "\"verification\":{\"method\":\"test\"}}";
		Provision provision = mapper.readValue(json, Provision.class);

		assertEquals(ProvisionKind.RECOMMENDATION, provision.getKind());
		assertEquals(ProvisionStatus.REJECTED, provision.getStatus());
		assertEquals(AnchorElementType.GRAPHIC, provision.getAnchors().get(0).getElementType());
		assertEquals(RelationType.DEPENDS_ON, provision.getRelations().get(0).getRelation());
		assertEquals(VerificationMethod.TEST, provision.getVerification().getMethod());

		Reference reference = mapper.readValue(
			"{\"reference_id\":\"r-1\",\"scope\":\"internal\",\"binding\":\"informative\",\"target\":{\"clause\":\"2\"}}",
			Reference.class);
		assertEquals(ReferenceScope.INTERNAL, reference.getScope());
		assertEquals(ReferenceBinding.INFORMATIVE, reference.getBinding());
	}

	@Test
	void unknownEnumValueIsRejected()
	{
		// Decision of issue 159: the read is strict, a value outside the contract is a load error
		for (Class<?> type : List.of(ProvisionKind.class, ProvisionStatus.class, AnchorElementType.class,
			RelationType.class, VerificationMethod.class, ReferenceScope.class, ReferenceBinding.class))
		{
			assertThrows(ValueInstantiationException.class, () -> mapper.readValue("\"wish\"", type),
				type.getSimpleName() + " must reject an unknown value");
		}
		assertThrows(ValueInstantiationException.class, () -> mapper.readValue(
			"{\"vim_version\":\"2.1\",\"layers\":{\"prescriptive\":{\"provisions\":[{\"kind\":\"wish\"}]}}}",
			VimDocument.class));
	}

	@Test
	void enumsAreReadWithoutRegardToCase() throws Exception
	{
		assertEquals(ProvisionStatus.CONFIRMED, mapper.readValue("\"Confirmed\"", ProvisionStatus.class));
		assertEquals(RelationType.EXCEPTION_TO, mapper.readValue("\"EXCEPTION_TO\"", RelationType.class));
		assertEquals("\"exception_to\"", mapper.writeValueAsString(RelationType.EXCEPTION_TO));
	}

	@Test
	void unknownPropertiesAreIgnored() throws Exception
	{
		String json = "{\"vim_version\":\"2.1\",\"document\":{\"source\":\"a.pdf\"},\"layers\":{\"prescriptive\":{"
			+ "\"future\":1,\"definition\":{\"identifier\":\"DF-1\",\"future\":true},"
			+ "\"provisions\":[{\"provision_id\":\"p-1\",\"kind\":\"permission\",\"statement\":\"s\",\"future\":\"x\","
			+ "\"anchors\":[{\"page\":2,\"future\":[]}],"
			+ "\"relations\":[{\"provision_id\":\"p-2\",\"relation\":\"refines\",\"future\":0}],"
			+ "\"verification\":{\"method\":\"manual\",\"future\":{}}}],"
			+ "\"references\":[{\"reference_id\":\"r-1\",\"scope\":\"external\",\"future\":1,"
			+ "\"target\":{\"identifier\":\"DF-2\",\"future\":1}}],"
			+ "\"terms\":[{\"term_id\":\"t-1\",\"term\":\"sheet\",\"future\":1}]}}}";

		PrescriptiveLayer layer = mapper.readValue(json, VimDocument.class).getLayers().getPrescriptive();

		assertEquals("DF-1", layer.getDefinition().getIdentifier());
		assertEquals("p-1", layer.getProvisions().get(0).getProvisionId());
		assertEquals(2, layer.getProvisions().get(0).getAnchors().get(0).getPage());
		assertEquals("DF-2", layer.getReferences().get(0).getTarget().getIdentifier());
		assertEquals("sheet", layer.getTerms().get(0).getTerm());
	}

	@Test
	void nullAndEmptyFieldsOmittedInOutput() throws Exception
	{
		Provision provision = new Provision();
		provision.setProvisionId("p-1");
		provision.setKind(ProvisionKind.REQUIREMENT);
		provision.setStatement("s");

		// anchors is required by the contract and is written when empty
		assertEquals("{\"provision_id\":\"p-1\",\"kind\":\"requirement\",\"statement\":\"s\",\"anchors\":[]}",
			mapper.writeValueAsString(provision));

		Reference reference = new Reference();
		reference.setReferenceId("r-1");
		assertEquals("{\"reference_id\":\"r-1\"}", mapper.writeValueAsString(reference));

		Term term = new Term();
		term.setTermId("t-1");
		assertEquals("{\"term_id\":\"t-1\"}", mapper.writeValueAsString(term));

		// provisions is required by the contract and is written when empty
		assertEquals("{\"provisions\":[]}", mapper.writeValueAsString(new PrescriptiveLayer()));
		assertEquals("{}", mapper.writeValueAsString(new Anchor()));
		assertEquals("{}", mapper.writeValueAsString(new Verification()));
	}

	@Test
	void termReferencesRoundTrip() throws Exception
	{
		String json = "{\"term_id\":\"t-1\",\"term\":\"sheet\",\"clause\":\"3\",\"references\":[\"r-1\",\"r-2\"],"
			+ "\"anchors\":[{\"page\":1}]}";

		Term term = mapper.readValue(json, Term.class);

		assertEquals(List.of("r-1", "r-2"), term.getReferences());
		assertEquals(json, mapper.writeValueAsString(term));
	}

	@Test
	void termWithoutReferencesIsWrittenAsBefore() throws Exception
	{
		String json = "{\"term_id\":\"t-1\",\"term\":\"sheet\",\"anchors\":[{\"page\":1}]}";

		Term term = mapper.readValue(json, Term.class);

		assertTrue(term.getReferences().isEmpty(), "references must default to an empty list");
		assertEquals(json, mapper.writeValueAsString(term));
	}

	@Test
	void definitionLanguageRoundTrips() throws Exception
	{
		String json = "{\"identifier\":\"DF-1\",\"issued\":\"2008-11\",\"publisher\":\"Org\",\"language\":\"cs\"}";

		Definition definition = mapper.readValue(json, Definition.class);

		assertEquals("cs", definition.getLanguage());
		assertEquals("2008-11", definition.getIssued());
		assertEquals(json, mapper.writeValueAsString(definition));
	}

	@Test
	void definitionWithoutLanguageIsWrittenAsBefore() throws Exception
	{
		String json = "{\"identifier\":\"DF-1\",\"issued\":\"2008\",\"publisher\":\"Org\"}";

		Definition definition = mapper.readValue(json, Definition.class);

		assertNull(definition.getLanguage());
		assertEquals(json, mapper.writeValueAsString(definition));
	}

	@Test
	void nullListsAreReadAsEmpty() throws Exception
	{
		String json = "{\"provisions\":[{\"provision_id\":\"p-1\",\"anchors\":null,\"references\":null,"
			+ "\"terms\":null,\"relations\":null,\"revisions\":null}],"
			+ "\"references\":[{\"reference_id\":\"r-1\",\"anchors\":null}],"
			+ "\"terms\":[{\"term_id\":\"t-1\",\"references\":null,\"anchors\":null}]}";

		PrescriptiveLayer layer = mapper.readValue(json, PrescriptiveLayer.class);

		Provision provision = layer.getProvisions().get(0);
		assertTrue(provision.getAnchors().isEmpty());
		assertTrue(provision.getReferences().isEmpty());
		assertTrue(provision.getTerms().isEmpty());
		assertTrue(provision.getRelations().isEmpty());
		assertTrue(provision.getRevisions().isEmpty());
		assertTrue(layer.getReferences().get(0).getAnchors().isEmpty());
		assertTrue(layer.getTerms().get(0).getReferences().isEmpty());
		assertTrue(layer.getTerms().get(0).getAnchors().isEmpty());

		String empty = "{\"provisions\":null,\"references\":null,\"terms\":null}";
		assertEquals("{\"provisions\":[]}", mapper.writeValueAsString(mapper.readValue(empty, PrescriptiveLayer.class)));
	}

	@Test
	void nullLanguageIsAbsent() throws Exception
	{
		Definition definition = mapper.readValue("{\"identifier\":\"DF-1\",\"language\":null}", Definition.class);

		assertNull(definition.getLanguage());
		assertEquals("{\"identifier\":\"DF-1\"}", mapper.writeValueAsString(definition));
	}

	@Test
	void documentWithoutLayerHasNoPrescriptiveKey() throws Exception
	{
		VimDocument doc = new VimDocument();

		JsonNode json = mapper.valueToTree(doc);

		assertEquals("1.5", json.get("vim_version").asText());
		assertTrue(!json.get("layers").has("prescriptive"), "prescriptive layer must be omitted");
		assertNull(mapper.readValue(mapper.writeValueAsString(doc), VimDocument.class).getLayers().getPrescriptive());
	}

	@Test
	void layerIsWrittenAfterExtraction() throws Exception
	{
		String json = "{\"vim_version\":\"2.1\",\"document\":{\"source\":\"a.pdf\"},\"layers\":{"
			+ "\"prescriptive\":{\"provisions\":[]},\"extraction\":{\"sources\":[]}}}";

		JsonNode layers = mapper.valueToTree(mapper.readValue(json, VimDocument.class)).get("layers");

		List<String> keys = new java.util.ArrayList<>();
		layers.fieldNames().forEachRemaining(keys::add);
		assertEquals(List.of("physical", "structural", "semantic", "extraction", "prescriptive"), keys);
	}

	@Test
	void absentStatusIsNotAdded() throws Exception
	{
		String json = "{\"provision_id\":\"p-1\",\"kind\":\"permission\",\"statement\":\"s\",\"anchors\":[{\"page\":1}]}";

		Provision provision = mapper.readValue(json, Provision.class);

		assertNull(provision.getStatus());
		assertEquals(json, mapper.writeValueAsString(provision));
	}

	@Test
	void verificationRuleIsOpenObject() throws Exception
	{
		String json = "{\"method\":\"automatic\",\"rule\":{\"field\":\"a\",\"max\":3,\"ratio\":0.5,"
			+ "\"strict\":true,\"any_of\":[\"x\",\"y\"],\"nested\":{\"k\":null}}}";

		Verification verification = mapper.readValue(json, Verification.class);

		assertEquals(3, ((Number) verification.getRule().get("max")).intValue());
		assertEquals(json, mapper.writeValueAsString(verification));
		// An empty rule object is data and is kept
		assertEquals("{\"method\":\"manual\",\"rule\":{}}",
			mapper.writeValueAsString(mapper.readValue("{\"method\":\"manual\",\"rule\":{}}", Verification.class)));
	}

	@Test
	void revisionPropertyRoundTrips() throws Exception
	{
		String json = "{\"provision_id\":\"p-1\",\"kind\":\"requirement\",\"text\":\"t\",\"anchors\":[{\"page\":1}],"
			+ "\"revisions\":[{\"revision\":1,\"property\":\"kind\",\"old_value\":\"permission\","
			+ "\"new_value\":\"requirement\",\"actor\":\"operator\"}]}";

		Provision provision = mapper.readValue(json, Provision.class);

		assertEquals("kind", provision.getRevisions().get(0).getProperty());
		assertEquals(json, mapper.writeValueAsString(provision));
	}

	@Test
	void fullPrescriptiveLayerRoundTrip() throws Exception
	{
		VimDocument doc = PrescriptiveLayerSamples.fullV21Document();

		String json = mapper.writeValueAsString(doc);
		VimDocument restored = mapper.readValue(json, VimDocument.class);
		String json2 = mapper.writeValueAsString(restored);

		// Round-trip must be byte-stable
		assertEquals(json, json2, "Round-trip JSON differs");
		assertEquals("2.1", restored.getVimVersion());

		// Spot-check the structure survives
		PrescriptiveLayer layer = restored.getLayers().getPrescriptive();
		assertNotNull(layer);
		assertEquals("DF-1234", layer.getDefinition().getIdentifier());
		assertEquals(ExtractionMethod.LLM_EXTRACT, layer.getProvenance().getExtractionMethod());
		assertEquals(4, layer.getProvisions().size());
		assertEquals(2, layer.getReferences().size());
		assertEquals(1, layer.getTerms().size());

		Provision second = layer.getProvisions().get(1);
		assertEquals(ProvisionKind.REQUIREMENT, second.getKind());
		assertEquals("1f77c4a6db023bea", second.getTextHash());
		assertEquals(310.5f, second.getAnchors().get(0).getBbox().getY());
		assertEquals(List.of("r-1"), second.getReferences());
		assertEquals(RelationType.REFINES, second.getRelations().get(0).getRelation());
		assertEquals("present", second.getVerification().getRule().get("check"));
		assertEquals(ProvisionStatus.CONFIRMED, second.getStatus());
		assertEquals(2, second.getRevisions().size());
		assertEquals("status", second.getRevisions().get(1).getProperty());

		assertNull(layer.getProvisions().get(2).getStatus());
		assertNull(layer.getProvisions().get(3).getText());
		assertEquals(ExtractionMethod.MANUAL, layer.getProvisions().get(3).getProvenance().getExtractionMethod());
		assertEquals(ReferenceScope.EXTERNAL, layer.getReferences().get(0).getScope());
		assertEquals("A.2", layer.getReferences().get(1).getTarget().getClause());
		assertEquals("title block", layer.getTerms().get(0).getTerm());
		assertEquals(List.of("r-1"), layer.getTerms().get(0).getReferences());
		assertEquals("en", layer.getDefinition().getLanguage());
	}

	@Test
	void camelCaseKeysAreNotWritten() throws Exception
	{
		String json = mapper.writeValueAsString(PrescriptiveLayerSamples.fullV21Document());

		for (String key : List.of("provisionId", "textHash", "elementType", "elementId", "referenceId", "termId"))
		{
			assertTrue(!json.contains("\"" + key + "\""), "camelCase key written: " + key);
		}
	}
}
