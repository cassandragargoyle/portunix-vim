/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.graphics;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.cassandragargoyle.portunix.vimparser.model.GraphicElement;
import org.cassandragargoyle.portunix.vimparser.model.GraphicElementType;
import org.cassandragargoyle.portunix.vimparser.model.PathPoint;
import org.pf4j.Extension;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Graphics extractor plugin for locating images, paths, lines, rectangles and shadings in PDF documents.
 * Uses PDFBox PDFGraphicsStreamEngine to intercept all graphic operations from the content stream
 * and aggregate them into complete graphic elements.
 *
 * @author Zdenek
 * @since 2026-02-03
 */
@Extension
public class GraphicsExtractorPlugin implements ExtractorPlugin, OrchestrationAware
{
	private static final Logger LOG = LogFactory.getLogger(GraphicsExtractorPlugin.class);

	public static final String PLUGIN_ID = "graphics";

	public static final String RESULT_KEY_GRAPHICS = "graphicElements";

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "Graphics Extractor";
	}

	@Override
	public String getVersion()
	{
		return "0.1.0";
	}

	@Override
	public int getPriority()
	{
		return 25;  // After barcode extraction
	}

	@Override
	public Set<String> getDependencies()
	{
		return Collections.emptySet();
	}

	@Override
	public Set<String> getPredecessors()
	{
		return Set.of("barcode");
	}

	@Override
	public boolean isAutoEnabled()
	{
		return true;
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.EXTRACT;
	}

	@Override
	public ExecutionMode[] getSupportedModes()
	{
		return new ExecutionMode[]
		{
			ExecutionMode.SYNC,
			ExecutionMode.ASYNC_THREAD,
			ExecutionMode.ASYNC_GRPC
		};
	}

	@Override
	public void initialize()
	{
		// No initialization needed
	}

	@Override
	public PluginResult extract(PluginContext context)
	{
		long startTime = System.currentTimeMillis();

		try
		{
			byte[] pdfContent = context.getPdfContent();
			if (pdfContent == null || pdfContent.length == 0)
			{
				return PluginResult.failure("No PDF content provided");
			}

			List<GraphicElement> graphics = new ArrayList<>();

			try (PDDocument document = Loader.loadPDF(pdfContent))
			{
				int pageCount = document.getNumberOfPages();
				int elementCounter = 1;

				int startPage = context.isAllPages() ? 0 : context.getPageNumber() - 1;
				int endPage = context.isAllPages() ? pageCount : context.getPageNumber();

				for (int pageNum = startPage; pageNum < endPage; pageNum++)
				{
					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Scanning page {0} for graphic elements", pageNum + 1);
					}

					PDPage page = document.getPage(pageNum);
					List<GraphicElement> pageGraphics = extractGraphicsFromPage(page, pageNum + 1, elementCounter, context);

					if (context.isVerbose() && !pageGraphics.isEmpty())
					{
						LOG.log(Level.INFO, "Page {0}: found {1} graphic element(s)",
								LogFactory.args(pageNum + 1, pageGraphics.size()));
					}

					elementCounter += pageGraphics.size();
					graphics.addAll(pageGraphics);
				}
			}

			// Store results in context for other plugins
			context.putSharedData(RESULT_KEY_GRAPHICS, graphics);

			PluginResult result = PluginResult.success(graphics);
			result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			result.addMetadata("graphic_count", String.valueOf(graphics.size()));

			return result;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Error extracting graphics: {0}", e.getMessage());
			return PluginResult.failure("Graphics extraction failed: " + e.getMessage());
		}
	}

	/**
	 * Extract graphic elements from a single page using PDFGraphicsStreamEngine
	 */
	private List<GraphicElement> extractGraphicsFromPage(PDPage page, int pageNum, int startId, PluginContext context)
	{
		List<GraphicElement> graphics = new ArrayList<>();

		try
		{
			GraphicsExtractorEngine engine = new GraphicsExtractorEngine(page, pageNum, startId, graphics, context);
			engine.processPage(page);
		}
		catch (IOException e)
		{
			LOG.log(Level.WARNING, "Error processing page {0}: {1}",
					LogFactory.args(pageNum, e.getMessage()));
		}

		return graphics;
	}

	@Override
	public void shutdown()
	{
		// No cleanup needed
	}

	/**
	 * Custom stream engine for extracting all graphic elements (images, paths, shadings)
	 */
	private static class GraphicsExtractorEngine extends PDFGraphicsStreamEngine
	{
		private final int pageNum;
		private int elementId;
		private final List<GraphicElement> graphics;
		private final PluginContext context;
		private static final Logger LOG = LogFactory.getLogger(GraphicsExtractorEngine.class);

		// Path accumulation state
		private List<PathPoint> currentPath = new ArrayList<>();
		private boolean currentPathIsRect = false;

		public GraphicsExtractorEngine(PDPage page, int pageNum, int startId, List<GraphicElement> graphics, PluginContext context)
		{
			super(page);
			this.pageNum = pageNum;
			this.elementId = startId;
			this.graphics = graphics;
			this.context = context;
		}

		// --- Image handling ---

		@Override
		public void drawImage(PDImage pdImage) throws IOException
		{
			GraphicElement element = new GraphicElement("graphic_" + elementId++, pageNum);
			element.setElementType(GraphicElementType.IMAGE);

			// Get position from current transformation matrix
			Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
			float width = ctm.getScalingFactorX();
			float height = ctm.getScalingFactorY();
			float x = ctm.getTranslateX();
			float y = ctm.getTranslateY();

			element.setBbox(new BoundingBox(x, y, Math.abs(width), Math.abs(height)));

			if (pdImage instanceof PDImageXObject)
			{
				PDImageXObject xImage = (PDImageXObject) pdImage;
				element.setImageType("embedded");
				element.setFormat(getImageFormat(xImage));
				element.setWidthPx(xImage.getWidth());
				element.setHeightPx(xImage.getHeight());

				if (xImage.getColorSpace() != null)
				{
					element.setColorSpace(xImage.getColorSpace().getName());
				}
				element.setBitsPerComponent(xImage.getBitsPerComponent());
			}
			else
			{
				element.setImageType("inline");
				element.setFormat("unknown");
				element.setWidthPx(pdImage.getWidth());
				element.setHeightPx(pdImage.getHeight());
				element.setBitsPerComponent(pdImage.getBitsPerComponent());

				if (pdImage.getColorSpace() != null)
				{
					element.setColorSpace(pdImage.getColorSpace().getName());
				}
			}

			graphics.add(element);

			if (context.isVerbose())
			{
				LOG.log(Level.FINE, "Found {0} image: {1}x{2} at ({3}, {4})",
						LogFactory.args(element.getImageType(), element.getWidthPx(), element.getHeightPx(),
								x, y));
			}
		}

		/**
		 * Determine image format from PDImageXObject
		 */
		private String getImageFormat(PDImageXObject image)
		{
			String suffix = image.getSuffix();
			if (suffix != null && !suffix.isEmpty())
			{
				String name = suffix.toLowerCase();
				if (name.contains("jpg") || name.contains("jpeg"))
				{
					return "jpeg";
				}
				else if (name.contains("png"))
				{
					return "png";
				}
				else if (name.contains("tif") || name.contains("tiff"))
				{
					return "tiff";
				}
				else if (name.contains("jp2") || name.contains("jpx"))
				{
					return "jpeg2000";
				}
				return name;
			}
			return "unknown";
		}

		// --- Path accumulation ---

		@Override
		public void moveTo(float x, float y) throws IOException
		{
			currentPath.add(new PathPoint(x, y, "move"));
			currentPathIsRect = false;
		}

		@Override
		public void lineTo(float x, float y) throws IOException
		{
			currentPath.add(new PathPoint(x, y, "line"));
		}

		@Override
		public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException
		{
			currentPath.add(new PathPoint(x1, y1, "curve_cp1"));
			currentPath.add(new PathPoint(x2, y2, "curve_cp2"));
			currentPath.add(new PathPoint(x3, y3, "curve_end"));
			currentPathIsRect = false;
		}

		@Override
		public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException
		{
			currentPath.clear();
			currentPath.add(new PathPoint((float) p0.getX(), (float) p0.getY(), "move"));
			currentPath.add(new PathPoint((float) p1.getX(), (float) p1.getY(), "line"));
			currentPath.add(new PathPoint((float) p2.getX(), (float) p2.getY(), "line"));
			currentPath.add(new PathPoint((float) p3.getX(), (float) p3.getY(), "line"));
			currentPathIsRect = true;
		}

		@Override
		public void closePath() throws IOException
		{
			if (!currentPath.isEmpty())
			{
				PathPoint first = currentPath.get(0);
				currentPath.add(new PathPoint(first.getX(), first.getY(), "line"));
			}
		}

		@Override
		public Point2D getCurrentPoint() throws IOException
		{
			if (!currentPath.isEmpty())
			{
				PathPoint last = currentPath.get(currentPath.size() - 1);
				return new Point2D.Float(last.getX(), last.getY());
			}
			return new Point2D.Float(0, 0);
		}

		// --- Path emission ---

		@Override
		public void strokePath() throws IOException
		{
			emitPathElement(true, false, -1);
		}

		@Override
		public void fillPath(int windingRule) throws IOException
		{
			emitPathElement(false, true, windingRule);
		}

		@Override
		public void fillAndStrokePath(int windingRule) throws IOException
		{
			emitPathElement(true, true, windingRule);
		}

		@Override
		public void endPath() throws IOException
		{
			// Discard path (clip-only or invisible)
			resetPath();
		}

		@Override
		public void clip(int windingRule) throws IOException
		{
			// Clip does not emit an element, just modifies clipping region
			if (context.isVerbose())
			{
				LOG.log(Level.FINE, "Clip operation (winding rule {0})", windingRule);
			}
		}

		// --- Shading ---

		@Override
		public void shadingFill(COSName shadingName) throws IOException
		{
			GraphicElement element = new GraphicElement("graphic_" + elementId++, pageNum);
			element.setElementType(GraphicElementType.SHADING);
			element.setShadingName(shadingName.getName());

			// Use current clipping area as approximate bounding box
			Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
			float x = ctm.getTranslateX();
			float y = ctm.getTranslateY();
			float w = Math.abs(ctm.getScalingFactorX());
			float h = Math.abs(ctm.getScalingFactorY());
			element.setBbox(new BoundingBox(x, y, w, h));

			graphics.add(element);

			if (context.isVerbose())
			{
				LOG.log(Level.FINE, "Found shading: {0}", shadingName.getName());
			}
		}

		// --- Helper methods ---

		/**
		 * Emit a path-based graphic element, classify shape, and reset path state
		 */
		private void emitPathElement(boolean stroked, boolean filled, int windingRule)
		{
			if (currentPath.isEmpty())
			{
				return;
			}

			GraphicElement element = new GraphicElement("graphic_" + elementId++, pageNum);
			element.setStroked(stroked);
			element.setFilled(filled);

			if (windingRule >= 0)
			{
				element.setWindingRule(windingRule == 0 ? "even_odd" : "non_zero");
			}

			// Get stroke width from graphics state
			try
			{
				float lineWidth = getGraphicsState().getLineWidth();
				element.setStrokeWidth(lineWidth);
			}
			catch (Exception e)
			{
				// Ignore if line width not available
			}

			// Classify shape
			GraphicElementType shapeType = classifyShape();
			element.setElementType(shapeType);

			// Compute bounding box from path points
			element.setBbox(computeBbox());

			// Store path points
			element.setPoints(new ArrayList<>(currentPath));

			graphics.add(element);

			if (context.isVerbose())
			{
				LOG.log(Level.FINE, "Found {0}: {1} points, stroked={2}, filled={3}",
						LogFactory.args(shapeType, currentPath.size(), stroked, filled));
			}

			resetPath();
		}

		/**
		 * Classify the current path as LINE, RECT, or PATH
		 */
		private GraphicElementType classifyShape()
		{
			if (currentPathIsRect)
			{
				return GraphicElementType.RECT;
			}

			// Count segments (non-move points)
			List<PathPoint> segments = currentPath.stream()
				.filter(p -> !"move".equals(p.getType()))
				.toList();

			// Single line segment
			if (segments.size() == 1 && "line".equals(segments.get(0).getType()))
			{
				return GraphicElementType.LINE;
			}

			// 4 line segments forming a closed rectangle
			if (segments.size() == 4 || segments.size() == 5)
			{
				boolean allLines = segments.stream().allMatch(p -> "line".equals(p.getType()));
				if (allLines && isRectangular())
				{
					return GraphicElementType.RECT;
				}
			}

			return GraphicElementType.PATH;
		}

		/**
		 * Check if current path points form a rectangle
		 * (4 corners with only horizontal/vertical lines)
		 */
		private boolean isRectangular()
		{
			if (currentPath.size() < 4)
			{
				return false;
			}

			// Get unique x and y coordinates
			float[] xs = new float[currentPath.size()];
			float[] ys = new float[currentPath.size()];
			for (int i = 0; i < currentPath.size(); i++)
			{
				xs[i] = currentPath.get(i).getX();
				ys[i] = currentPath.get(i).getY();
			}

			// A rectangle should have exactly 2 distinct x and 2 distinct y values (within tolerance)
			float tolerance = 0.5f;
			int distinctX = countDistinct(xs, tolerance);
			int distinctY = countDistinct(ys, tolerance);

			return distinctX == 2 && distinctY == 2;
		}

		/**
		 * Count distinct values in array within tolerance
		 */
		private int countDistinct(float[] values, float tolerance)
		{
			List<Float> distinct = new ArrayList<>();
			for (float v : values)
			{
				boolean found = false;
				for (float d : distinct)
				{
					if (Math.abs(v - d) < tolerance)
					{
						found = true;
						break;
					}
				}
				if (!found)
				{
					distinct.add(v);
				}
			}
			return distinct.size();
		}

		/**
		 * Compute bounding box from current path points
		 */
		private BoundingBox computeBbox()
		{
			float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
			float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

			for (PathPoint p : currentPath)
			{
				minX = Math.min(minX, p.getX());
				minY = Math.min(minY, p.getY());
				maxX = Math.max(maxX, p.getX());
				maxY = Math.max(maxY, p.getY());
			}

			return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
		}

		private void resetPath()
		{
			currentPath.clear();
			currentPathIsRect = false;
		}
	}
}
