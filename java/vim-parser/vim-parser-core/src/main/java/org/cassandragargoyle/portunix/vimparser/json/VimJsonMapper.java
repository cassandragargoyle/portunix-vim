/*
 * SPDX-License-Identifier: MIT
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.LogicalType;

import java.io.IOException;

/**
 * Shared factory of the mappers that read and write VIM documents
 * <p>
 * The reader is strict on scalar types: a JSON scalar of the wrong type is a load error instead of
 * a silent conversion (number or boolean to String, any string including "NaN" to a number, string
 * or number to boolean, fractional number to integer). An integral float such as 245.0 is accepted
 * in an integer field, as the JSON Schema integer type does. The error message names the field
 * through the Jackson reference chain.
 *
 * @author Zdenek
 * @since 2026-10-26
 */
public final class VimJsonMapper
{
	private static final Class<?>[] INTEGRAL_TYPES = {int.class, Integer.class, long.class, Long.class};
	private static final Class<?>[] FLOATING_TYPES = {double.class, Double.class, float.class, Float.class};

	private VimJsonMapper()
	{
	}

	/**
	 * Mapper for reading VIM documents with strict scalar types
	 * <p>
	 * Unknown properties are tolerated for forward compatibility. A float in an untyped field
	 * ({@code Object}, {@code Map<String, Object>}) is read as {@code BigDecimal}, exact and without
	 * overflow.
	 *
	 * @return new strict reading mapper
	 */
	public static ObjectMapper strictReader()
	{
		return JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			// Object and Map<String, Object> fields keep floats exact, a Double would overflow 1e400 to Infinity
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.addModule(strictNumberModule())
			.withCoercionConfig(LogicalType.Textual, cfg -> cfg
				.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
			.withCoercionConfig(LogicalType.Boolean, cfg -> cfg
				.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
			.build();
	}

	/**
	 * Mapper for writing VIM documents
	 *
	 * @param indent pretty print the output
	 * @return new writing mapper
	 */
	public static ObjectMapper writer(boolean indent)
	{
		return JsonMapper.builder()
			.configure(SerializationFeature.INDENT_OUTPUT, indent)
			.build();
	}

	private static SimpleModule strictNumberModule()
	{
		SimpleModule module = new SimpleModule("vim-strict-numbers");
		for (Class<?> type : INTEGRAL_TYPES)
		{
			register(module, type, true);
		}
		for (Class<?> type : FLOATING_TYPES)
		{
			register(module, type, false);
		}
		return module;
	}

	@SuppressWarnings("unchecked")
	private static void register(SimpleModule module, Class<?> type, boolean integral)
	{
		module.addDeserializer((Class<Object>) type, new StrictNumberDeserializer(type, integral));
	}

	/**
	 * Number deserializer that accepts JSON numbers only
	 * <p>
	 * Rejects every JSON string, including the "NaN" and "Infinity" literals that the default
	 * deserializer accepts before any coercion config applies. For integer types it rejects a
	 * number with a non-zero fractional part instead of truncating it. For floating types it rejects
	 * a number that overflows to Infinity. Everything else, including integer range checks and null
	 * handling, is left to the default deserializer.
	 */
	private static final class StrictNumberDeserializer extends StdScalarDeserializer<Object>
	{
		private final JsonDeserializer<Object> delegate;
		private final boolean integral;

		@SuppressWarnings("unchecked")
		StrictNumberDeserializer(Class<?> type, boolean integral)
		{
			super(type);
			this.delegate = (JsonDeserializer<Object>) NumberDeserializers.find(type, type.getName());
			this.integral = integral;
		}

		@Override
		public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
		{
			if (p.hasToken(JsonToken.VALUE_STRING))
			{
				return ctxt.reportInputMismatch(this, "Cannot read String value (\"%s\") as %s, a JSON number is required",
					p.getText(), handledType().getName());
			}
			if (integral && p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)
				&& p.getDecimalValue().stripTrailingZeros().scale() > 0)
			{
				return ctxt.reportInputMismatch(this, "Cannot read fractional value (%s) as %s",
					p.getText(), handledType().getName());
			}
			Object value = delegate.deserialize(p, ctxt);
			if (isInfinite(value))
			{
				// A finite JSON number such as 1e400 overflows to Infinity, which is written back as a string
				return ctxt.reportInputMismatch(this, "Numeric value (%s) out of range of %s",
					p.getText(), handledType().getName());
			}
			return value;
		}

		private static boolean isInfinite(Object value)
		{
			return (value instanceof Double && ((Double) value).isInfinite())
				|| (value instanceof Float && ((Float) value).isInfinite());
		}

		@Override
		public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException
		{
			return delegate.getNullValue(ctxt);
		}

		@Override
		public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException
		{
			return delegate.getEmptyValue(ctxt);
		}

		@Override
		public LogicalType logicalType()
		{
			return delegate.logicalType();
		}
	}
}
