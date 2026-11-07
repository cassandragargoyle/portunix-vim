/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for VIM Parser execution
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class VimParserConfig
{
	@JsonProperty("execution")
	private ExecutionConfig execution = new ExecutionConfig();

	@JsonProperty("plugins")
	private Map<String, PluginConfig> plugins = new HashMap<>();

	public static VimParserConfig load(File configFile) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(configFile, VimParserConfig.class);
	}

	public static VimParserConfig defaultConfig()
	{
		return new VimParserConfig();
	}

	public ExecutionConfig getExecution()
	{
		return execution;
	}

	public void setExecution(ExecutionConfig execution)
	{
		this.execution = execution;
	}

	public Map<String, PluginConfig> getPlugins()
	{
		return plugins;
	}

	public void setPlugins(Map<String, PluginConfig> plugins)
	{
		this.plugins = plugins;
	}

	public PluginConfig getPluginConfig(String pluginId)
	{
		return plugins.get(pluginId);
	}

	/**
	 * Execution configuration
	 */
	public static class ExecutionConfig
	{
		@JsonProperty("defaultMode")
		private ExecutionMode defaultMode = ExecutionMode.SYNC;

		@JsonProperty("threadPoolSize")
		private int threadPoolSize = 4;

		@JsonProperty("grpc")
		private GrpcConfig grpc = new GrpcConfig();

		public ExecutionMode getDefaultMode()
		{
			return defaultMode;
		}

		public void setDefaultMode(ExecutionMode defaultMode)
		{
			this.defaultMode = defaultMode;
		}

		public int getThreadPoolSize()
		{
			return threadPoolSize;
		}

		public void setThreadPoolSize(int threadPoolSize)
		{
			this.threadPoolSize = threadPoolSize;
		}

		public GrpcConfig getGrpc()
		{
			return grpc;
		}

		public void setGrpc(GrpcConfig grpc)
		{
			this.grpc = grpc;
		}
	}

	/**
	 * gRPC configuration
	 */
	public static class GrpcConfig
	{
		@JsonProperty("enabled")
		private boolean enabled = false;

		@JsonProperty("portRange")
		private String portRange = "50051-50060";

		@JsonProperty("processTimeoutMs")
		private long processTimeoutMs = 30000;

		public boolean isEnabled()
		{
			return enabled;
		}

		public void setEnabled(boolean enabled)
		{
			this.enabled = enabled;
		}

		public String getPortRange()
		{
			return portRange;
		}

		public void setPortRange(String portRange)
		{
			this.portRange = portRange;
		}

		public long getProcessTimeoutMs()
		{
			return processTimeoutMs;
		}

		public void setProcessTimeoutMs(long processTimeoutMs)
		{
			this.processTimeoutMs = processTimeoutMs;
		}
	}

	/**
	 * Per-plugin configuration
	 */
	public static class PluginConfig
	{
		@JsonProperty("enabled")
		private boolean enabled = true;

		@JsonProperty("mode")
		private ExecutionMode mode;

		@JsonProperty("priority")
		private Integer priority;

		@JsonProperty("options")
		private Map<String, String> options = new HashMap<>();

		public boolean isEnabled()
		{
			return enabled;
		}

		public void setEnabled(boolean enabled)
		{
			this.enabled = enabled;
		}

		public ExecutionMode getMode()
		{
			return mode;
		}

		public void setMode(ExecutionMode mode)
		{
			this.mode = mode;
		}

		public Integer getPriority()
		{
			return priority;
		}

		public void setPriority(Integer priority)
		{
			this.priority = priority;
		}

		public Map<String, String> getOptions()
		{
			return options;
		}

		public void setOptions(Map<String, String> options)
		{
			this.options = options;
		}
	}
}
