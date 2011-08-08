/**
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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Stack;

/**
 * Internal class that handles emitting to an {@link Appendable}. Users only see the public subclasses,
 * {@link JsonStringWriter} and {@link JsonAppendableWriter}.
 * 
 * @param <SELF>
 *            A subclass of {@link JsonWriterBase}.
 */
class JsonWriterBase<SELF extends JsonWriterBase<SELF>> {
	protected final Appendable appendable;
	private Stack<Boolean> states = new Stack<Boolean>();
	private boolean first = true;
	private boolean inObject;

	JsonWriterBase(Appendable appendable) {
		this.appendable = appendable;
	}

	/**
	 * This is guaranteed to be safe as the type of "this" will always be the type of "SELF".
	 */
	@SuppressWarnings("unchecked")
	private SELF castThis() {
		return (SELF)this;
	}

	public SELF array(Collection<?> c) {
		return array(null, c);
	}

	public SELF array(String key, Collection<?> c) {
		if (key == null)
			array();
		else
			array(key);

		for (Object o : c) {
			value(o);
		}

		return end();
	}

	public SELF object(Map<?, ?> map) {
		return object(null, map);
	}

	public SELF object(String key, Map<?, ?> map) {
		if (key == null)
			object();
		else
			object(key);

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object o = entry.getValue();
			if (!(entry.getKey() instanceof String))
				throw new JsonWriterException("Invalid key type for map: "
						+ (entry.getKey() == null ? "null" : entry.getKey().getClass()));
			String k = (String)entry.getKey();
			value(k, o);
		}

