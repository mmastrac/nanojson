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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;

/**
 * Test for {@link JsonWriter}.
 */
public class JsonWriterTest {
	private static final Charset UTF8 = StandardCharsets.UTF_8;

	// CHECKSTYLE_OFF: MagicNumber
	// CHECKSTYLE_OFF: JavadocMethod
	// CHECKSTYLE_OFF: EmptyBlock
	/**
	 * Test emitting simple values.
	 */
	@Test
	public void testSimpleValues() {
		assertEquals("true", JsonWriter.string().value(true).done());
		assertEquals("null", JsonWriter.string().nul().done());
		assertEquals("1.0", JsonWriter.string().value(1.0).done());
		assertEquals("1.0", JsonWriter.string().value(1.0f).done());
		assertEquals("1", JsonWriter.string().value(1).done());
		assertEquals("\"abc\"", JsonWriter.string().value("abc").done());
	}

	/**
	 * Write progressively longer strings to see if we can tickle a boundary
	 * exception.
	 */
	@Test
	public void testStreamWriterWithNonBMPStringAroundBufferSize() throws JsonParserException {
		char[] c = new char[JsonWriterBase.BUFFER_SIZE - 128];
		Arrays.fill(c, ' ');
		String base = new String(c);
		for (int i = 0; i < 256; i++) {
			base += " ";
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			String s = base + new String(new int[] { 0x10ffff }, 0, 1);
			JsonWriter.on(bytes).value(s).done();
			assertEquals(s, JsonParser.any().from(new String(bytes.toByteArray(), UTF8)));
		}
	}

	/**
	 * Write progressively longer strings to see if we can tickle a boundary
	 * exception.
	 */
	@Test
	public void testStreamWriterWithBMPStringAroundBufferSize() throws JsonParserException {
		char[] c = new char[JsonWriterBase.BUFFER_SIZE - 128];
		Arrays.fill(c, ' ');
		String base = new String(c);
		for (int i = 0; i < 256; i++) {
			base += " ";
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			String s = base + new String(new int[] { 0xffff }, 0, 1);
			JsonWriter.on(bytes).value(s).done();
			assertEquals(s, JsonParser.any().from(new String(bytes.toByteArray(), UTF8)));
		}
	}

	/**
	 * Write progressively longer string + array to see if we can tickle a
	 * boundary exception.
	 */
	@Test
	public void testStreamWriterWithArrayAroundBufferSize() throws JsonParserException {
		char[] c = new char[JsonWriterBase.BUFFER_SIZE - 128];
		Arrays.fill(c,  ' ');
		String base = new String(c);
		for (int i = 0; i < 256; i++) {
			base += " ";
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			String s = base + new String(new int[] { 0x10ffff }, 0, 1);
			JsonWriter.on(bytes).array().value(s).nul().end().done();
			String s2 = new String(bytes.toByteArray(), UTF8);
			JsonArray array = JsonParser.array().from(s2);
			assertEquals(s, array.get(0));
			assertEquals(null, array.get(1));
		}
	}

	/**
	 * Test various ways of writing null, as well as various situations.
	 */
	@Test
	public void testNull() {
		assertEquals("null", JsonWriter.string().value((String) null).done());
		assertEquals("null", JsonWriter.string().value((Number) null).done());
		assertEquals("null", JsonWriter.string().nul().done());
		assertEquals("[null]", JsonWriter.string().array().value((String) null)
				.end().done());
		assertEquals("[null]", JsonWriter.string().array().value((Number) null)
				.end().done());
		assertEquals("[null]", JsonWriter.string().array().nul().end().done());
		assertEquals("{\"a\":null}",
				JsonWriter.string().object().value("a", (String) null).end()
						.done());
		assertEquals("{\"a\":null}",
				JsonWriter.string().object().value("a", (Number) null).end()
						.done());
		assertEquals("{\"a\":null}", JsonWriter.string().object().nul("a")
				.end().done());
	}

	@Test
	public void testSeparateKeyWriting() {
		assertEquals("{\"a\":null}",
				JsonWriter.string().object().key("a").value((Number) null).end()
						.done());
		assertEquals("{\"a\":{\"b\":null}}",
				JsonWriter.string().object().key("a").object().value("b", (Number) null)
						.end().end().done());
	}

	/**
	 * Test escaping of chars < 256.
	 */
	@Test
	public void testStringControlCharacters() {
		StringBuilder chars = new StringBuilder();
		for (int i = 0; i < 0xa0; i++)
			chars.append((char) i);
		chars.append("\u20ff");

		assertEquals(
				"\"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000b\\f\\r\\u000e\\u000f\\u0010"
						+ "\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d"
						+ "\\u001e\\u001f !\\\"#$%&'()*+,-./0123456789:;<=>?@"
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\\u0080\\u0081\\u0082"
						+ "\\u0083\\u0084\\u0085\\u0086\\u0087\\u0088\\u0089\\u008a\\u008b\\u008c\\u008d\\u008e\\u008f"
						+ "\\u0090\\u0091\\u0092\\u0093\\u0094\\u0095\\u0096\\u0097\\u0098\\u0099\\u009a\\u009b\\u009c"
						+ "\\u009d\\u009e\\u009f\\u20ff\"",
				JsonWriter.string(chars.toString()));
	}

