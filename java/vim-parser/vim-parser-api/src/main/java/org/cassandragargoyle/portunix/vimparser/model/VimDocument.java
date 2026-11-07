/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.cassandragargoyle.portunix.vimparser.model.extraction.ExtractionLayer;
import org.cassandragargoyle.portunix.vimparser.model.prescriptive.PrescriptiveLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete VIM (Layered Information Model) document structure
 *
 * @author Zdenek
 * @since 2026-01-30
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
	"vim_version", "document", "layers"
})
public class VimDocument
{
	@JsonProperty("vim_version")
	private String vimVersion = "1.5";

	@JsonProperty("document")
	private DocumentInfo document;

	@JsonProperty("layers")
	private Layers layers;

	public VimDocument()
	{
		this.document = new DocumentInfo();
		this.layers = new Layers();
	}

	public String getVimVersion()
	{
		return vimVersion;
	}

	public void setVimVersion(String vimVersion)
	{
		this.vimVersion = vimVersion;
	}

	public DocumentInfo getDocument()
	{
		return document;
	}

	public void setDocument(DocumentInfo document)
	{
		this.document = document;
	}

	public Layers getLayers()
	{
		return layers;
	}

	public void setLayers(Layers layers)
	{
		this.layers = layers;
	}

	/**
	 * Document metadata
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonPropertyOrder({
		"source", "sources", "pages", "metadata"
	})
	public static class DocumentInfo
	{
		@JsonProperty("source")
		private String source;

		@JsonProperty("sources")
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		private List<DocumentSource> sources;

		@JsonProperty("pages")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private Integer pages;

		@JsonProperty("metadata")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private DocumentMetadata metadata;

		public DocumentInfo()
		{
			this.sources = new ArrayList<>();
		}

		public String getSource()
		{
			return source;
		}

		public void setSource(String source)
		{
			this.source = source;
		}

		public List<DocumentSource> getSources()
		{
			return sources;
		}

		public void setSources(List<DocumentSource> sources)
		{
			this.sources = sources;
		}

		public Integer getPages()
		{
			return pages;
		}

		public void setPages(Integer pages)
		{
			this.pages = pages;
		}

		public DocumentMetadata getMetadata()
		{
			return metadata;
		}

		public void setMetadata(DocumentMetadata metadata)
		{
			this.metadata = metadata;
		}
	}

	/**
	 * Container for all VIM layers
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonPropertyOrder({
		"physical", "structural", "semantic", "toc", "extraction", "prescriptive"
	})
	public static class Layers
	{
		@JsonProperty("physical")
		private PhysicalLayer physical;

		@JsonProperty("structural")
		private StructuralLayer structural;

		@JsonProperty("semantic")
		private SemanticLayer semantic;

		@JsonProperty("toc")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private TableOfContents toc;

		@JsonProperty("extraction")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private ExtractionLayer extraction;

		@JsonProperty("prescriptive")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private PrescriptiveLayer prescriptive;

		public Layers()
		{
			this.physical = new PhysicalLayer();
			this.structural = new StructuralLayer();
			this.semantic = new SemanticLayer();
		}

		public PhysicalLayer getPhysical()
		{
			return physical;
		}

		public void setPhysical(PhysicalLayer physical)
		{
			this.physical = physical;
		}

		public StructuralLayer getStructural()
		{
			return structural;
		}

		public void setStructural(StructuralLayer structural)
		{
			this.structural = structural;
		}

		public SemanticLayer getSemantic()
		{
			return semantic;
		}

		public void setSemantic(SemanticLayer semantic)
		{
			this.semantic = semantic;
		}

		public TableOfContents getToc()
		{
			return toc;
		}

		public void setToc(TableOfContents toc)
		{
			this.toc = toc;
		}

		public ExtractionLayer getExtraction()
		{
			return extraction;
		}

		public void setExtraction(ExtractionLayer extraction)
		{
			this.extraction = extraction;
		}

		public PrescriptiveLayer getPrescriptive()
		{
			return prescriptive;
		}

		public void setPrescriptive(PrescriptiveLayer prescriptive)
		{
			this.prescriptive = prescriptive;
		}
	}

	/**
	 * Physical layer - raw text with positions
	 */
	@JsonPropertyOrder({"blocks", "barcodes", "tables", "graphics", "fulltext"})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PhysicalLayer
	{
		@JsonProperty("blocks")
		private List<PhysicalBlock> blocks;

		@JsonProperty("barcodes")
		private List<PhysicalBarcode> barcodes;

		@JsonProperty("tables")
		private List<PhysicalTable> tables;

		@JsonProperty("graphics")
		private List<GraphicElement> graphics;

		@JsonProperty("fulltext")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private FulltextContent fulltext;

		public PhysicalLayer()
		{
			this.blocks = new ArrayList<>();
			this.barcodes = new ArrayList<>();
			this.tables = new ArrayList<>();
			this.graphics = new ArrayList<>();
		}

		public List<PhysicalBlock> getBlocks()
		{
			return blocks;
		}

		public void setBlocks(List<PhysicalBlock> blocks)
		{
			this.blocks = blocks;
		}

		public void addBlock(PhysicalBlock block)
		{
			this.blocks.add(block);
		}

		public List<PhysicalBarcode> getBarcodes()
		{
			return barcodes;
		}

		public void setBarcodes(List<PhysicalBarcode> barcodes)
		{
			this.barcodes = barcodes;
		}

		public void addBarcode(PhysicalBarcode barcode)
		{
			this.barcodes.add(barcode);
		}

		public List<PhysicalTable> getTables()
		{
			return tables;
		}

		public void setTables(List<PhysicalTable> tables)
		{
			this.tables = tables;
		}

		public void addTable(PhysicalTable table)
		{
			this.tables.add(table);
		}

		public List<GraphicElement> getGraphics()
		{
			return graphics;
		}

		public void setGraphics(List<GraphicElement> graphics)
		{
			this.graphics = graphics;
		}

		public void addGraphic(GraphicElement graphic)
		{
			this.graphics.add(graphic);
		}

		public FulltextContent getFulltext()
		{
			return fulltext;
		}

		public void setFulltext(FulltextContent fulltext)
		{
			this.fulltext = fulltext;
		}
	}