		return end();
	}

	/**
	 * Emits a 'null' token.
	 */
	public SELF nul() {
		return value(null);
	}

	/**
	 * Emits a 'null' token with a key.
	 */
	public SELF nul(String key) {
		return value(key, null);
	}

	/**
	 * Emits an object if it is a JSON-compatible type, otherwise throws an exception.
	 */
	public SELF value(Object o) {
		if (o == null)
			return nul();
		else if (o instanceof String)
			return value((String)o);
		else if (o instanceof Number) {
			rawValue(((Number)o).toString());
			return castThis();
		} else if (o instanceof Boolean)
			return value((boolean)(Boolean)o);
		else if (o instanceof Collection)
			return array((Collection<?>)o);
		else if (o instanceof Map)
			return object((Map<?, ?>)o);
		else
			throw new JsonWriterException("Unable to handle type: " + o.getClass());
	}

	/**
	 * Emits an object with a key if it is a JSON-compatible type, otherwise throws an exception.
	 */
	public SELF value(String key, Object o) {
		if (o == null)
			return nul(key);
		else if (o instanceof String)
			return value(key, (String)o);
		else if (o instanceof Number) {
			rawValue(key, ((Number)o).toString());
			return castThis();
		} else if (o instanceof Boolean)
			return value(key, (boolean)(Boolean)o);
		else if (o instanceof Collection)
			return array(key, (Collection<?>)o);
		else if (o instanceof Map)
			return object(key, (Map<?, ?>)o);
		else
			throw new JsonWriterException("Unable to handle type: " + o.getClass());
	}

	/**
	 * Emits a string value (or null).
	 */
	public SELF value(String s) {
		preValue();
		if (s == null)
			raw("null");
		else
			emitStringValue(s);
		post();
		return castThis();
	}

	/**
	 * Emits an integer value.
	 */
	public SELF value(int i) {
		preValue();
		raw(Integer.toString(i));
		post();
		return castThis();
	}

	/**
	 * Emits a boolean value.
	 */
	public SELF value(boolean b) {
		preValue();
		raw(Boolean.toString(b));
		post();
		return castThis();
	}

	/**
	 * Emits a double value.
	 */
	public SELF value(double d) {
		preValue();
		raw(Double.toString(d));
		post();
		return castThis();
	}

	/**
	 * Emits a string value (or null) with a key.
	 */
	public SELF value(String key, String s) {
		preValue(key);
		if (s == null)
			raw("null");
		else
			emitStringValue(s);
		post();
		return castThis();
	}

	/**
	 * Emits an integer value with a key.
	 */
	public SELF value(String key, int i) {
		preValue(key);
		raw(Integer.toString(i));
		post();
		return castThis();
	}

	/**
	 * Emits a boolean value with a key.
	 */
	public SELF value(String key, boolean b) {
		preValue(key);
		raw(Boolean.toString(b));
		post();
		return castThis();
	}

	/**
	 * Emits a double value with a key.
	 */
	public SELF value(String key, double d) {
		preValue(key);
		raw(Double.toString(d));
		post();
		return castThis();
	}

	/**
	 * Starts an array.
	 */
	public SELF array() {
		preValue();
		states.push(inObject);
		inObject = false;
		first = true;
		raw("[");
		return castThis();
	}

	/**
	 * Starts an object.
	 */
	public SELF object() {
		preValue();
		states.push(inObject);
		inObject = true;
		first = true;
		raw("{");
		return castThis();
	}

	/**
	 * Starts an array within an object, prefixed with a key.
	 */
	public SELF array(String key) {
		preValue(key);
		states.push(inObject);
		inObject = false;
		first = true;
		raw("[");
		return castThis();
	}

	/**
	 * Starts an object within an object, prefixed with a key.
	 */
	public SELF object(String key) {
		preValue(key);
		states.push(inObject);
		inObject = true;
		first = true;
		raw("{");
		return castThis();
	}

	/**
	 * Ends the current array or object.
	 */
	public SELF end() {
		if (states.size() == 0)
			throw new JsonWriterException("Invalid call to end()");

		if (inObject) {
			raw("}");
		} else {
			raw("]");
		}

		inObject = states.pop();
		post();
		return castThis();
	}

	/**
	 * Ensures that the object is in the finished state.
	 * 
	 * @throws JsonWriterException
	 *             if the written JSON is not properly balanced, ie: all arrays and objects that were started have been
	 *             properly ended.
	 */
	protected void closeInternal() {
		if (states.size() > 0)
			throw new JsonWriterException("Unclosed JSON objects and/or arrays when closing writer");
		if (first)
			throw new JsonWriterException("Nothing was written to the JSON writer");
	}

	private void rawValue(String s) {
		preValue();
		raw(s);
		post();
	}

	private void rawValue(String key, String s) {
		preValue(key);
		raw(s);
		post();
	}

	private void raw(String s) {
		try {
			appendable.append(s);
		} catch (IOException e) {
			throw new JsonWriterException(e);
		}
	}

	private void raw(char c) {
		try {
			appendable.append(c);
		} catch (IOException e) {
			throw new JsonWriterException(e);
		}
	}

	private void pre() {
		if (first) {
			first = false;
		} else {
			if (states.size() == 0)
				throw new JsonWriterException("Invalid call to emit a value in a finished JSON writer");
			raw(",");
		}
	}

	private void post() {
	}

	private void preValue() {
		if (inObject)
			throw new JsonWriterException("Invalid call to emit a keyless value while writing an object");

		pre();
	}

	private void preValue(String key) {
		if (!inObject)
			throw new JsonWriterException("Invalid call to emit a key value while not writing an object");

		pre();

		emitStringValue(key);
		raw(":");
	}

	/**
	 * Emits a quoted string value, escaping characters that are required to be escaped.
	 */
	private void emitStringValue(String s) {
		raw('"');
		char b = 0, c = 0;
		for (int i = 0; i < s.length(); i++) {
			b = c;
			c = s.charAt(i);

			switch (c) {
			case '\\':
			case '"':
				raw('\\');
				raw(c);
				break;
			case '/':
				// Special case to ensure that </script> doesn't appear in JSON
				// output
				if (b == '<')
					raw('\\');
				raw(c);
				break;
			case '\b':
				raw("\\b");
				break;
			case '\t':
				raw("\\t");
				break;
			case '\n':
				raw("\\n");
				break;
			case '\f':
				raw("\\f");
				break;
			case '\r':
				raw("\\r");
				break;
			default:
				if (shouldBeEscaped(c)) {
					String t = "000" + Integer.toHexString(c);
					raw("\\u" + t.substring(t.length() - "0000".length()));
				} else {
					raw(c);
				}
			}
		}

		raw('"');
	}

	/**
	 * json.org spec says that all control characters must be escaped.
	 */
	private boolean shouldBeEscaped(char c) {
		return c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100');
	}
}
