/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON key order of the model classes written by a plain ObjectMapper,
 * the same mapper settings as the production code, and presence of a
 * complete @JsonPropertyOrder on every model class
 */
class ModelJsonKeyOrderTest
{
	private static final String MODEL_PACKAGE = BoundingBox.class.getPackageName();

	private final ObjectMapper mapper = new ObjectMapper();

	// An explicit and complete @JsonPropertyOrder wins over this feature, the natural order does not
	private final ObjectMapper sortingMapper = JsonMapper.builder()
		.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
		.build();

	@Test
	void boundingBoxKeyOrder() throws Exception
	{
		BoundingBox bbox = new BoundingBox(10, 20, 100, 50);

		assertEquals(List.of("right", "bottom", "x", "y", "width", "height"), keys(bbox));
	}

	@Test
	void textElementKeyOrder() throws Exception
	{
		TextElement element = new TextElement("Hello", new BoundingBox(10, 20, 50, 15), 1, 12.0f, "Arial");
		element.setDropCapMerged(true);
		element.setDropCapBbox(new BoundingBox(5, 20, 5, 15));

		assertEquals(List.of("text", "bbox", "page", "font_size", "font_name", "confidence",
			"drop_cap_merged", "drop_cap_bbox"), keys(element));
	}

	@Test
	void textBlockKeyOrder() throws Exception
	{
		TextBlock block = new TextBlock("block_1", 1);
		block.addTextElement(new TextElement("Hello", new BoundingBox(10, 20, 50, 15), 1, 12.0f, "Arial"));

		assertEquals(List.of("id", "bbox", "page", "text_elements", "header", "content", "type",
			"significance", "confidence"), keys(block));
	}

	@Test
	void tableOfContentsKeyOrder() throws Exception
	{
		TableOfContents toc = new TableOfContents();
		toc.setTocPage(2);

		assertEquals(List.of("entries", "toc_page"), keys(toc));
	}

	@Test
	void barcodeKeyOrder() throws Exception
	{
		Barcode barcode = new Barcode("barcode_1", 1);
		barcode.setRawContent("SPD*1.0*ACC:CZ5855000000001265098001");

		assertEquals(List.of("id", "bbox", "page", "format", "raw_content", "content_type", "confidence"),
			keys(barcode));
	}

	@Test
	void everyModelClassHasCompleteKeyOrder() throws Exception
	{
		List<Class<?>> classes = modelClasses();
		assertTrue(classes.size() > 40, "Model classes not found, only " + classes.size());

		List<String> problems = new ArrayList<>();
		for (Class<?> type : classes)
		{
			List<String> properties = serializedProperties(type);
			if (properties.isEmpty())
			{
				continue;
			}
			JsonPropertyOrder order = type.getAnnotation(JsonPropertyOrder.class);
			if (order == null)
			{
				problems.add(type.getName() + ": no @JsonPropertyOrder for " + properties);
			}
			else if (!Arrays.asList(order.value()).equals(properties))
			{
				problems.add(type.getName() + ": @JsonPropertyOrder " + Arrays.asList(order.value())
					+ " does not match the written keys " + properties);
			}
		}

		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private List<String> serializedProperties(Class<?> type)
	{
		BeanDescription description = mapper.getSerializationConfig().introspect(mapper.constructType(type));
		List<String> properties = new ArrayList<>();
		for (BeanPropertyDefinition property : description.findProperties())
		{
			if (property.couldSerialize())
			{
				properties.add(property.getName());
			}
		}
		return properties;
	}

	// All bean classes of the model package and its subpackages, nested classes included
	private static List<Class<?>> modelClasses() throws Exception
	{
		Path root = Path.of(BoundingBox.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path modelDir = root.resolve(MODEL_PACKAGE.replace('.', '/'));
		assertTrue(Files.isDirectory(modelDir), "Not a class directory: " + modelDir);

		List<Class<?>> classes = new ArrayList<>();
		try (Stream<Path> files = Files.walk(modelDir))
		{
			for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".class")).sorted()::iterator)
			{
				String name = root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), ".");
				Class<?> type = Class.forName(name.substring(0, name.length() - ".class".length()));
				if (!type.isEnum() && !type.isInterface() && !type.isAnonymousClass())
				{
					classes.add(type);
				}
			}
		}
		return classes;
	}

	// Key order written by the plain mapper, it must not depend on the sorting feature
	private List<String> keys(Object value) throws Exception
	{
		List<String> keys = keys(mapper, value);
		assertNotNull(value.getClass().getAnnotation(JsonPropertyOrder.class),
			value.getClass().getSimpleName() + " has no @JsonPropertyOrder");
		assertEquals(keys, keys(sortingMapper, value), "Key order depends on the mapper settings");
		return keys;
	}

	private static List<String> keys(ObjectMapper objectMapper, Object value) throws Exception
	{
		List<String> keys = new ArrayList<>();
		objectMapper.readTree(objectMapper.writeValueAsString(value)).fieldNames().forEachRemaining(keys::add);
		return keys;
	}
}
