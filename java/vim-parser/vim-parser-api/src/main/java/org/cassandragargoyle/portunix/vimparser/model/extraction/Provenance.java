/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context describing how a value was produced (engine, parameters, timing)
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"extraction_method", "engine", "engine_version", "params", "timestamp", "duration_ms", "pipeline_step"
})
public class Provenance
{
	@JsonProperty("extraction_method")
	private ExtractionMethod extractionMethod;

	@JsonProperty("engine")
	private String engine;

	@JsonProperty("engine_version")
	private String engineVersion;

	@JsonProperty("params")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String, Object> params;

	@JsonProperty("timestamp")
	private String timestamp;

	@JsonProperty("duration_ms")
	private Long durationMs;

	@JsonProperty("pipeline_step")
	private String pipelineStep;

	public Provenance()
	{
		this.params = new LinkedHashMap<>();
	}

	public ExtractionMethod getExtractionMethod()
	{
		return extractionMethod;
	}

	public void setExtractionMethod(ExtractionMethod extractionMethod)
	{
		this.extractionMethod = extractionMethod;
	}

	public String getEngine()
	{
		return engine;
	}

	public void setEngine(String engine)
	{
		this.engine = engine;
	}

	public String getEngineVersion()
	{
		return engineVersion;
	}

	public void setEngineVersion(String engineVersion)
	{
		this.engineVersion = engineVersion;
	}

	public Map<String, Object> getParams()
	{
		return params;
	}

	public void setParams(Map<String, Object> params)
	{
		this.params = params;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(String timestamp)
	{
		this.timestamp = timestamp;
	}

	public Long getDurationMs()
	{
		return durationMs;
	}

	public void setDurationMs(Long durationMs)
	{
		this.durationMs = durationMs;
	}

	public String getPipelineStep()
	{
		return pipelineStep;
	}

	public void setPipelineStep(String pipelineStep)
	{
		this.pipelineStep = pipelineStep;
	}
}
