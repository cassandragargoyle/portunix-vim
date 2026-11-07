/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.cassandragargoyle.api.log.LogFactory;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.ExtractorPlugin;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.Barcode;
import org.cassandragargoyle.portunix.vimparser.model.BoundingBox;
import org.pf4j.Extension;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Barcode extractor plugin using ZXing for detection
 * Supports QR_CODE, DATA_MATRIX, PDF_417, and AZTEC formats
 *
 * @author Zdenek
 * @since 2026-01-31
 */
@Extension
public class BarcodeExtractorPlugin implements ExtractorPlugin, OrchestrationAware
{
	private static final Logger LOG = LogFactory.getLogger(BarcodeExtractorPlugin.class);

	public static final String PLUGIN_ID = "barcode";

	public static final String RESULT_KEY_BARCODES = "barcodes";

	// Render DPI for barcode detection (higher = better detection but slower)
	private static final float RENDER_DPI = 150.0f;

	// Supported barcode formats
	private static final List<BarcodeFormat> SUPPORTED_FORMATS = Arrays.asList(
		BarcodeFormat.QR_CODE,
		BarcodeFormat.DATA_MATRIX,
		BarcodeFormat.PDF_417,
		BarcodeFormat.AZTEC
	);

	private MultiFormatReader reader;

	private GenericMultipleBarcodeReader multiReader;

	private Map<DecodeHintType, Object> hints;

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "Barcode Extractor";
	}

	@Override
	public String getVersion()
	{
		return "0.1.0";
	}

	@Override
	public int getPriority()
	{
		return 20;  // After text extraction
	}

	@Override
	public Set<String> getDependencies()
	{
		return Collections.emptySet();  // No dependencies
	}

	@Override
	public Set<String> getPredecessors()
	{
		return Collections.emptySet();  // No predecessors
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
		this.reader = new MultiFormatReader();
		this.multiReader = new GenericMultipleBarcodeReader(reader);

		// Configure decoding hints
		this.hints = new HashMap<>();
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, SUPPORTED_FORMATS);
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

			List<Barcode> barcodes = new ArrayList<>();

			try (PDDocument document = Loader.loadPDF(pdfContent))
			{
				PDFRenderer renderer = new PDFRenderer(document);
				int pageCount = document.getNumberOfPages();
				int barcodeCounter = 1;

				int startPage = context.isAllPages() ? 0 : context.getPageNumber() - 1;
				int endPage = context.isAllPages() ? pageCount : context.getPageNumber();

				for (int pageNum = startPage; pageNum < endPage; pageNum++)
				{
					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Scanning page {0} for barcodes", pageNum + 1);
					}

					// Render page to image
					BufferedImage pageImage = renderer.renderImageWithDPI(pageNum, RENDER_DPI);

					// Detect barcodes on this page
					List<Barcode> pageBarcodes = detectBarcodes(pageImage, pageNum + 1, barcodeCounter, context);

					if (context.isVerbose() && !pageBarcodes.isEmpty())
					{
						LOG.log(Level.INFO, "Page {0}: found {1} barcode(s)",
								LogFactory.args(pageNum + 1, pageBarcodes.size()));
					}

					barcodeCounter += pageBarcodes.size();
					barcodes.addAll(pageBarcodes);
				}
			}

			// Store results in context for other plugins
			context.putSharedData(RESULT_KEY_BARCODES, barcodes);

			PluginResult result = PluginResult.success(barcodes);
			result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			result.addMetadata("barcode_count", String.valueOf(barcodes.size()));

			return result;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Error extracting barcodes: {0}", e.getMessage());
			return PluginResult.failure("Barcode extraction failed: " + e.getMessage());
		}
	}

	/**
	 * Detect barcodes in a single page image
	 */
	private List<Barcode> detectBarcodes(BufferedImage image, int pageNum, int startId, PluginContext context)
	{
		List<Barcode> barcodes = new ArrayList<>();

		try
		{
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			// Try to detect multiple barcodes
			Result[] results;
			try
			{
				results = multiReader.decodeMultiple(bitmap, hints);
			}
			catch (NotFoundException e)
			{
				// No barcodes found on this page
				return barcodes;
			}

			int id = startId;
			for (Result result : results)
			{
				Barcode barcode = createBarcode(result, pageNum, id++, image, context);
				if (barcode != null)
				{
					barcodes.add(barcode);

					if (context.isVerbose())
					{
						LOG.log(Level.INFO, "Detected {0}: {1}",
								LogFactory.args(result.getBarcodeFormat(),
												truncate(result.getText(), 50)));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, "Error detecting barcodes on page {0}: {1}", LogFactory.args(pageNum, e.getMessage()));
		}

		return barcodes;
	}

	/**
	 * Create Barcode object from ZXing Result
	 */
	private Barcode createBarcode(Result result, int pageNum, int id, BufferedImage image, PluginContext context)
	{
		if (result == null || result.getText() == null || result.getText().isEmpty())
		{
			return null;
		}

		Barcode barcode = new Barcode("barcode_" + id, pageNum);
		barcode.setFormat(result.getBarcodeFormat().name());
		barcode.setRawContent(result.getText());

		// Calculate bounding box from result points
		BoundingBox bbox = calculateBoundingBox(result.getResultPoints(), image);
		barcode.setBbox(bbox);

		// Determine content type
		String content = result.getText();
		if (content.startsWith("SPD*"))
		{
			barcode.setContentType("spayd");
		}
		else if (content.startsWith("http://") || content.startsWith("https://"))
		{
			barcode.setContentType("url");
		}
		else if (content.startsWith("BEGIN:VCARD"))
		{
			barcode.setContentType("vcard");
		}
		else
		{
			barcode.setContentType("text");
		}

		return barcode;
	}

	/**
	 * Calculate bounding box from result points
	 * Points are in image coordinates, need to convert to PDF coordinates
	 */
	private BoundingBox calculateBoundingBox(ResultPoint[] points, BufferedImage image)
	{
		if (points == null || points.length == 0)
		{
			return new BoundingBox(0, 0, 0, 0);
		}

		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = Float.MIN_VALUE;
		float maxY = Float.MIN_VALUE;

		for (ResultPoint point : points)
		{
			minX = Math.min(minX, point.getX());
			minY = Math.min(minY, point.getY());
			maxX = Math.max(maxX, point.getX());
			maxY = Math.max(maxY, point.getY());
		}

		// Convert from image coordinates (at RENDER_DPI) to PDF coordinates (72 DPI)
		float scale = 72.0f / RENDER_DPI;

		return new BoundingBox(
			minX * scale,
			minY * scale,
			(maxX - minX) * scale,
			(maxY - minY) * scale
		);
	}

	/**
	 * Truncate string for logging
	 */
	private String truncate(String text, int maxLength)
	{
		if (text == null)
		{
			return "";
		}
		if (text.length() <= maxLength)
		{
			return text;
		}
		return text.substring(0, maxLength) + "...";
	}

	@Override
	public void shutdown()
	{
		// Clean up resources if needed
		reader = null;
		multiReader = null;
		hints = null;
	}
}
