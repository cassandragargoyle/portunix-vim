/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.it;

import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.ParserPlugin;
import org.cassandragargoyle.portunix.vimparser.api.VisualizerPlugin;
import org.cassandragargoyle.portunix.vimparser.core.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test verifying that PluginManager discovers the real sibling
 * plugin modules (barcode, fulltext, spayd, html). Runs in the integration-test phase
 * via Failsafe after all reactor JARs are built, so their META-INF/extensions.idx and
 * plugin.properties are on the test classpath.
 *
 * <p>Companion to {@code vim-parser-core/.../PluginManagerTest}, which uses test-only stubs
 * for fast surefire feedback. This IT guards against breakage in real plugin manifests
 * (e.g. missing @Extension, missing META-INF/services file, renamed plugin ID).
 */
class PluginManagerIT
{
	private PluginManager manager;

	@BeforeEach
	void setUp()
	{
		manager = new PluginManager();
	}

	@AfterEach
	void tearDown()
	{
		manager.shutdown();
	}

	@Test
	void discoversRealExtractors()
	{
		manager.discoverPlugins();

		List<ExtractorPlugin> extractors = manager.getExtractorPlugins();
		assertTrue(extractors.size() >= 2, "expected ≥2 real extractors (barcode, fulltext)");

		assertNotNull(manager.getExtractorPlugin("barcode"), "barcode extractor not discovered");
		assertNotNull(manager.getExtractorPlugin("fulltext"), "fulltext extractor not discovered");
	}

	@Test
	void discoversRealParser()
	{
		manager.discoverPlugins();

		ParserPlugin spayd = manager.getParserPlugin("spayd");
		assertNotNull(spayd, "spayd parser not discovered");
		assertEquals("spayd", spayd.getId());
	}

	@Test
	void discoversRealVisualizer()
	{
		manager.discoverPlugins();

		VisualizerPlugin html = manager.getVisualizerPlugin("html");
		assertNotNull(html, "html visualizer not discovered");
		assertEquals("html", html.getId());
	}

	@Test
	void hasPluginReportsRealIds()
	{
		manager.discoverPlugins();

		assertTrue(manager.hasExtractorPlugin("barcode"));
		assertTrue(manager.hasExtractorPlugin("fulltext"));
		assertFalse(manager.hasExtractorPlugin("nonexistent"));

		assertTrue(manager.hasParserPlugin("spayd"));
		assertTrue(manager.hasVisualizerPlugin("html"));
	}

	@Test
	void filterByRealId()
	{
		manager.discoverPlugins();

		Set<String> filter = new HashSet<>();
		filter.add("barcode");

		List<ExtractorPlugin> extractors = manager.getExtractorPlugins(filter);
		assertEquals(1, extractors.size());
		assertEquals("barcode", extractors.get(0).getId());
	}

	@Test
	void getAllPluginIdsContainsRealIds()
	{
		manager.discoverPlugins();

		List<String> ids = manager.getAllPluginIds();
		assertTrue(ids.contains("barcode"));
		assertTrue(ids.contains("fulltext"));
		assertTrue(ids.contains("spayd"));
		assertTrue(ids.contains("html"));
	}
}
