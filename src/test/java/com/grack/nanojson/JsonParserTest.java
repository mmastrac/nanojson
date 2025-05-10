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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link JsonParser}.
 */
class JsonParserTest {
	private static final Charset UTF8;

	static {
		UTF8 = Charset.forName("UTF-8");
	}

	// CHECKSTYLE_OFF: MagicNumber
	// CHECKSTYLE_OFF: JavadocMethod
	// CHECKSTYLE_OFF: EmptyBlock
	@Test
	void whitespace() throws JsonParserException {
		assertEquals(JsonObject.class,
				JsonParser.object().from(" \t\r\n  { \t\r\n \"abc\"   \t\r\n : \t\r\n  1 \t\r\n  }  \t\r\n   ")
						.getClass());
		assertEquals("{}", JsonParser.object().from("{}").toString());
	}

	@Test
	void whitespaceSimpler() throws JsonParserException {
		assertEquals(JsonObject.class,
				JsonParser.object().from(" {} ")
						.getClass());
	}


	@Test
	void writerOutput() throws JsonParserException {
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
						.value("c", JsonArray.from("v0", "v1", "v2"))
					.end()
				.end()
			.done();
		//@formatter:on
		
		// Just make sure it can be read - don't validate
		JsonParser.object().from(json);
	}

