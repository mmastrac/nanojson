package com.grack.nanojson;

import java.math.BigDecimal;

/**
 * Lazily-parsed number for performance.
 */
@SuppressWarnings("serial")
class JsonLazyNumber extends Number {
	private String value;

	public JsonLazyNumber(String value) {
		this.value = value;
	}

	@Override
	public double doubleValue() {
		return Double.parseDouble(value);
	}

	@Override
	public float floatValue() {
		return Float.parseFloat(value);
	}

	@Override
	public int intValue() {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			try {
				return (int)Long.parseLong(value);
			} catch (NumberFormatException e2) {
				return new BigDecimal(value).intValue();
			}
		}
	}

	@Override
	public long longValue() {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e2) {
			return new BigDecimal(value).longValue();
		}
	}

	/**
	 * Avoid serializing {@link JsonLazyNumber}.
	 */
	private Object writeReplace() {
		return new BigDecimal(value);
	}
}
