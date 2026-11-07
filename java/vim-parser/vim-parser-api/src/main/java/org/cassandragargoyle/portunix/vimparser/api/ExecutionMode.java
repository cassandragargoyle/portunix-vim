/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.api;

/**
 * Execution modes for plugin processing
 *
 * @author Zdenek
 * @since 2026-01-31
 */
public enum ExecutionMode
{
	/**
	 * Sequential execution in main thread
	 */
	SYNC,

	/**
	 * Asynchronous execution in thread pool
	 */
	ASYNC_THREAD,

	/**
	 * Asynchronous execution via gRPC in separate process
	 */
	ASYNC_GRPC
}
