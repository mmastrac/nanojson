package com.grack.nanojson;

import java.math.BigDecimal;

/**
 * Lazily-parsed number for performance.
 */
@SuppressWarnings("serial")
class JsonLazyNumber extends Number {
	private String value;
	private boolean isDouble;

	JsonLazyNumber(String number, boolean isDoubleValue) {
		this.value = number;
		this.isDouble = isDoubleValue;
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
		return isDouble ? (int)Double.parseDouble(value) : Integer.parseInt(value);
	}

	@Override
	public long longValue() {
		return isDouble ? (long)Double.parseDouble(value) : Long.parseLong(value);
	}

	/**
	 * Avoid serializing {@link JsonLazyNumber}.
	 */
	private Object writeReplace() {
		return new BigDecimal(value);
	}
}
