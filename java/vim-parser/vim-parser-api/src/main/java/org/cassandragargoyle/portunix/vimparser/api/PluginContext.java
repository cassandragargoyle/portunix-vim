/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

import java.util.HashMap;
import java.util.Map;

/**
 * Context passed to plugins during extraction
 * Contains shared state and results from previous plugins
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class PluginContext
{
	private byte[] pdfContent;
	private String sourcePath;
	private int pageNumber = -1;  // -1 means all pages
	private boolean verbose;
	private Tracer tracer;
	private Map<String, Object> sharedData;
	private Map<String, PluginResult> previousResults;
	private Map<String, Object> options;

	public PluginContext()
	{
		this.sharedData = new HashMap<>();
		this.previousResults = new HashMap<>();
		this.options = new HashMap<>();
	}

	public byte[] getPdfContent()
	{
		return pdfContent;
	}

	public void setPdfContent(byte[] pdfContent)
	{
		this.pdfContent = pdfContent;
	}

	public String getSourcePath()
	{
		return sourcePath;
	}

	public void setSourcePath(String sourcePath)
	{
		this.sourcePath = sourcePath;
	}

	public int getPageNumber()
	{
		return pageNumber;
	}

	public void setPageNumber(int pageNumber)
	{
		this.pageNumber = pageNumber;
	}

	public boolean isVerbose()
	{
		return verbose;
	}

	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
	}

	/**
	 * Get tracer for creating sub-spans within plugins.
	 * Returns NoOp tracer if tracing is disabled.
	 */
	public Tracer getTracer()
	{
		if (tracer == null)
		{
			return OpenTelemetry.noop().getTracer("vim-parser");
		}
		return tracer;
	}

	public void setTracer(Tracer tracer)
	{
		this.tracer = tracer;
	}

	public Map<String, Object> getSharedData()
	{
		return sharedData;
	}

	public void setSharedData(Map<String, Object> sharedData)
	{
		this.sharedData = sharedData;
	}

	@SuppressWarnings("unchecked")
	public <T> T getSharedData(String key, Class<T> type)
	{
		Object value = sharedData.get(key);
		if (value != null && type.isInstance(value))
		{
			return (T) value;
		}
		return null;
	}

	public void putSharedData(String key, Object value)
	{
		sharedData.put(key, value);
	}

	public Map<String, PluginResult> getPreviousResults()
	{
		return previousResults;
	}

	public void setPreviousResults(Map<String, PluginResult> previousResults)
	{
		this.previousResults = previousResults;
	}

	public PluginResult getPreviousResult(String pluginId)
	{
		return previousResults.get(pluginId);
	}

	public void addPreviousResult(String pluginId, PluginResult result)
	{
		previousResults.put(pluginId, result);
	}

	public Map<String, Object> getOptions()
	{
		return options;
	}

	public void setOptions(Map<String, Object> options)
	{
		this.options = options;
	}

	public Object getOption(String key)
	{
		return options.get(key);
	}

	public Object getOption(String key, Object defaultValue)
	{
		return options.getOrDefault(key, defaultValue);
	}

	public void setOption(String key, Object value)
	{
		options.put(key, value);
	}

	/**
	 * Check if processing all pages
	 */
	public boolean isAllPages()
	{
		return pageNumber < 0;
	}
}
