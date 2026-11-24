/*
 * Copyright 2011 The nanojson Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.grack.nanojson;

import java.math.BigDecimal;
import java.math.BigInteger;

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
		if (isDouble)
			return (int)Double.parseDouble(value);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return new BigInteger(value).intValue();
		}
	}

	@Override
	public long longValue() {
		if (isDouble)
			return (long)Double.parseDouble(value);
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return new BigInteger(value).longValue();
		}
	}

	@Override
	public String toString() {
		return value;
	}

	/**
	 * Avoid serializing {@link JsonLazyNumber}.
	 */
	private Object writeReplace() {
		return new BigDecimal(value);
	}
}
