/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a parsed SPAYD (Short Payment Descriptor) payment
 *
 * @author Zdenek
 * @since 2026-01-30
 */
@JsonPropertyOrder({
	"type", "source_barcode", "format", "fields", "confidence"
})
public class SpaydPayment
{
	@JsonProperty("type")
	private String type = "payment";

	@JsonProperty("source_barcode")
	private String sourceBarcode;

	@JsonProperty("format")
	private String format = "spayd";

	@JsonProperty("fields")
	private SpaydFields fields;

	@JsonProperty("confidence")
	private double confidence;

	public SpaydPayment()
	{
		this.fields = new SpaydFields();
		this.confidence = 0.7;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getSourceBarcode()
	{
		return sourceBarcode;
	}

	public void setSourceBarcode(String sourceBarcode)
	{
		this.sourceBarcode = sourceBarcode;
	}

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

	public SpaydFields getFields()
	{
		return fields;
	}

	public void setFields(SpaydFields fields)
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

	/**
	 * SPAYD payment fields
	 */
	@JsonPropertyOrder({
		"account", "amount", "currency", "variable_symbol", "constant_symbol",
		"specific_symbol", "message", "recipient_name"
	})
	public static class SpaydFields
	{
		@JsonProperty("account")
		private String account;

		@JsonProperty("amount")
		private Double amount;

		@JsonProperty("currency")
		private String currency;

		@JsonProperty("variable_symbol")
		private String variableSymbol;

		@JsonProperty("constant_symbol")
		private String constantSymbol;

		@JsonProperty("specific_symbol")
		private String specificSymbol;

		@JsonProperty("message")
		private String message;

		@JsonProperty("recipient_name")
		private String recipientName;

		public String getAccount()
		{
			return account;
		}

		public void setAccount(String account)
		{
			this.account = account;
		}

		public Double getAmount()
		{
			return amount;
		}

		public void setAmount(Double amount)
		{
			this.amount = amount;
		}

		public String getCurrency()
		{
			return currency;
		}

		public void setCurrency(String currency)
		{
			this.currency = currency;
		}

		public String getVariableSymbol()
		{
			return variableSymbol;
		}

		public void setVariableSymbol(String variableSymbol)
		{
			this.variableSymbol = variableSymbol;
		}

		public String getConstantSymbol()
		{
			return constantSymbol;
		}

		public void setConstantSymbol(String constantSymbol)
		{
			this.constantSymbol = constantSymbol;
		}

		public String getSpecificSymbol()
		{
			return specificSymbol;
		}

		public void setSpecificSymbol(String specificSymbol)
		{
			this.specificSymbol = specificSymbol;
		}

		public String getMessage()
		{
			return message;
		}

		public void setMessage(String message)
		{
			this.message = message;
		}

		public String getRecipientName()
		{
			return recipientName;
		}

		public void setRecipientName(String recipientName)
		{
			this.recipientName = recipientName;
		}
	}
}
