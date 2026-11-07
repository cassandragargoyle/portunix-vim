/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.core.testsupport;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.pf4j.Extension;

import java.util.Collections;
import java.util.Set;

@Extension
public class TestExtractorPluginBeta implements ExtractorPlugin
{
	public static final String ID = "test-extractor-beta";

	@Override
	public String getId() { return ID; }

	@Override
	public String getName() { return "Test Extractor Beta"; }

	@Override
	public String getVersion() { return "0.0.0-test"; }

	@Override
	public int getPriority() { return 20; }

	@Override
	public Set<String> getDependencies() { return Collections.emptySet(); }

	@Override
	public ExecutionMode[] getSupportedModes() { return new ExecutionMode[] { ExecutionMode.SYNC }; }

	@Override
	public PluginResult extract(PluginContext context) { return PluginResult.success(null); }
}
