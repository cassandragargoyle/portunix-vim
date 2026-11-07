/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Document-level merged extraction result — fields, tables, and repeating
 * groups after collecting from all sources
 *
 * @author Zdenek
 * @since 2026-05-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"completeness", "field_count", "fields", "tables", "groups"
})
public class ExtractionDocument
{
	@JsonProperty("completeness")
	private Double completeness;

	@JsonProperty("field_count")
	private Integer fieldCount;

	@JsonProperty("fields")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<DocumentField> fields;

	@JsonProperty("tables")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<DocumentTable> tables;

	@JsonProperty("groups")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<FieldGroup> groups;

	public ExtractionDocument()
	{
		this.fields = new ArrayList<>();
		this.tables = new ArrayList<>();
		this.groups = new ArrayList<>();
	}

	public Double getCompleteness()
	{
		return completeness;
	}

	public void setCompleteness(Double completeness)
	{
		this.completeness = completeness;
	}

	public Integer getFieldCount()
	{
		return fieldCount;
	}

	public void setFieldCount(Integer fieldCount)
	{
		this.fieldCount = fieldCount;
	}

	public List<DocumentField> getFields()
	{
		return fields;
	}

	public void setFields(List<DocumentField> fields)
	{
		this.fields = fields;
	}

	public List<DocumentTable> getTables()
	{
		return tables;
	}

	public void setTables(List<DocumentTable> tables)
	{
		this.tables = tables;
	}

	public List<FieldGroup> getGroups()
	{
		return groups;
	}

	public void setGroups(List<FieldGroup> groups)
	{
		this.groups = groups;
	}
}
