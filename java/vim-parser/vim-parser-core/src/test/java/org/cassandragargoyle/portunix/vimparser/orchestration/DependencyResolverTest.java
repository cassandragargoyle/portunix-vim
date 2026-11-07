/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.orchestration;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.PluginInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyResolver
 *
 * @author Zdenek
 * @since 2026-01-31
 */
class DependencyResolverTest
{
	private DependencyResolver resolver;

	@BeforeEach
	void setUp()
	{
		resolver = new DependencyResolver();
	}

	@Test
	void testEmptyPlugins()
	{
		List<PluginInfo> result = resolver.resolve(Collections.emptyList());
		assertTrue(result.isEmpty());
	}

	@Test
	void testSinglePlugin()
	{
		List<PluginInfo> plugins = List.of(
			createPlugin("fulltext", ExecutionPhase.EXTRACT, Set.of(), 10)
		);

		List<PluginInfo> result = resolver.resolve(plugins);

		assertEquals(1, result.size());
		assertEquals("fulltext", result.get(0).getId());
	}

	@Test
	void testTwoIndependentPlugins()
	{
		List<PluginInfo> plugins = List.of(
			createPlugin("barcode", ExecutionPhase.EXTRACT, Set.of(), 20),
			createPlugin("fulltext", ExecutionPhase.EXTRACT, Set.of(), 10)
		);

		List<PluginInfo> result = resolver.resolve(plugins);

		assertEquals(2, result.size());
		// Should be sorted by priority within same phase
		assertEquals("fulltext", result.get(0).getId());
		assertEquals("barcode", result.get(1).getId());
	}

	@Test
	void testSimpleChain()
	{
		// fulltext -> barcode -> spayd
		List<PluginInfo> plugins = List.of(
			createPlugin("spayd", ExecutionPhase.PARSE, Set.of("barcode"), 30),
			createPlugin("fulltext", ExecutionPhase.EXTRACT, Set.of(), 10),
			createPlugin("barcode", ExecutionPhase.EXTRACT, Set.of(), 20)
		);

		List<PluginInfo> result = resolver.resolve(plugins);

		assertEquals(3, result.size());
		// barcode must come before spayd
		int barcodeIdx = indexOf(result, "barcode");
		int spaydIdx = indexOf(result, "spayd");
		assertTrue(barcodeIdx < spaydIdx, "barcode should come before spayd");
	}

	@Test
	void testDiamondDependency()
	{
		// A depends on B and C, B and C are independent
		//     B
		//    / \
		//   D   A
		//    \ /
		//     C
		List<PluginInfo> plugins = List.of(
			createPlugin("A", ExecutionPhase.PARSE, Set.of("B", "C"), 40),
			createPlugin("B", ExecutionPhase.EXTRACT, Set.of("D"), 10),
			createPlugin("C", ExecutionPhase.EXTRACT, Set.of("D"), 20),
			createPlugin("D", ExecutionPhase.EXTRACT, Set.of(), 5)
		);

		List<PluginInfo> result = resolver.resolve(plugins);

		assertEquals(4, result.size());
		int dIdx = indexOf(result, "D");
		int bIdx = indexOf(result, "B");
		int cIdx = indexOf(result, "C");
		int aIdx = indexOf(result, "A");

		assertTrue(dIdx < bIdx, "D should come before B");
		assertTrue(dIdx < cIdx, "D should come before C");
		assertTrue(bIdx < aIdx, "B should come before A");
		assertTrue(cIdx < aIdx, "C should come before A");
	}

	@Test
	void testCyclicDependency()
	{
		// A -> B -> C -> A (cycle)
		List<PluginInfo> plugins = List.of(
			createPlugin("A", ExecutionPhase.EXTRACT, Set.of("C"), 10),
			createPlugin("B", ExecutionPhase.EXTRACT, Set.of("A"), 20),
			createPlugin("C", ExecutionPhase.EXTRACT, Set.of("B"), 30)
		);

		assertThrows(DependencyResolver.CyclicDependencyException.class,
			() -> resolver.resolve(plugins));
	}

	@Test
	void testMissingDependency()
	{
		List<PluginInfo> plugins = List.of(
			createPlugin("spayd", ExecutionPhase.PARSE, Set.of("missing-plugin"), 30)
		);

		assertThrows(DependencyResolver.UnresolvedDependencyException.class,
			() -> resolver.resolve(plugins));
	}

	@Test
	void testPhaseOrdering()
	{
		// EXTRACT phase should come before PARSE phase
		List<PluginInfo> plugins = List.of(
			createPlugin("parser", ExecutionPhase.PARSE, Set.of(), 10),
			createPlugin("extractor", ExecutionPhase.EXTRACT, Set.of(), 20)
		);

		List<PluginInfo> result = resolver.resolve(plugins);

		assertEquals(2, result.size());
		assertEquals("extractor", result.get(0).getId());
		assertEquals("parser", result.get(1).getId());
	}

	@Test
	void testValidation()
	{
		List<PluginInfo> plugins = List.of(
			createPlugin("fulltext", ExecutionPhase.EXTRACT, Set.of(), 10),
			createPlugin("barcode", ExecutionPhase.EXTRACT, Set.of(), 20)
		);

		DependencyResolver.ValidationResult validation = resolver.validate(plugins);

		assertTrue(validation.isValid());
		assertTrue(validation.getMissingDependencies().isEmpty());
		assertTrue(validation.getCyclicDependencies().isEmpty());
	}

	@Test
	void testValidationWithMissing()
	{
		List<PluginInfo> plugins = List.of(
			createPlugin("spayd", ExecutionPhase.PARSE, Set.of("missing"), 30)
		);

		DependencyResolver.ValidationResult validation = resolver.validate(plugins);

		assertFalse(validation.isValid());
		assertTrue(validation.getMissingDependencies().containsKey("spayd"));
		assertTrue(validation.getMissingDependencies().get("spayd").contains("missing"));
	}

	@Test
	void testRealWorldScenario()
	{
		// Simulate real vim-parser plugins
		List<PluginInfo> plugins = List.of(
			createPlugin("fulltext", ExecutionPhase.EXTRACT, Set.of(), 10),
			createPlugin("barcode", ExecutionPhase.EXTRACT, Set.of(), 20),
			createPlugin("spayd", ExecutionPhase.PARSE, Set.of("barcode"), 30)
		);

		List<PluginInfo> result = resolver.resolve(plugins);

		assertEquals(3, result.size());

		// Verify fulltext and barcode are before spayd
		int fulltextIdx = indexOf(result, "fulltext");
		int barcodeIdx = indexOf(result, "barcode");
		int spaydIdx = indexOf(result, "spayd");

		assertTrue(barcodeIdx < spaydIdx, "barcode must come before spayd");
		// fulltext has no relation to spayd, but should be in EXTRACT phase
		assertTrue(fulltextIdx < spaydIdx, "fulltext should come before spayd (phase ordering)");
	}

	private PluginInfo createPlugin(String id, ExecutionPhase phase, Set<String> predecessors, int priority)
	{
		return new PluginInfo(
			id,
			id + " Plugin",
			"1.0.0",
			PluginInfo.PluginType.EXTRACTOR,
			phase,
			predecessors,
			true,
			priority
		);
	}

	private int indexOf(List<PluginInfo> list, String id)
	{
		for (int i = 0; i < list.size(); i++)
		{
			if (list.get(i).getId().equals(id))
			{
				return i;
			}
		}
		return -1;
	}
}