	/**
	 * Test escaping of chars < 256.
	 */
	@Test
	public void testEscape() {
		StringBuilder chars = new StringBuilder();
		for (int i = 0; i < 0xa0; i++)
			chars.append((char) i);

		assertEquals(
				"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000b\\f\\r\\u000e\\u000f\\u0010"
						+ "\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d"
						+ "\\u001e\\u001f !\\\"#$%&'()*+,-./0123456789:;<=>?@"
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\\u0080\\u0081\\u0082"
						+ "\\u0083\\u0084\\u0085\\u0086\\u0087\\u0088\\u0089\\u008a\\u008b\\u008c\\u008d\\u008e\\u008f"
						+ "\\u0090\\u0091\\u0092\\u0093\\u0094\\u0095\\u0096\\u0097\\u0098\\u0099\\u009a\\u009b\\u009c"
						+ "\\u009d\\u009e\\u009f",
				JsonWriter.escape(chars.toString()));
	}

	/**
	 * Torture test for UTF8 character encoding.
	 */
	@Test
	public void testBMPCharacters() throws Exception {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 0xD000; i++) {
			builder.append((char)i);
		}
		builder.append("\ue000");
		builder.append("\uefff");
		builder.append("\uf000");
		builder.append("\uffff");

		// Base string
		String s = JsonWriter.string(builder.toString());
		assertEquals(builder.toString(), (String)JsonParser.any().from(s));

		// Ensure that it also matches the PrintStream output
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		JsonWriter.on(new PrintStream(bytes, false, "UTF-8")).value(builder.toString()).done();
		assertEquals(builder.toString(), (String)JsonParser.any().from(new String(bytes.toByteArray(),
				UTF8)));