	@Test
	void emptyObject() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.object().from("{}").getClass());
		assertEquals("{}", JsonParser.object().from("{}").toString());
	}

	@Test
	void objectOneElement() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.object().from("{\"a\":1}").getClass());
		assertEquals("{a=1}", JsonParser.object().from("{\"a\":1}").toString());
	}

	@Test
	void objectTwoElements() throws JsonParserException {
		JsonObject obj = JsonParser.object().from("{\"a\":1,\"B\":1}");
		assertEquals(JsonObject.class, obj.getClass());
		assertEquals(1, obj.get("B"));
		assertEquals(1, obj.get("a"));
		assertEquals(2, obj.size());
	}

	@Test
	void emptyArray() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.array().from("[]").getClass());
		assertEquals("[]", JsonParser.array().from("[]").toString());
	}

	@Test
	void arrayOneElement() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.array().from("[1]").getClass());
		assertEquals("[1]", JsonParser.array().from("[1]").toString());
	}

	@Test
	void arrayTwoElements() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.array().from("[1,1]").getClass());
		assertEquals("[1, 1]", JsonParser.array().from("[1,1]").toString());
	}

	@Test
	void basicTypes() throws JsonParserException {
		assertEquals("true", JsonParser.any().from("true").toString());
		assertEquals("false", JsonParser.any().from("false").toString());
		assertNull(JsonParser.any().from("null"));
		assertEquals("1", JsonParser.any().from("1").toString());
		assertEquals("1.0", JsonParser.any().from("1.0").toString());
		assertEquals("", JsonParser.any().from("\"\"").toString());
		assertEquals("a", JsonParser.any().from("\"a\"").toString());
	}

	@Test
	void arrayWithEverything() throws JsonParserException {
		JsonArray a = JsonParser.array().from("[1, -1.0e6, \"abc\", [1,2,3], {\"abc\":123}, true, false]");
		assertEquals("[1, -1000000.0, abc, [1, 2, 3], {abc=123}, true, false]", a.toString());
		assertEquals(1.0, a.getDouble(0), 0.001f);
		assertEquals(1, a.getInt(0));
		assertEquals(-1000000, a.getInt(1));
		assertEquals(-1000000, a.getDouble(1), 0.001f);
		assertEquals("abc", a.getString(2));
		assertEquals(1, a.getArray(3).getInt(0));
		assertEquals(123, a.getObject(4).getInt("abc"));
		assertTrue(a.getBoolean(5));
		assertFalse(a.getBoolean(6));
	}

	@Test
	void objectWithEverything() throws JsonParserException {
		// TODO: Is this deterministic if we use string keys?
		JsonObject o = JsonParser.object().from(
				"{\"abc\":123, \"def\":456.0, \"ghi\":[true, false], \"jkl\":null, \"mno\":true}");

		assertNull(o.get("jkl"));
		assertTrue(o.containsKey("jkl"));
		assertEquals(123, o.get("abc"));
		assertEquals(Arrays.asList(true, false), o.get("ghi"));
		assertEquals(456.0, o.get("def"));
		assertEquals(true, o.get("mno"));
		assertEquals(5, o.size());

		assertEquals(123, o.getInt("abc"));
		assertEquals(456, o.getInt("def"));
		assertTrue(o.getArray("ghi").getBoolean(0));
		assertNull(o.get("jkl"));
		assertTrue(o.isNull("jkl"));
		assertTrue(o.getBoolean("mno"));
	}

	@Test
	void stringEscapes() throws JsonParserException {
		assertEquals("\n", JsonParser.any().from("\"\\n\""));
		assertEquals("\r", JsonParser.any().from("\"\\r\""));
		assertEquals("\t", JsonParser.any().from("\"\\t\""));
		assertEquals("\b", JsonParser.any().from("\"\\b\""));
		assertEquals("\f", JsonParser.any().from("\"\\f\""));
		assertEquals("/", JsonParser.any().from("\"/\""));
		assertEquals("\\", JsonParser.any().from("\"\\\\\""));
		assertEquals("\"", JsonParser.any().from("\"\\\"\""));
		assertEquals("\0", JsonParser.any().from("\"\\u0000\""));
		assertEquals("\u8000", JsonParser.any().from("\"\\u8000\""));
		assertEquals("\uffff", JsonParser.any().from("\"\\uffff\""));
		assertEquals("\uFFFF", JsonParser.any().from("\"\\uFFFF\""));

		assertEquals("all together: \\/\n\r\t\b\f (fin)",
				JsonParser.any().from("\"all together: \\\\\\/\\n\\r\\t\\b\\f (fin)\""));
	}

	@Test
	void stringEscapesAroundBufferBoundary() throws JsonParserException {
		char[] c = new char[JsonTokener.BUFFER_SIZE - 1024];
		Arrays.fill(c,  ' ');
		String base = new String(c);
		for (int i = 0; i < 2048; i++) {
			base += " ";
			assertEquals("\u0055", JsonParser.any().from(base + "\"\\u0055\""));
		}
	}

	@Test
	void stringsAroundBufferBoundary() throws JsonParserException {
		char[] c = new char[JsonTokener.BUFFER_SIZE - 16];
		Arrays.fill(c,  ' ');
		String base = new String(c);
		for (int i = 0; i < 32; i++) {
			base += " ";
			assertEquals(base, JsonParser.any().from('"' + base + '"'));
		}
	}

	@Test
	void numbers() throws JsonParserException {
		String[] testCases = new String[] { "0", "1", "-0", "-1", "0.1", "1.1", "-0.1", "0.10", "-0.10", "0e1", "0e0",
				"-0e-1", "0.0e0", "-0.0e0", "9" };
		for (String testCase : testCases) {
			Number n = (Number)JsonParser.any().from(testCase);
			assertEquals(Double.parseDouble(testCase), n.doubleValue(), Double.MIN_NORMAL);
			Number n2 = (Number)JsonParser.any().from(testCase.toUpperCase());
			assertEquals(Double.parseDouble(testCase.toUpperCase()), n2.doubleValue(), Double.MIN_NORMAL);
		}
	}

	/**
	 * Test that negative zero ends up as negative zero in both the parser and the writer.
	 */
	@Test
	void negativeZero() throws JsonParserException {
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0.0")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0.0e0")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0e0")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0e1")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0e-1")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0e-0")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0e-01")).doubleValue()));
		assertEquals("-0.0", Double.toString(((Number)JsonParser.any().from("-0e-000000000001")).doubleValue()));

		assertEquals("-0.0", JsonWriter.string(-0.0));
		assertEquals("-0.0", JsonWriter.string(-0.0f));
	}

	/**
	 * Test the basic numbers from -100 to 100 as a sanity check.
	 */
	@Test
	void basicNumbers() throws JsonParserException {
		for (int i = -100; i <= +100; i++) {
			assertEquals(i, (int)(Integer)JsonParser.any().from("" + i));
		}
	}

	@Test
	void bigint() throws JsonParserException {
		JsonObject o = JsonParser.object().from("{\"v\":123456789123456789123456789}");
		BigInteger bigint = (BigInteger)o.get("v");
		assertEquals("123456789123456789123456789", bigint.toString());
	}

	@Test
	void failWrongType() {
		try {
			JsonParser.object().from("1");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 1, "did not contain the correct type");
		}
	}

	@Test
	void failNull() {
		try {
			JsonParser.object().from("null");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 4, "did not contain the correct type");
		}
	}

	@Test
	void failNoJson1() {
		try {
			JsonParser.object().from("");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 0);
		}
	}

	@Test
	void failNoJson2() {
		try {
			JsonParser.object().from(" ");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	void failNoJson3() {
		try {
			JsonParser.object().from("  ");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	void failNumberEdgeCases() {
		String[] edgeCases = { "-", ".", "e", "01", "-01", "+01", "01.1", "-01.1", "+01.1", ".1", "-.1", "+.1", "+1",
				"0.", "-0.", "+0.", "0.e", "-0.e", "+0.e", "0e", "-0e", "+0e", "0e-", "-0e-", "+0e-", "0e+", "-0e+",
				"+0e+", "-e", "+e", "2.", "-2.", "-1.e1", "1.e1", "0.e1" };
		for (String edgeCase : edgeCases) {
			try {
				JsonParser.any().from(edgeCase);
				fail("Should have failed to parse: " + edgeCase);
			} catch (JsonParserException e) {
				testException(e, 1, 1);
			}

			// Should fail in uppercase too
			try {
				JsonParser.any().from(edgeCase.toUpperCase());
				fail("Should have failed to parse: " + edgeCase.toUpperCase());
			} catch (JsonParserException e) {
				testException(e, 1, 1);
			}

			// All these should fail with lazy numbers too
			try {
				JsonParser.any().withLazyNumbers().from(edgeCase);
				fail("Should have failed to parse: " + edgeCase);
			} catch (JsonParserException e) {
				testException(e, 1, 1);
			}
		}
	}

	/**
	 * See http://seriot.ch/json/parsing.html and https://github.com/mmastrac/nanojson/issues/3.
	 */
	@Test
	void failNumberEdgeCasesFromJSONSuite() {
		String[] edgeCases = { "[-2.]", "[0.e1]", "[2.e+3]", "[2.e-3]", "[2.e3]", "[1.]" };
		for (String edgeCase : edgeCases) {
			try {
				JsonParser.array().from(edgeCase);
				fail("Should have failed to parse: " + edgeCase);
			} catch (JsonParserException e) {
				testException(e, 1, 2);
			}
		}
	}

	/**
	 * See http://seriot.ch/json/parsing.html and https://github.com/mmastrac/nanojson/issues/3.
	 */
	@Test
	void failNumberEdgeCasesFromJSONSuiteNoArray() {
		String[] edgeCases = { "-2.", "0.e1", "2.e+3", "2.e-3", "2.e3", "1." };
		for (String edgeCase : edgeCases) {
			try {
				JsonParser.any().from(edgeCase);
				fail("Should have failed to parse: " + edgeCase);
			} catch (JsonParserException e) {
				testException(e, 1, 1);
			}
		}
	}

	@Test
	void failBustedNumber1() {
		try {
			// There's no 'f' in double, but it treats it as a new token
			JsonParser.object().from("123f");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 4);
		}
	}

	@Test
	void failBustedNumber2() {
		try {
			// Badly formed number
			JsonParser.object().from("-1-1");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	void failBustedString1() {
		try {
			// Missing " at end
			JsonParser.object().from("\"abc");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	void failBustedString2() {
		try {
			// \n in middle of string
			JsonParser.object().from("\"abc\n\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 2, 1);
		}
	}

	@Test
	void failBustedString3() {
		try {
			// Bad escape "\x" in middle of string
			JsonParser.object().from("\"abc\\x\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 6);
		}
	}

	@Test
	void failBustedString4() {
		try {
			// Bad escape "\\u123x" in middle of string
			JsonParser.object().from("\"\\u123x\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 7);
		}
	}

	@Test
	void failBustedString5() {
		try {
			// Incomplete unicode escape
			JsonParser.object().from("\"\\u222\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 7);
		}
	}

	@Test
	void failBustedString6() {
		try {
			// String that terminates halfway through a unicode escape
			JsonParser.object().from("\"\\u222");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 6);
		}
	}

	@Test
	void failBustedString7() {
		try {
			// String that terminates halfway through a regular escape
			JsonParser.object().from("\"\\");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	void failArrayTrailingComma1() {
		try {
			JsonParser.object().from("[,]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	void failArrayTrailingComma2() {
		try {
			JsonParser.object().from("[1,]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 4);
		}
	}

	@Test
	void failObjectTrailingComma1() {
		try {
			JsonParser.object().from("{,}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	void failObjectTrailingComma2() {
		try {
			JsonParser.object().from("{\"abc\":123,}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 12);
		}
	}

	@Test
	void failObjectBadKey1() {
		try {
			JsonParser.object().from("{true:1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	void failObjectBadKey2() {
		try {
			JsonParser.object().from("{2:1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	void failObjectBadColon1() {
		try {
			JsonParser.object().from("{\"abc\":}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 8);
		}
	}

	@Test
	void failObjectBadColon2() {
		try {
			JsonParser.object().from("{\"abc\":1:}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 9);
		}
	}

	@Test
	void failObjectBadColon3() {
		try {
			JsonParser.object().from("{:\"abc\":1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	void failBadKeywords1() {
		try {
			JsonParser.object().from("truef");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'truef'");
		}
	}

	@Test
	void failBadKeywords2() {
		try {
			JsonParser.object().from("true1");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 5);
		}
	}

	@Test
	void failBadKeywords3() {
		try {
			JsonParser.object().from("tru");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'tru'");
		}
	}

	@Test
	void failBadKeywords4() {
		try {
			JsonParser.object().from("[truef,true]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2, "'truef'");
		}
	}

	@Test
	void failBadKeywords5() {
		try {
			JsonParser.object().from("grue");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'grue'");
		}
	}

	@Test
	void failBadKeywords6() {
		try {
			JsonParser.object().from("trueeeeeeeeeeeeeeeeeeee");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'trueeeeeeeeeeee'");
		}
	}

	@Test
	void failBadKeywords7() {
		try {
			JsonParser.object().from("g");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'g'");
		}
	}

	@Test
	void failTrailingCommaMultiline() {
		String testString = "{\n\"abc\":123,\n\"def\":456,\n}";
		try {
			JsonParser.object().from(testString);
			fail();
		} catch (JsonParserException e) {
			testException(e, 4, 1);
		}
	}

	/**
	 * Ensures that we're correctly tracking UTF-8 character positions.
	 */
	@Test
	void failTrailingCommaUTF8() {
		ByteArrayInputStream in1 = new ByteArrayInputStream("{\n\"abc\":123,\"def\":456,}".getBytes(Charset
				.forName("UTF-8")));
		ByteArrayInputStream in2 = new ByteArrayInputStream(
				"{\n\"\ub123\ub124\ub125\":123,\"def\":456,}".getBytes(UTF8));
		JsonParserException e1;

		try {
			JsonParser.object().from(in1);
			fail();
			return;
		} catch (JsonParserException e) {
			e1 = e;
		}

		try {
			JsonParser.object().from(in2);
			fail();
		} catch (JsonParserException e) {
			testException(e, e1.getLinePosition(), e1.getCharPosition());
		}
	}

	@Test
	void encodingUTF8() throws JsonParserException {
		testEncoding(UTF8);
		testEncodingBOM(UTF8);
	}

	@Test
	void encodingUTF16LE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-16LE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	void encodingUTF16BE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-16BE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	void encodingUTF32LE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-32LE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	void encodingUTF32BE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-32BE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	void validUTF8Codepoint() throws JsonParserException {
		assertEquals("\ud83d\ude8a",
				JsonParser.any().from(new ByteArrayInputStream("\"\ud83d\ude8a\"".getBytes(UTF8))));
	}

	@Test
	void validUTF8Codepoint2() throws JsonParserException {
		assertEquals("\u2602",
				JsonParser.any().from(new ByteArrayInputStream("\"\u2602\"".getBytes(UTF8))));
	}

	@Test
	void illegalUTF8Bytes() {
		// Test the always-illegal bytes
		int[] failures = new int[] { 0xc0, 0xc1, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff };
		for (int i = 0; i < failures.length; i++) {
			try {
				JsonParser.object().from(new ByteArrayInputStream(new byte[] { '"', (byte)failures[i], '"' }));
			} catch (JsonParserException e) {
				testException(e, 1, 2, "UTF-8");
			}
		}

		// Test the continuation bytes outside of a continuation
		for (int i = 0x80; i <= 0xBF; i++) {
			try {
				JsonParser.object().from(new ByteArrayInputStream(new byte[] { '"', (byte)i, '"' }));
			} catch (JsonParserException e) {
				testException(e, 1, 2, "UTF-8");
			}
		}
	}

	/**
	 * See http://seriot.ch/parsing_json.html and https://github.com/mmastrac/nanojson/issues/3.
	 */
	@Test
	void illegalUTF8StringFromJSONSuite() {
		try {
			JsonParser.object().from(new ByteArrayInputStream(new byte[] {
					'"', (byte) 0xed, (byte) 0xa0, (byte) 0x80, '"' }));
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2, "UTF-8");
		}
	}

	private void testEncoding(Charset charset) throws JsonParserException {
		String unicodeKeyFromHell = new String(new int[] { 0x7f, 0x80, 0x7ff, 0x800, 0xffff, 0x10000, 0x10ffff }, 0, 7);
		ByteArrayInputStream in = new ByteArrayInputStream(
				("{\"" + unicodeKeyFromHell + "\":\"\u007f\u07ff\uffff\"}").getBytes(charset));
		JsonObject obj = JsonParser.object().from(in);
		assertEquals("\u007f\u07ff\uffff", obj.getString(unicodeKeyFromHell));
	}

	private void testEncodingBOM(Charset charset) throws JsonParserException {
		String unicodeKeyFromHell = new String(new int[] { 0x7f, 0x80, 0x7ff, 0x800, 0xffff, 0x10000, 0x10ffff }, 0, 7);
		ByteArrayInputStream in = new ByteArrayInputStream(
				("\ufeff{\"" + unicodeKeyFromHell + "\":\"\u007f\u07ff\uffff\"}").getBytes(charset));
		JsonObject obj = JsonParser.object().from(in);
		assertEquals("\u007f\u07ff\uffff", obj.getString(unicodeKeyFromHell));
	}

	@Test
	void failureTestsFromYui() throws IOException {
		InputStream input = getClass().getClassLoader().getResourceAsStream("yui_fail_cases.txt");

		String[] failCases = readAsUtf8(input).split("\n");
		for (String failCase : failCases) {
			try {
				JsonParser.object().from(failCase);
				fail("Should have failed, but didn't: " + failCase);
			} catch (JsonParserException e) {
				// expected
			}
		}
	}

	@Test
	void tortureTest() throws JsonParserException, IOException {
		InputStream input = getClass().getClassLoader().getResourceAsStream("sample.json");
		JsonObject o = JsonParser.object().from(readAsUtf8(input));
		assertNotNull(o.get("a"));
		assertNotNull(o.getObject("a").getArray("b\uecee\u8324\u007a\\\ue768.N"));
		String json = JsonWriter.string().object(o).done();
		JsonObject o2 = JsonParser.object().from(json);
		/* String json2 = */JsonWriter.string().object(o2).done();

		// This doesn't work - keys don't sort properly
		// assertEquals(json, json2);
	}

	@Test
	void tortureTestUrl() throws JsonParserException {
		JsonObject o = JsonParser.object().from(getClass().getClassLoader().getResource("sample.json"));
		assertNotNull(o.getObject("a").getArray("b\uecee\u8324\u007a\\\ue768.N"));
	}

	@Test
	void tortureTestStream() throws JsonParserException {
		JsonObject o = JsonParser.object().from(getClass().getClassLoader().getResourceAsStream("sample.json"));
		assertNotNull(o.getObject("a").getArray("b\uecee\u8324\u007a\\\ue768.N"));
	}

	@Test
	void issue38() throws JsonParserException, IOException {
		// https://github.com/mmastrac/nanojson/issues/38
		InputStream input = getClass().getClassLoader().getResourceAsStream("issue-38.json");
		JsonParser.any().from(readAsUtf8(input));
	}

	@Test
	void escapeSequencesAcrossBufferBoundary() throws JsonParserException {
		String s1 = "";
		String s2 = "";

		// Push the single string over one buffer size
		for (int i = 0; i < 7000; i++) {
			s1 = "\\u1234" + s1;
		}

		// Try a number of different alignments
		for (int i = 0; i < 10; i++) {
			s2 += " ";
			JsonParser.any().from("\"" + s2 + s1 + "\\u1234\"");
		}
	}

	@Test
	void failTruncatedEscapeAcrossBufferBoundary() {
		String s1 = "\\u123";
		String s2 = "";
		for (int i = 0; i < 126; i++) {
			s1 = "\\n" + s1;
		}
		for (int i = 0; i < JsonTokener.BUFFER_SIZE - 256 - 20; i++) {
			s1 = " " + s1;
		}

		// Try a number of different alignments
		for (int i = 0; i < 20; i++) {
			s2 += " ";
			try {
				JsonParser.object().from("\"" + s2 + s1);
				fail();
			} catch (JsonParserException e) {
				assertTrue(e.getMessage().contains("EOF"), e.getMessage());
			}
		}
	}

	/**
	 * Tests from json.org: http://www.json.org/JSON_checker/
	 * 
	 * Skips two tests that don't match reality (ie: Chrome).
	 */
	@Test
	void jsonOrgTest() throws IOException {
		InputStream input = getClass().getClassLoader().getResourceAsStream("json_org_test.zip");
		ZipInputStream zip = new ZipInputStream(input);
		ZipEntry ze;

		while ((ze = zip.getNextEntry()) != null) {
			if (ze.isDirectory())
				continue;

			// skip "A JSON payload should be an object or array, not a string."
			if (ze.getName().contains("fail1.json"))
				continue;

			// skip "Too deep"
			if (ze.getName().contains("fail18.json"))
				continue;

			boolean positive = ze.getName().startsWith("test/pass");
			int offset = 0;
			int size = (int)ze.getSize();
			byte[] buffer = new byte[size];
			while (size > 0) {
				int r = zip.read(buffer, offset, buffer.length - offset);
				if (r <= 0)
					break;
				size -= r;
				offset += r;
			}

			String testCase = new String(buffer, "ASCII");
			if (positive)
				try {
					Object out = JsonParser.any().from(testCase);
					JsonWriter.string(out);
				} catch (JsonParserException e) {
					e.printStackTrace();
					fail("Should not have failed " + ze.getName() + ": " + testCase);
				}
			else {
				try {
					JsonParser.object().from(testCase);
					fail("Should have failed " + ze.getName() + ": " + testCase);
				} catch (JsonParserException e) {
					// expected
				}
			}

		}
	}

	private String readAsUtf8(InputStream input) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] b = new byte[1024 * 1024];
		while (true) {
			int r = input.read(b);
			if (r <= 0)
				break;
			out.write(b, 0, r);
		}
		String s = new String(out.toByteArray(), UTF8);
		return s;
	}

	private void testException(JsonParserException e, int linePos, int charPos) {
		assertEquals("line " + linePos + " char " + charPos,
				"line " + e.getLinePosition() + " char " + e.getCharPosition(),
				e.getMessage() + " incorrect location");
	}

	private void testException(JsonParserException e, int linePos, int charPos, String inError) {
		assertEquals("line " + linePos + " char " + charPos,
				"line " + e.getLinePosition() + " char " + e.getCharPosition());
		assertTrue(e.getMessage().contains(inError), "Error did not contain '" + inError + "': " + e.getMessage());
	}
}
