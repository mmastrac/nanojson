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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

//@formatter:off
/**
 * Light-weight JSON writer with state checking. Writes JSON to a {@link String}, an {@link OutputStream}, or an
 * {@link Appendable} such as a {@link StringBuilder}, a {@link Writer} a {@link PrintStream} or a {@link CharBuffer}.
 * 
 * <pre>
 * String json = JsonEmitter.string()
 *     .object()
 *         .array("a")
 *             .value(1)
 *             .value(2)
 *         .end()
 *         .value("b", false)
 *         .value("c", true)
 *     .end()
 * .close();
 * </pre>
 */
//@formatter:on
public class JsonWriter<T> {
	//@formatter:off
	/**
	 * Creates a new {@link JsonStringWriter}.
	 * 
     * <pre>
	 * String json = JsonEmitter.string()
	 *     .object()
	 *         .array("a")
	 *             .value(1)
	 *             .value(2)
	 *         .end()
	 *         .value("b", false)
	 *         .value("c", true)
	 *     .end()
	 * .close();
	 * </pre>
	 */
	//@formatter:on
	public static JsonStringWriter string() {
		return new JsonStringWriter();
	}

	/**
	 * Emits a single value (a JSON primitive such as a {@link Number}, {@link Boolean}, {@link String}, a {@link Map}
	 * or {@link JsonObject}, or a {@link Collection} or {@link JsonArray}.
	 * 
	 * Emit a {@link String}, JSON-escaped:
	 * 
	 * <pre>
	 * JsonEmitter.string("abc\n\"") // "\"abc\\n\\"\""
	 * </pre>
	 * 
	 * <pre>
	 * JsonObject obj = new JsonObject();
	 * obj.put("abc", 1);
	 * JsonEmitter.string(obj) // "{\"abc\":1}"
	 * </pre>
	 */
	public static String string(Object value) {
		return new JsonStringWriter().value(value).close();
	}

	public static JsonAppendableWriter on(Appendable appendable) {
		return new JsonAppendableWriter(appendable);
	}

	public static JsonAppendableWriter on(OutputStream out) {
		return new JsonAppendableWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
	}
}
