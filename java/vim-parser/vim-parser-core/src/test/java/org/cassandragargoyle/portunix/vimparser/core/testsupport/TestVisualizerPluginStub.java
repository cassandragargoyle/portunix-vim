/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.core.testsupport;

import org.cassandragargoyle.portunix.vimparser.api.VisualizerPlugin;
import org.cassandragargoyle.portunix.vimparser.model.VimDocument;
import org.pf4j.Extension;

@Extension
public class TestVisualizerPluginStub implements VisualizerPlugin
{
	public static final String ID = "test-visualizer";

	@Override
	public String getId() { return ID; }

	@Override
	public String getName() { return "Test Visualizer"; }

	@Override
	public String getVersion() { return "0.0.0-test"; }

	@Override
	public String getOutputFormat() { return "TEST"; }

	@Override
	public String getFileExtension() { return ".test"; }

	@Override
	public String visualize(VimDocument document) { return ""; }
}
