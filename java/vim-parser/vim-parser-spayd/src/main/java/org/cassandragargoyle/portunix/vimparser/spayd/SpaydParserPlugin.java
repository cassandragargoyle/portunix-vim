/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.spayd;

import org.cassandragargoyle.portunix.vimparser.api.ExecutionMode;
import org.cassandragargoyle.portunix.vimparser.api.ExecutionPhase;
import org.cassandragargoyle.portunix.vimparser.api.OrchestrationAware;
import org.cassandragargoyle.portunix.vimparser.api.ParserPlugin;
import org.cassandragargoyle.portunix.vimparser.api.PluginContext;
import org.cassandragargoyle.portunix.vimparser.api.PluginResult;
import org.cassandragargoyle.portunix.vimparser.model.Barcode;
import org.cassandragargoyle.portunix.vimparser.model.SpaydPayment;
import org.pf4j.Extension;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SPAYD payment QR code parser plugin
 * Parses Czech QR Platba standard format
 *
 * Format: SPD*1.0*ACC:CZ6508000000192000145399*AM:1667.83*CC:CZK*X-VS:123*MSG:Payment
 *
 * @author Zdenek
 * @since 2026-01-31
 * @see <a href="https://qr-platba.cz/pro-vyvojare/specifikace-formatu/">QR Platba specification</a>
 */
@Extension
public class SpaydParserPlugin implements ParserPlugin, OrchestrationAware
{
	public static final String PLUGIN_ID = "spayd";

	public static final String RESULT_KEY_PAYMENTS = "spaydPayments";

	private static final String SPAYD_PREFIX = "SPD*";

	private static final String FIELD_SEPARATOR = "*";

	@Override
	public String getId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getName()
	{
		return "SPAYD Parser";
	}

	@Override
	public String getVersion()
	{
		return "0.1.0";
	}

	@Override
	public int getPriority()
	{
		return 30;  // After barcode extraction
	}

	@Override
	public Set<String> getDependencies()
	{
		Set<String> deps = new HashSet<>();
		deps.add("barcode");  // Depends on barcode extractor
		return deps;
	}

	@Override
	public Set<String> getPredecessors()
	{
		Set<String> preds = new HashSet<>();
		preds.add("barcode");  // Requires barcode extractor to run first
		return preds;
	}

	@Override
	public boolean isAutoEnabled()
	{
		return true;
	}

	@Override
	public ExecutionPhase getPhase()
	{
		return ExecutionPhase.PARSE;
	}

	@Override
	public ExecutionMode[] getSupportedModes()
	{
		return new ExecutionMode[]
		{
			ExecutionMode.SYNC,
			ExecutionMode.ASYNC_THREAD
		};
	}

	@Override
	public boolean canParse(String content)
	{
		return content != null && content.startsWith(SPAYD_PREFIX);
	}

	@Override
	public PluginResult parse(PluginContext context)
	{
		long startTime = System.currentTimeMillis();

		try
		{
			// Get barcodes from previous results
			@SuppressWarnings("unchecked")
			List<Barcode> barcodes = context.getSharedData("barcodes", List.class);

			if (barcodes == null || barcodes.isEmpty())
			{
				// No barcodes to parse
				PluginResult result = PluginResult.success(new ArrayList<>());
				result.addMetadata("payment_count", "0");
				return result;
			}

			List<SpaydPayment> payments = new ArrayList<>();

			for (Barcode barcode : barcodes)
			{
				if (barcode.isSpaydPayment())
				{
					SpaydPayment payment = parseSpayd(barcode.getRawContent());
					if (payment != null)
					{
						payment.setSourceBarcode(barcode.getId());
						payments.add(payment);
					}
				}
			}

			// Store results in context
			context.putSharedData(RESULT_KEY_PAYMENTS, payments);

			PluginResult result = PluginResult.success(payments);
			result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			result.addMetadata("payment_count", String.valueOf(payments.size()));

			return result;
		}
		catch (Exception e)
		{
			return PluginResult.failure("SPAYD parsing failed: " + e.getMessage());
		}
	}

