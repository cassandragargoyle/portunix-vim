/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Result returned by a plugin after extraction
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class PluginResult
{
	private boolean success;
	private String errorMessage;
	private Object data;
	private Map<String, String> metadata;
	private long processingTimeMs;

	public PluginResult()
	{
		this.metadata = new HashMap<>();
	}

	public static PluginResult success(Object data)
	{
		PluginResult result = new PluginResult();
		result.setSuccess(true);
		result.setData(data);
		return result;
	}

	public static PluginResult failure(String errorMessage)
	{
		PluginResult result = new PluginResult();
		result.setSuccess(false);
		result.setErrorMessage(errorMessage);
		return result;
	}

	public boolean isSuccess()
	{
		return success;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public Object getData()
	{
		return data;
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(Class<T> type)
	{
		if (data != null && type.isInstance(data))
		{
			return (T) data;
		}
		return null;
	}

	public void setData(Object data)
	{
		this.data = data;
	}

	public Map<String, String> getMetadata()
	{
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata)
	{
		this.metadata = metadata;
	}

	public String getMetadata(String key)
	{
		return metadata.get(key);
	}

	public void addMetadata(String key, String value)
	{
		metadata.put(key, value);
	}

	public long getProcessingTimeMs()
	{
		return processingTimeMs;
	}

	public void setProcessingTimeMs(long processingTimeMs)
	{
		this.processingTimeMs = processingTimeMs;
	}
}