	/**
	 * Physical barcode representation
	 */
	@JsonPropertyOrder({"id", "bbox", "page", "format", "raw_content"})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PhysicalBarcode
	{
		@JsonProperty("id")
		private String id;

		@JsonProperty("bbox")
		private BoundingBox bbox;

		@JsonProperty("page")
		private int page;

		@JsonProperty("format")
		private String format;

		@JsonProperty("raw_content")
		private String rawContent;

		public PhysicalBarcode()
		{
		}

		public static PhysicalBarcode fromBarcode(Barcode barcode)
		{
			PhysicalBarcode pb = new PhysicalBarcode();
			pb.setId(barcode.getId());
			pb.setBbox(barcode.getBbox());
			pb.setPage(barcode.getPage());
			pb.setFormat(barcode.getFormat());
			pb.setRawContent(barcode.getRawContent());
			return pb;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public BoundingBox getBbox()
		{
			return bbox;
		}

		public void setBbox(BoundingBox bbox)
		{
			this.bbox = bbox;
		}

		public int getPage()
		{
			return page;
		}

		public void setPage(int page)
		{
			this.page = page;
		}

		public String getFormat()
		{
			return format;
		}

		public void setFormat(String format)
		{
			this.format = format;
		}

		public String getRawContent()
		{
			return rawContent;
		}

		public void setRawContent(String rawContent)
		{
			this.rawContent = rawContent;
		}
	}

	/**
	 * Physical table representation
	 */
	@JsonPropertyOrder({"id", "page", "bbox", "rows", "columns", "confidence", "cells"})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PhysicalTable
	{
		@JsonProperty("id")
		private String id;

		@JsonProperty("page")
		private int page;

		@JsonProperty("bbox")
		private BoundingBox bbox;

		@JsonProperty("rows")
		private int rows;

		@JsonProperty("columns")
		private int columns;

		@JsonProperty("confidence")
		private float confidence;

		@JsonProperty("cells")
		private List<PhysicalTableCell> cells;

		public PhysicalTable()
		{
			this.cells = new ArrayList<>();
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public int getPage()
		{
			return page;
		}

		public void setPage(int page)
		{
			this.page = page;
		}

		public BoundingBox getBbox()
		{
			return bbox;
		}

		public void setBbox(BoundingBox bbox)
		{
			this.bbox = bbox;
		}

		public int getRows()
		{
			return rows;
		}

		public void setRows(int rows)
		{
			this.rows = rows;
		}

		public int getColumns()
		{
			return columns;
		}

		public void setColumns(int columns)
		{
			this.columns = columns;
		}

		public float getConfidence()
		{
			return confidence;
		}

		public void setConfidence(float confidence)
		{
			this.confidence = confidence;
		}

		public List<PhysicalTableCell> getCells()
		{
			return cells;
		}

		public void setCells(List<PhysicalTableCell> cells)
		{
			this.cells = cells;
		}

		public void addCell(PhysicalTableCell cell)
		{
			this.cells.add(cell);
		}

		public PhysicalTableCell getCell(int row, int col)
		{
			for (PhysicalTableCell cell : cells)
			{
				if (cell.getRow() == row && cell.getColumn() == col)
				{
					return cell;
				}
			}
			return null;
		}
	}

	/**
	 * Physical table cell representation
	 */
	@JsonPropertyOrder({"row", "column", "text", "isHeader", "bbox"})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PhysicalTableCell
	{
		@JsonProperty("row")
		private int row;

		@JsonProperty("column")
		private int column;

		@JsonProperty("text")
		private String text;

		@JsonProperty("isHeader")
		private boolean header;

		@JsonProperty("bbox")
		private BoundingBox bbox;

		public PhysicalTableCell()
		{
		}

		public int getRow()
		{
			return row;
		}

		public void setRow(int row)
		{
			this.row = row;
		}

		public int getColumn()
		{
			return column;
		}

		public void setColumn(int column)
		{
			this.column = column;
		}

		public String getText()
		{
			return text;
		}

		public void setText(String text)
		{
			this.text = text;
		}

		public boolean isHeader()
		{
			return header;
		}

		public void setHeader(boolean header)
		{
			this.header = header;
		}

		public BoundingBox getBbox()
		{
			return bbox;
		}

		public void setBbox(BoundingBox bbox)
		{
			this.bbox = bbox;
		}
	}

	/**
	 * Physical block representation
	 */
	@JsonPropertyOrder({
		"id", "bbox", "page", "text_elements"
	})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PhysicalBlock
	{
		@JsonProperty("id")
		private String id;

		@JsonProperty("bbox")
		private BoundingBox bbox;

		@JsonProperty("page")
		private int page;

		@JsonProperty("text_elements")
		private List<TextElement> textElements;

		public PhysicalBlock()
		{
			this.textElements = new ArrayList<>();
		}

		public static PhysicalBlock fromTextBlock(TextBlock block)
		{
			PhysicalBlock pb = new PhysicalBlock();
			pb.setId(block.getId());
			pb.setBbox(block.getBbox());
			pb.setPage(block.getPage());
			pb.setTextElements(block.getTextElements());
			return pb;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public BoundingBox getBbox()
		{
			return bbox;
		}

		public void setBbox(BoundingBox bbox)
		{
			this.bbox = bbox;
		}

		public int getPage()
		{
			return page;
		}

		public void setPage(int page)
		{
			this.page = page;
		}

		public List<TextElement> getTextElements()
		{
			return textElements;
		}

		public void setTextElements(List<TextElement> textElements)
		{
			this.textElements = textElements;
		}
	}

	/**
	 * Structural layer - labeled content blocks
	 */
	@JsonPropertyOrder({"blocks", "barcodes"})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class StructuralLayer
	{
		@JsonProperty("blocks")
		private List<StructuralBlock> blocks;

		@JsonProperty("barcodes")
		private List<StructuralBarcode> barcodes;

		public StructuralLayer()
		{
			this.blocks = new ArrayList<>();
			this.barcodes = new ArrayList<>();
		}

		public List<StructuralBlock> getBlocks()
		{
			return blocks;
		}

		public void setBlocks(List<StructuralBlock> blocks)
		{
			this.blocks = blocks;
		}

		public void addBlock(StructuralBlock block)
		{
			this.blocks.add(block);
		}

		public List<StructuralBarcode> getBarcodes()
		{
			return barcodes;
		}

		public void setBarcodes(List<StructuralBarcode> barcodes)
		{
			this.barcodes = barcodes;
		}

		public void addBarcode(StructuralBarcode barcode)
		{
			this.barcodes.add(barcode);
		}
	}

	/**
	 * Structural barcode representation
	 */
	@JsonPropertyOrder({"id", "format", "content_type", "nearby_blocks", "confidence"})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class StructuralBarcode
	{
		@JsonProperty("id")
		private String id;

		@JsonProperty("format")
		private String format;

		@JsonProperty("content_type")
		private String contentType;

		@JsonProperty("nearby_blocks")
		private List<String> nearbyBlocks;

		@JsonProperty("confidence")
		private double confidence;

		public StructuralBarcode()
		{
			this.nearbyBlocks = new ArrayList<>();
			this.confidence = 1.0;
		}

		public static StructuralBarcode fromBarcode(Barcode barcode)
		{
			StructuralBarcode sb = new StructuralBarcode();
			sb.setId(barcode.getId());
			sb.setFormat(barcode.getFormat());
			sb.setContentType(barcode.getContentType());
			sb.setConfidence(barcode.getConfidence());
			return sb;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public String getFormat()
		{
			return format;
		}

		public void setFormat(String format)
		{
			this.format = format;
		}

		public String getContentType()
		{
			return contentType;
		}

		public void setContentType(String contentType)
		{
			this.contentType = contentType;
		}

		public List<String> getNearbyBlocks()
		{
			return nearbyBlocks;
		}

		public void setNearbyBlocks(List<String> nearbyBlocks)
		{
			this.nearbyBlocks = nearbyBlocks;
		}

		public void addNearbyBlock(String blockId)
		{
			this.nearbyBlocks.add(blockId);
		}

		public double getConfidence()
		{
			return confidence;
		}

		public void setConfidence(double confidence)
		{
			this.confidence = confidence;
		}
	}

	/**
	 * Structural block representation
	 */
	@JsonPropertyOrder({
		"id", "type", "significance", "page", "header", "content"
	})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class StructuralBlock
	{
		@JsonProperty("id")
		private String id;

		@JsonProperty("type")
		private String type;

		@JsonProperty("significance")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String significance;

		@JsonProperty("page")
		private int page;

		@JsonProperty("header")
		private String header;

		@JsonProperty("content")
		private List<String> content;

		public StructuralBlock()
		{
			this.content = new ArrayList<>();
		}

		public static StructuralBlock fromTextBlock(TextBlock block)
		{
			StructuralBlock sb = new StructuralBlock();
			sb.setId(block.getId());
			sb.setType(block.getType());
			sb.setSignificance(block.getSignificance());
			sb.setPage(block.getPage());
			sb.setHeader(block.getHeader());
			sb.setContent(block.getContent());
			return sb;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public String getSignificance()
		{
			return significance;
		}

		public void setSignificance(String significance)
		{
			this.significance = significance;
		}

		public int getPage()
		{
			return page;
		}

		public void setPage(int page)
		{
			this.page = page;
		}

		public String getHeader()
		{
			return header;
		}

		public void setHeader(String header)
		{
			this.header = header;
		}

		public List<String> getContent()
		{
			return content;
		}

		public void setContent(List<String> content)
		{
			this.content = content;
		}
	}

	/**
	 * Semantic layer - interpreted entities
	 */
	@JsonPropertyOrder({"entities", "payments", "stemming"})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SemanticLayer
	{
		@JsonProperty("entities")
		private List<SemanticEntity> entities;

		@JsonProperty("payments")
		private List<SpaydPayment> payments;

		@JsonProperty("stemming")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private StemmedResult stemming;

		public SemanticLayer()
		{
			this.entities = new ArrayList<>();
			this.payments = new ArrayList<>();
		}

		public List<SemanticEntity> getEntities()
		{
			return entities;
		}

		public void setEntities(List<SemanticEntity> entities)
		{
			this.entities = entities;
		}

		public void addEntity(SemanticEntity entity)
		{
			this.entities.add(entity);
		}

		public List<SpaydPayment> getPayments()
		{
			return payments;
		}

		public void setPayments(List<SpaydPayment> payments)
		{
			this.payments = payments;
		}

		public void addPayment(SpaydPayment payment)
		{
			this.payments.add(payment);
		}

		public StemmedResult getStemming()
		{
			return stemming;
		}

		public void setStemming(StemmedResult stemming)
		{
			this.stemming = stemming;
		}
	}

	/**
	 * Semantic entity representation
	 */
	@JsonPropertyOrder({
		"type", "source_block", "fields", "confidence"
	})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SemanticEntity
	{
		@JsonProperty("type")
		private String type;

		@JsonProperty("source_block")
		private String sourceBlock;

		@JsonProperty("fields")
		private Map<String, String> fields;

		@JsonProperty("confidence")
		private double confidence;

		public SemanticEntity()
		{
			this.fields = new HashMap<>();
			this.confidence = 0.0;
		}

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public String getSourceBlock()
		{
			return sourceBlock;
		}

		public void setSourceBlock(String sourceBlock)
		{
			this.sourceBlock = sourceBlock;
		}

		public Map<String, String> getFields()
		{
			return fields;
		}

		public void setFields(Map<String, String> fields)
		{
			this.fields = fields;
		}

		public double getConfidence()
		{
			return confidence;
		}

		public void setConfidence(double confidence)
		{
			this.confidence = confidence;
		}

		public void addField(String key, String value)
		{
			this.fields.put(key, value);
		}
	}
}
