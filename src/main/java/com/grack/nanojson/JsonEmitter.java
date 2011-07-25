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
import java.io.PrintStream;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Stack;

//@formatter:off
/**
 * Light-weight JSON emitter with state checking. Emits JSON to an
 * {@link Appendable} such as a {@link StringBuilder}, a {@link Writer} a
 * {@link PrintStream} or a {@link CharBuffer}.
 * <pre>
 * StringBuilder builder = new StringBuilder();
 * new JsonEmitter(builder)
 *     .startObject()
 *         .startArray("a")
 *             .value(1)
 *             .value(2)
 *         .endArray()
 *         .value("b", false)
 *         .value("c", true)
 *     .endObject()
 * .end();
 * </pre>
 */
//@formatter:on
public class JsonEmitter {
	private final Appendable appendable;
	private Stack<State> states = new Stack<State>();

	private enum State {
		EMPTY, ARRAY_START, ARRAY, OBJECT_START, OBJECT, FINI
	}

	public JsonEmitter(Appendable appendable) {
		this.appendable = appendable;
		states.push(State.EMPTY);
	}

	public JsonEmitter array(Collection<?> c) {
		return array(null, c);
	}

	public JsonEmitter array(String key, Collection<?> c) {
		if (key == null)
			startArray();
		else
			startArray(key);

		for (Object o : c) {
			if (o == null)
				nul();
			else if (o instanceof String)
				value((String)o);
			else if (o instanceof Number)
				rawValue(((Number)o).toString());
			else if (o instanceof Boolean)
				value((Boolean)o);
			else if (o instanceof Collection)
				array((Collection<?>)o);
			else if (o instanceof Map)
				object((Map<?, ?>)o);
			else
				throw new JsonEmitterException("Unable to handle type: " + o.getClass());
		}

		return endArray();
	}

	public JsonEmitter object(Map<?, ?> map) {
		return object(null, map);
	}

	public JsonEmitter object(String key, Map<?, ?> map) {
		if (key == null)
			startObject();
		else
			startObject(key);

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object o = entry.getValue();
			if (!(entry.getKey() instanceof String))
				throw new JsonEmitterException("Invalid key type for map: "
						+ (entry.getKey() == null ? "null" : entry.getKey().getClass()));
			String k = (String)entry.getKey();
			if (o == null)
				nul(k);
			else if (o instanceof String)
				value(k, (String)o);
			else if (o instanceof Number)
				rawValue(k, ((Number)o).toString());
			else if (o instanceof Boolean)
				value(k, (Boolean)o);
			else if (o instanceof Collection)
				array(k, (Collection<?>)o);
			else if (o instanceof Map)
				object(k, (Map<?, ?>)o);
			else
				throw new JsonEmitterException("Unable to handle type: " + o.getClass());
		}

		return endObject();
	}

	/**
	 * Emits a 'null' token.
	 */
	public JsonEmitter nul() {
		return value(null);
	}

	/**
	 * Emits a 'null' token with a key.
	 */
	public JsonEmitter nul(String key) {
		return value(key, null);
	}

	/**
	 * Emits a string value (or null).
	 */
	public JsonEmitter value(String s) {
		preValue();
		if (s == null)
			raw("null");
		else
			emitStringValue(s);
		post();
		return this;
	}

	/**
	 * Emits an integer value.
	 */
	public JsonEmitter value(int i) {
		preValue();
		raw(Integer.toString(i));
		post();
		return this;
	}

	/**
	 * Emits a boolean value.
	 */
	public JsonEmitter value(boolean b) {
		preValue();
		raw(Boolean.toString(b));
		post();
		return this;
	}

	/**
	 * Emits a double value.
	 */
	public JsonEmitter value(double d) {
		preValue();
		raw(Double.toString(d));
		post();
		return this;
	}

	/**
	 * Emits a string value (or null) with a key.
	 */
	public JsonEmitter value(String key, String s) {
		preValue(key);
		if (s == null)
			raw("null");
		else
			emitStringValue(s);
		post();
		return this;
	}

	/**
	 * Emits an integer value with a key.
	 */
	public JsonEmitter value(String key, int i) {
		preValue(key);
		raw(Integer.toString(i));
		post();
		return this;
	}

	/**
	 * Emits a boolean value with a key.
	 */
	public JsonEmitter value(String key, boolean b) {
		preValue(key);
		raw(Boolean.toString(b));
		post();
		return this;
	}

	/**
	 * Emits a double value with a key.
	 */
	public JsonEmitter value(String key, double d) {
		preValue(key);
		raw(Double.toString(d));
		post();
		return this;
	}

	/**
	 * Starts an array.
	 */
	public JsonEmitter startArray() {
		preValue();
		states.push(State.ARRAY_START);
		raw("[");
		return this;
	}

	/**
	 * Starts an object.
	 */
	public JsonEmitter startObject() {
		preValue();
		states.push(State.OBJECT_START);
		raw("{");
		return this;
	}

	/**
	 * Starts an array within an object, prefixed with a key.
	 */
	public JsonEmitter startArray(String key) {
		preValue(key);
		states.push(State.ARRAY_START);
		raw("[");
		return this;
	}

	/**
	 * Starts an object within an object, prefixed with a key.
	 */
	public JsonEmitter startObject(String key) {
		preValue(key);
		states.push(State.OBJECT_START);
		raw("{");
		return this;
	}

	/**
	 * Ends the current array.
	 */
	public JsonEmitter endArray() {
		raw("]");
		states.pop();
		post();
		return this;
	}

	/**
	 * Ends the current object.
	 */
	public JsonEmitter endObject() {
		raw("}");
		states.pop();
		post();
		return this;
	}

	/**
	 * Ensures that the object is in the finished state.
	 * 
	 * @throws JsonEmitterException
	 *             if the written JSON is not properly balanced, ie: all arrays and objects that were started have been
	 *             properly ended.
	 */
	public void end() {
		if (states.peek() != State.FINI)
			throw new JsonEmitterException("JSON was not properly balanced");
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
			throw new JsonEmitterException(e);
		}
	}

	private void raw(char c) {
		try {
			appendable.append(c);
		} catch (IOException e) {
			throw new JsonEmitterException(e);
		}
	}

	private void pre() {
		switch (states.peek()) {
		case ARRAY_START:
			states.pop();
			states.push(State.ARRAY);
			break;
		case ARRAY:
			raw(",");
			break;
		case OBJECT_START:
			states.pop();
			states.push(State.OBJECT);
			break;
		case OBJECT:
			raw(",");
			break;
		case FINI:
			throw new JsonEmitterException("Invalid call to emit a value in a finished JSON writer");
		default:
			// Nothing
		}
	}

	private void post() {
		switch (states.peek()) {
		case EMPTY:
			states.pop();
			states.push(State.FINI);
			break;
		default:
			// Nothing
		}
	}

	private void preValue() {
		pre();

		if (states.peek() == State.OBJECT_START || states.peek() == State.OBJECT) {
			throw new JsonEmitterException("Invalid call to emit a keyless value while writing an object");
		}
	}

	private void preValue(String key) {
		pre();

		emitStringValue(key);
		raw(":");

		if (states.peek() != State.OBJECT_START && states.peek() != State.OBJECT) {
			throw new JsonEmitterException("Invalid call to emit a key value while not writing an object");
		}
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
					raw("\\u" + t.substring(t.length() - 4));
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