		// Ensure that it also matches the stream output
		bytes = new ByteArrayOutputStream();
		JsonWriter.on(bytes).value(builder.toString()).done();
		assertEquals(builder.toString(), (String)JsonParser.any().from(new String(bytes.toByteArray(),
				UTF8)));
	}

	/**
	 * Torture test for UTF8 character encoding outside the basic multilingual plane.
	 */
	@Test
	public void testNonBMP() throws Exception {
		StringBuilder builder = new StringBuilder();
		builder.appendCodePoint(0x10000); // Start of non-BMP
		builder.appendCodePoint(0x1f601); // GRINNING FACE WITH SMILING EYES
		builder.appendCodePoint(0x10ffff); // Character.MAX_CODE_POINT

		// Base string
		String s = JsonWriter.string(builder.toString());
		assertEquals(builder.toString(), (String)JsonParser.any().from(s));

		// Ensure that it also matches the PrintStream output
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		JsonWriter.on(new PrintStream(bytes, false, "UTF-8")).value(builder.toString()).done();
		assertEquals(builder.toString(), (String)JsonParser.any().from(new String(bytes.toByteArray(),
				UTF8)));

		// Ensure that it also matches the stream output
		bytes = new ByteArrayOutputStream();
		JsonWriter.on(bytes).value(builder.toString()).done();
		assertEquals(builder.toString(), (String)JsonParser.any().from(new String(bytes.toByteArray(),
				UTF8)));
	}

	/**
	 * Basic {@link OutputStream} smoke test.
	 */
	@Test
	public void testWriteToUTF8Stream() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		JsonWriter.on(bytes).object().value("a\n", 1)
				.value("b", 2).end().done();
		assertEquals("{\"a\\n\":1,\"b\":2}", new String(bytes.toByteArray(),
				UTF8));
	}

	/**
	 * Basic {@link PrintStream} smoke test.
	 */
	@Test
	public void testWriteToSystemOutLikeStream() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		JsonWriter.on(new PrintStream(bytes, false, "UTF-8")).object().value("a\n", 1)
				.value("b", 2).end().done();

		assertEquals("{\"a\\n\":1,\"b\":2}", new String(bytes.toByteArray(),
				UTF8));
	}

	/**
	 * Test escaping of / when following < to handle &lt;/script&gt;.
	 */
	@Test
	public void testScriptEndEscaping() {
		assertEquals("\"<\\/script>\"", JsonWriter.string("</script>"));
		assertEquals("\"/script\"", JsonWriter.string("/script"));
	}

	/**
	 * Test a simple array.
	 */
	@Test
	public void testArray() {
		String json = JsonWriter.string().array().value(true).value(false)
				.value(true).end().done();
		assertEquals("[true,false,true]", json);
	}

	/**
	 * Test an empty array.
	 */
	@Test
	public void testArrayEmpty() {
		String json = JsonWriter.string().array().end().done();
		assertEquals("[]", json);
	}

	/**
	 * Test the auto-conversion of Writables.
	 */
	@Test
	public void testWritable() {
		assertEquals("null", JsonWriter.string((JsonConvertible) () -> null));
		assertEquals("[]", JsonWriter.string((JsonConvertible) ArrayList::new));
		assertEquals("{}", JsonWriter.string((JsonConvertible) HashMap::new));
		assertEquals("\"\"", JsonWriter.string((JsonConvertible) () -> ""));
		assertEquals("1", JsonWriter.string((JsonConvertible) () -> Integer.valueOf(1)));
		assertEquals("1.0", JsonWriter.string((JsonConvertible) () -> Double.valueOf(1.0)));
		assertEquals("1", JsonWriter.string((JsonConvertible) () -> Long.valueOf(1)));
		assertEquals("1.0", JsonWriter.string((JsonConvertible) () -> Float.valueOf(1.0f)));
		assertEquals(
				"[null,[1,2,3],{\"a\":1,\"b\":2.0,\"c\":\"a\",\"d\":null,\"e\":[]}]",
				JsonWriter.string((JsonConvertible) () -> (JsonConvertible) () -> {
					ArrayList<Object> list = new ArrayList<>();
					list.add(null);
					list.add((JsonConvertible) () -> new int[] {1, 2, 3});
					list.add((JsonConvertible) () -> {
						HashMap<String, Object> map = new HashMap<>();
						map.put("a", 1);
						map.put("b", 2.0);
						map.put("c", "a");
						map.put("d", null);
						map.put("e", (JsonConvertible) ArrayList::new);
						return map;
					});
					return list;
				})
		);
		assertEquals(
				"Unable to handle type: class java.lang.Object",
				assertThrows(
						JsonWriterException.class,
						() -> JsonWriter.string((JsonConvertible) Object::new)
				).getMessage()
		);
		assertEquals(
				"Unable to handle type: class java.lang.Object",
				assertThrows(
						JsonWriterException.class,
						() -> JsonWriter.string((JsonConvertible) () -> Arrays.asList("d", 1, new Object()))
				).getMessage()
		);
	}

	/**
	 * Test an array of empty arrays.
	 */
	@Test
	public void testArrayOfEmpty() {
		String json = JsonWriter.string().array().array().end().array().end()
				.end().done();
		assertEquals("[[],[]]", json);
	}

	/**
	 * Test a nested array.
	 */
	@Test
	public void testNestedArray() {
		String json = JsonWriter.string().array().array().array().value(true)
				.value(false).value(true).end().end().end().done();
		assertEquals("[[[true,false,true]]]", json);
	}

	/**
	 * Test a nested array.
	 */
	@Test
	public void testNestedArray2() {
		String json = JsonWriter.string().array().value(true).array().array()
				.value(false).end().end().value(true).end().done();
		assertEquals("[true,[[false]],true]", json);
	}

	/**
	 * Test a simple object.
	 */
	@Test
	public void testObject() {
		String json = JsonWriter.string().object().value("a", true)
				.value("b", false).value("c", true).end().done();
		assertEquals("{\"a\":true,\"b\":false,\"c\":true}", json);
	}

	/**
	 * Test a simple object with indent.
	 */
	@Test
	public void testObjectIndent() {
		String json = JsonWriter.indent("  ").string().object()
				.value("a", true).value("b", false).value("c", true).end()
				.done();
		assertEquals("{\n  \"a\":true,\n  \"b\":false,\n  \"c\":true\n}", json);
	}

	/**
	 * Test a nested object.
	 */
	@Test
	public void testNestedObject() {
		String json = JsonWriter.string().object().object("a")
				.value("b", false).value("c", true).end().end().done();
		assertEquals("{\"a\":{\"b\":false,\"c\":true}}", json);
	}

	/**
	 * Test a nested object and array.
	 */
	@Test
	public void testNestedObjectArray() {
		//@formatter:off
		String json = JsonWriter.string()
				.object()
					.object("a")
						.array("b")
							.object()
								.value("a", 1)
								.value("b", 2)
							.end()
							.object()
								.value("c", 1.0)
								.value("d", 2.0)
							.end()
						.end()
						.value("c", JsonArray.from("a", "b", "c"))
					.end()
				.end()
			.done();
		//@formatter:on
		assertEquals(
				"{\"a\":{\"b\":[{\"a\":1,\"b\":2},{\"c\":1.0,\"d\":2.0}],\"c\":[\"a\",\"b\",\"c\"]}}",
				json);
	}

	/**
	 * Test a nested object and array.
	 */
	@Test
	public void testNestedObjectArrayIndent() {
		//@formatter:off
		String json = JsonWriter.indent("  ").string()
				.object()
					.object("a")
						.array("b")
							.object()
								.value("a", 1)
								.value("b", 2)
							.end()
							.object()
								.value("c", 1.0)
								.value("d", 2.0)
							.end()
						.end()
						.value("c", JsonArray.from("a", "b", "c"))
					.end()
				.end()
			.done();
		//@formatter:on

		assertEquals(
				"{\n  \"a\":{\n    \"b\":[{\n      \"a\":1,\n      \"b\":2\n    },{\n"
						+ "      \"c\":1.0,\n      \"d\":2.0\n    }],\n"
						+ "    \"c\":[\"a\",\"b\",\"c\"]\n  }\n}", json);
	}

	/**
	 * Tests the {@link Appendable} code.
	 */
	@Test
	public void testAppendable() {
		StringWriter writer = new StringWriter();
		JsonWriter.on(writer).object().value("abc", "def").end().done();
		assertEquals("{\"abc\":\"def\"}", writer.toString());
	}

	/**
	 * Tests the {@link OutputStream} code.
	 */
	@Test
	public void testOutputStream() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JsonWriter.on(out).object().value("abc", "def").end().done();
		assertEquals("{\"abc\":\"def\"}",
				new String(out.toByteArray(), UTF8));
	}

	@Test
	public void testQuickJson() {
		assertEquals("true", JsonWriter.string(true));
	}

	@Test
	public void testQuickJsonArray() {
		assertEquals("[1,2,3]", JsonWriter.string(JsonArray.from(1, 2, 3)));
	}

	@Test
	public void testQuickArray() {
		assertEquals("[1,2,3]", JsonWriter.string(Arrays.asList(1, 2, 3)));
	}

	@Test
	public void testQuickArrayEmpty() {
		assertEquals("[]", JsonWriter.string(Collections.emptyList()));
	}

	@Test
	public void testQuickObjectArray() {
		assertEquals("[1,2,3]", JsonWriter.string(new Object[] { 1, 2, 3 }));
	}

	@Test
	public void testQuickObjectArrayNested() {
		assertEquals(
				"[[1,2],[[3]]]",
				JsonWriter.string(new Object[] { new Object[] { 1, 2 },
						new Object[] { new Object[] { 3 } } }));
	}

	@Test
	public void testQuickObjectArrayEmpty() {
		assertEquals("[]", JsonWriter.string(new Object[0]));
	}

	@Test
	public void testObjectArrayInMap() {
		JsonObject o = new JsonObject();
		o.put("array of string", new String[] { "a", "b", "c" });
		o.put("array of Boolean", new Boolean[] { true, false });
		o.put("array of int", new int[] { 1, 2, 3 });
		o.put("array of JsonObject",
				new JsonObject[] { new JsonObject(), null });

		String[] bits = { "\"array of JsonObject\":[{},null]",
				"\"array of Boolean\":[true,false]",
				"\"array of string\":[\"a\",\"b\",\"c\"]",
				"\"array of int\":[1,2,3]" };
		String s = JsonWriter.string(o);
		for (String bit : bits) {
			assertTrue("Didn't contain " + bit, s.contains(bit));
		}
	}

	@Test
	public void testFailureNoKeyInObject() {
		try {
			JsonWriter.string().object().value(true).end().done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureNoKeyInObject2() {
		try {
			JsonWriter.string().object().value("a", 1).value(true).end().done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureKeyInArray() {
		try {
			JsonWriter.string().array().value("x", true).end().done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureKeyInArray2() {
		try {
			JsonWriter.string().array().value(1).value("x", true).end().done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test(expected = JsonWriterException.class)
	public void testFailureRepeatedKey() {
		JsonWriter.string().object().key("a").value("b", 2).end().done();
	}

	@Test(expected = JsonWriterException.class)
	public void testFailureRepeatedKey2() {
		JsonWriter.string().object().key("a").key("b").end().done();
	}

	@Test
	public void testFailureNotFullyClosed() {
		try {
			JsonWriter.string().array().value(1).done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureNotFullyClosed2() {
		try {
			JsonWriter.string().array().done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureEmpty() {
		try {
			JsonWriter.string().done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureEmpty2() {
		try {
			JsonWriter.string().end();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureMoreThanOneRoot() {
		try {
			JsonWriter.string().value(1).value(1).done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureMoreThanOneRoot2() {
		try {
			JsonWriter.string().array().value(1).end().value(1).done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureMoreThanOneRoot3() {
		try {
			JsonWriter.string().array().value(1).end().array().value(1).end()
					.done();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

}
