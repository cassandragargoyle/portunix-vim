/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.prescriptive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import org.cassandragargoyle.portunix.vimparser.model.ContractSchemas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Contract tests: the classes and enums of the prescriptive layer must have
 * the properties and values of the contract schemas, in the same order, so
 * that a new schema property is not dropped without a message
 */
class PrescriptiveLayerContractTest
{
	private static final String LAYER_SCHEMA = "vim-prescriptive.schema.json";
	private static final String PROVISION_SCHEMA = "vim-prescriptive-provision.schema.json";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static JsonNode layerSchema;
	private static JsonNode provisionSchema;

	@BeforeAll
	static void loadSchemas() throws Exception
	{
		layerSchema = read(LAYER_SCHEMA);
		provisionSchema = read(PROVISION_SCHEMA);
	}

	@Test
	void classPropertiesEqualSchemaProperties()
	{
		Map<Class<?>, JsonNode> classes = new LinkedHashMap<>();
		classes.put(PrescriptiveLayer.class, layerSchema);
		classes.put(Definition.class, layerSchema.at("/$defs/Definition"));
		classes.put(Reference.class, layerSchema.at("/$defs/Reference"));
		classes.put(Target.class, layerSchema.at("/$defs/Target"));
		classes.put(Term.class, layerSchema.at("/$defs/Term"));
		classes.put(Provision.class, provisionSchema);
		classes.put(Anchor.class, provisionSchema.at("/$defs/Anchor"));
		classes.put(Relation.class, provisionSchema.at("/$defs/Relation"));
		classes.put(Verification.class, provisionSchema.at("/$defs/Verification"));

		for (Map.Entry<Class<?>, JsonNode> entry : classes.entrySet())
		{
			List<String> expected = new ArrayList<>();
			entry.getValue().get("properties").fieldNames().forEachRemaining(expected::add);
			assertFalse(expected.isEmpty(), "No schema properties for " + entry.getKey().getSimpleName());

			assertEquals(expected, serializedProperties(entry.getKey()),
				"Written keys of " + entry.getKey().getSimpleName());
			assertEquals(expected, deserializedProperties(entry.getKey()),
				"Read keys of " + entry.getKey().getSimpleName());
		}
	}

	@Test
	void enumValuesEqualSchemaValues() throws Exception
	{
		Map<Class<? extends Enum<?>>, JsonNode> enums = new LinkedHashMap<>();
		enums.put(ProvisionKind.class, provisionSchema.at("/properties/kind/enum"));
		enums.put(ProvisionStatus.class, provisionSchema.at("/properties/status/enum"));
		enums.put(AnchorElementType.class, provisionSchema.at("/$defs/Anchor/properties/element_type/enum"));
		enums.put(RelationType.class, provisionSchema.at("/$defs/Relation/properties/relation/enum"));
		enums.put(VerificationMethod.class, provisionSchema.at("/$defs/Verification/properties/method/enum"));
		enums.put(ReferenceScope.class, layerSchema.at("/$defs/Reference/properties/scope/enum"));
		enums.put(ReferenceBinding.class, layerSchema.at("/$defs/Reference/properties/binding/enum"));

		for (Map.Entry<Class<? extends Enum<?>>, JsonNode> entry : enums.entrySet())
		{
			List<String> expected = new ArrayList<>();
			entry.getValue().forEach(value -> expected.add(value.asText()));
			assertFalse(expected.isEmpty(), "No schema values for " + entry.getKey().getSimpleName());

			List<String> actual = new ArrayList<>();
			for (Enum<?> constant : entry.getKey().getEnumConstants())
			{
				actual.add(MAPPER.valueToTree(constant).asText());
			}

			assertEquals(expected, actual, "Values of " + entry.getKey().getSimpleName());
		}
	}

	private static List<String> serializedProperties(Class<?> type)
	{
		List<String> properties = new ArrayList<>();
		for (BeanPropertyDefinition property : MAPPER.getSerializationConfig()
			.introspect(MAPPER.constructType(type)).findProperties())
		{
			if (property.couldSerialize())
			{
				properties.add(property.getName());
			}
		}
		return properties;
	}

	private static List<String> deserializedProperties(Class<?> type)
	{
		List<String> properties = new ArrayList<>();
		for (BeanPropertyDefinition property : MAPPER.getDeserializationConfig()
			.introspect(MAPPER.constructType(type)).findProperties())
		{
			if (property.couldDeserialize())
			{
				properties.add(property.getName());
			}
		}
		return properties;
	}

	private static JsonNode read(String fileName) throws Exception
	{
		try (InputStream in = ContractSchemas.open(fileName))
		{
			return MAPPER.readTree(in);
		}
	}
}
