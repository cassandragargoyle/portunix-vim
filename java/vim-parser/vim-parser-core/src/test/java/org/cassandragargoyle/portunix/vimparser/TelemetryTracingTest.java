/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.cassandragargoyle.api.telemetry.ExporterType;
import org.cassandragargoyle.api.telemetry.TelemetryProvider;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenTelemetry tracing integration in vim-parser
 */
class TelemetryTracingTest
{

	@Test
	void noOpTracerHasZeroOverhead()
	{
		TelemetryProvider provider = TelemetryProvider.noOp();
		assertTrue(provider.isNoOp());

		Tracer tracer = provider.getTracer("test");
		assertNotNull(tracer);

		Span span = tracer.spanBuilder("test-span").startSpan();
		assertNotNull(span);
		span.end();

		provider.shutdown();
	}

	@Test
	void pluginContextReturnsNoOpTracerByDefault()
	{
		PluginContext context = new PluginContext();
		Tracer tracer = context.getTracer();
		assertNotNull(tracer);

		// Should be NoOp tracer - creating spans should work but do nothing
		Span span = tracer.spanBuilder("noop-span").startSpan();
		assertNotNull(span);
		span.end();
	}

	@Test
	void pluginContextPropagatesTracer()
	{
		TelemetryProvider provider = TelemetryProvider.builder("ptx-vim-test")
			.enabled(true)
			.exporter(ExporterType.CONSOLE)
			.build();

		Tracer tracer = provider.getTracer("vim-parser-engine");

		PluginContext context = new PluginContext();
		context.setTracer(tracer);

		assertSame(tracer, context.getTracer());

		provider.shutdown();
	}

	@Test
	void spanHierarchyForAutoOrchestration()
	{
		TelemetryProvider provider = TelemetryProvider.builder("ptx-vim-test")
			.enabled(true)
			.exporter(ExporterType.CONSOLE)
			.build();

		Tracer tracer = provider.getTracer("vim-parser-orchestrator");

		// Simulate auto-orchestrate span hierarchy
		Span autoSpan = tracer.spanBuilder("auto-orchestrate")
			.setAttribute("vim.command", "auto")
			.setAttribute("document.path", "test.pdf")
			.setAttribute("document.size", 1024L)
			.setAttribute("vim.plugin.count", 4L)
			.startSpan();

		try (var scope = autoSpan.makeCurrent())
		{
			// Per-plugin spans
			String[] plugins = {"fulltext", "barcode", "graphics", "spayd"};
			for (String pluginName : plugins)
			{
				Span pluginSpan = tracer.spanBuilder("plugin:" + pluginName)
					.setAttribute("vim.plugin.name", pluginName)
					.startSpan();
				pluginSpan.end();
			}

			// Build document span
			Span buildSpan = tracer.spanBuilder("build-vim-document").startSpan();
			buildSpan.end();
		}
		finally
		{
			autoSpan.end();
		}

		provider.shutdown();
	}

	@Test
	void spanHierarchyForExtractCommand()
	{
		TelemetryProvider provider = TelemetryProvider.builder("ptx-vim-test")
			.enabled(true)
			.exporter(ExporterType.CONSOLE)
			.build();

		Tracer tracer = provider.getTracer("vim-parser-engine");

		// Simulate process-pdf span hierarchy
		Span processSpan = tracer.spanBuilder("process-pdf")
			.setAttribute("document.path", "test.pdf")
			.setAttribute("vim.command", "extract")
			.setAttribute("vim.mode", "SYNC")
			.startSpan();

		try (var scope = processSpan.makeCurrent())
		{
			Span extractorsSpan = tracer.spanBuilder("run-extractors")
				.setAttribute("vim.plugin.count", 2L)
				.startSpan();

			try (var exScope = extractorsSpan.makeCurrent())
			{
				Span p1 = tracer.spanBuilder("plugin:fulltext")
					.setAttribute("vim.plugin.name", "fulltext")
					.startSpan();
				p1.end();

				Span p2 = tracer.spanBuilder("plugin:barcode")
					.setAttribute("vim.plugin.name", "barcode")
					.startSpan();
				p2.end();
			}
			finally
			{
				extractorsSpan.end();
			}

			Span parsersSpan = tracer.spanBuilder("run-parsers")
				.setAttribute("vim.plugin.count", 1L)
				.startSpan();

			try (var pScope = parsersSpan.makeCurrent())
			{
				Span p3 = tracer.spanBuilder("plugin:spayd")
					.setAttribute("vim.plugin.name", "spayd")
					.startSpan();
				p3.end();
			}
			finally
			{
				parsersSpan.end();
			}

			Span buildSpan = tracer.spanBuilder("build-vim-document").startSpan();
			buildSpan.end();
		}
		finally
		{
			processSpan.end();
		}

		provider.shutdown();
	}

	@Test
	void builderRejectsInvalidSampleRate()
	{
		assertThrows(IllegalArgumentException.class, () ->
			TelemetryProvider.builder("test").sampleRate(-0.1).build()
		);
	}
}