	/**
	 * Parse SPAYD string into SpaydPayment object
	 */
	private SpaydPayment parseSpayd(String content)
	{
		if (!canParse(content))
		{
			return null;
		}

		Map<String, String> fields = parseFields(content);
		if (fields.isEmpty())
		{
			return null;
		}

		SpaydPayment payment = new SpaydPayment();
		SpaydPayment.SpaydFields paymentFields = payment.getFields();

		// ACC - Account (IBAN or Czech format)
		String account = fields.get("ACC");
		if (account != null)
		{
			paymentFields.setAccount(account);
		}

		// AM - Amount
		String amount = fields.get("AM");
		if (amount != null)
		{
			try
			{
				paymentFields.setAmount(Double.valueOf(amount));
			}
			catch (NumberFormatException e)
			{
				// Invalid amount, skip
			}
		}

		// CC - Currency code (default CZK)
		String currency = fields.get("CC");
		paymentFields.setCurrency(currency != null ? currency : "CZK");

		// X-VS - Variable symbol
		String vs = fields.get("X-VS");
		if (vs != null)
		{
			paymentFields.setVariableSymbol(vs);
		}

		// X-KS - Constant symbol
		String ks = fields.get("X-KS");
		if (ks != null)
		{
			paymentFields.setConstantSymbol(ks);
		}

		// X-SS - Specific symbol
		String ss = fields.get("X-SS");
		if (ss != null)
		{
			paymentFields.setSpecificSymbol(ss);
		}

		// MSG - Message for recipient
		String msg = fields.get("MSG");
		if (msg != null)
		{
			paymentFields.setMessage(decodeValue(msg));
		}

		// RN - Recipient name
		String rn = fields.get("RN");
		if (rn != null)
		{
			paymentFields.setRecipientName(decodeValue(rn));
		}

		// Calculate confidence based on completeness
		double confidence = calculateConfidence(paymentFields);
		payment.setConfidence(confidence);

		return payment;
	}

	/**
	 * Parse SPAYD fields into a map
	 */
	private Map<String, String> parseFields(String content)
	{
		Map<String, String> fields = new HashMap<>();

		// Remove SPD* prefix and split by *
		String data = content.substring(SPAYD_PREFIX.length());
		String[] parts = data.split("\\*");

		for (String part : parts)
		{
			int colonIndex = part.indexOf(':');
			if (colonIndex > 0)
			{
				String key = part.substring(0, colonIndex).toUpperCase();
				String value = part.substring(colonIndex + 1);
				fields.put(key, value);
			}
		}

		return fields;
	}

	/**
	 * Decode URL-encoded value (SPAYD uses percent-encoding for special chars)
	 */
	private String decodeValue(String value)
	{
		if (value == null)
		{
			return null;
		}

		// Replace + with %2B for proper URL decoding
		String prepared = value.replace("+", "%2B");

		try
		{
			return URLDecoder.decode(prepared, StandardCharsets.UTF_8.name());
		}
		catch (UnsupportedEncodingException e)
		{
			return value;
		}
	}

	/**
	 * Calculate confidence score based on field completeness
	 */
	private double calculateConfidence(SpaydPayment.SpaydFields fields)
	{
		int totalFields = 4;  // account, amount, currency, vs are considered primary
		int filledFields = 0;

		if (fields.getAccount() != null && !fields.getAccount().isEmpty())
		{
			filledFields++;
		}
		if (fields.getAmount() != null && fields.getAmount() > 0)
		{
			filledFields++;
		}
		if (fields.getCurrency() != null)
		{
			filledFields++;
		}
		if (fields.getVariableSymbol() != null)
		{
			filledFields++;
		}

		// Base confidence 0.7 + up to 0.3 based on completeness
		return 0.7 + (0.3 * filledFields / totalFields);
	}
}
