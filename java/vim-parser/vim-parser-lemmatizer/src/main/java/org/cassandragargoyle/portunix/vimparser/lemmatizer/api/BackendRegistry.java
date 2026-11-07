/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer.api;

import org.cassandragargoyle.api.log.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for lemmatizer backends
 * Manages discovery, registration and selection of available backends
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public class BackendRegistry
{
	private static final Logger LOG = LogFactory.getLogger(BackendRegistry.class);

	private final Map<String, LemmatizerBackend> backends;

	public BackendRegistry()
	{
		this.backends = new ConcurrentHashMap<>();
	}

	/**
	 * Register a backend
	 * @param backend backend to register
	 */
	public void register(LemmatizerBackend backend)
	{
		if (backend == null)
		{
			return;
		}
		String id = backend.getBackendId();
		backends.put(id, backend);
		LOG.log(Level.FINE, "Registered lemmatizer backend: {0} ({1})",	LogFactory.args(id, backend.getBackendName()));
	}

	/**
	 * Unregister a backend
	 * @param backendId backend ID to unregister
	 */
	public void unregister(String backendId)
	{
		backends.remove(backendId);
	}

	/**
	 * Get backend by ID
	 * @param backendId backend ID
	 * @return backend if found and available
	 */
	public Optional<LemmatizerBackend> getBackend(String backendId)
	{
		LemmatizerBackend backend = backends.get(backendId);
		if (backend != null && backend.isAvailable())
		{
			return Optional.of(backend);
		}
		return Optional.empty();
	}

	/**
	 * Get best available backend for given language
	 * @param language ISO 639-1 language code
	 * @return backend that supports the language, or empty if none available
	 */
	public Optional<LemmatizerBackend> getBackendForLanguage(String language)
	{
		return backends.values().stream()
			.filter(LemmatizerBackend::isAvailable)
			.filter(b -> b.supportsLanguage(language))
			.findFirst();
	}

	/**
	 * Get all registered backends
	 * @return unmodifiable list of backends
	 */
	public List<LemmatizerBackend> getAllBackends()
	{
		return Collections.unmodifiableList(new ArrayList<>(backends.values()));
	}

	/**
	 * Get all available backends (registered and ready)
	 * @return list of available backends
	 */
	public List<LemmatizerBackend> getAvailableBackends()
	{
		return backends.values().stream()
			.filter(LemmatizerBackend::isAvailable)
			.toList();
	}

	/**
	 * Check if any backend is available
	 * @return true if at least one backend is available
	 */
	public boolean hasAvailableBackends()
	{
		return backends.values().stream().anyMatch(LemmatizerBackend::isAvailable);
	}

	/**
	 * Get count of registered backends
	 * @return number of registered backends
	 */
	public int size()
	{
		return backends.size();
	}
}
