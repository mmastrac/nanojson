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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

public class JsonParserTest {
	@Test
	public void testWhitespace() throws JsonParserException {
		assertEquals(JsonObject.class,
				JsonParser.object().from(" \t\r\n  { \t\r\n \"abc\"   \t\r\n : \t\r\n  1 \t\r\n  }  \t\r\n   ")
						.getClass());
		assertEquals("{}", JsonParser.object().from("{}").toString());
	}

	@Test
	public void testEmptyObject() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.object().from("{}").getClass());
		assertEquals("{}", JsonParser.object().from("{}").toString());
	}

	@Test
	public void testObjectOneElement() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.object().from("{\"a\":1}").getClass());
		assertEquals("{a=1}", JsonParser.object().from("{\"a\":1}").toString());
	}

	@Test
	public void testObjectTwoElements() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.object().from("{\"a\":1,\"B\":1}").getClass());
		assertEquals("{B=1, a=1}", JsonParser.object().from("{\"a\":1,\"B\":1}").toString());
	}

	@Test
	public void testEmptyArray() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.array().from("[]").getClass());
		assertEquals("[]", JsonParser.array().from("[]").toString());
	}

	@Test
	public void testArrayOneElement() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.array().from("[1]").getClass());
		assertEquals("[1]", JsonParser.array().from("[1]").toString());
	}

	@Test
	public void testArrayTwoElements() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.array().from("[1,1]").getClass());
		assertEquals("[1, 1]", JsonParser.array().from("[1,1]").toString());
	}

	@Test
	public void testBasicTypes() throws JsonParserException {
		assertEquals("true", JsonParser.any().from("true").toString());
		assertEquals("false", JsonParser.any().from("false").toString());
		assertNull(JsonParser.any().from("null"));
		assertEquals("1", JsonParser.any().from("1").toString());
		assertEquals("1.0", JsonParser.any().from("1.0").toString());
		assertEquals("", JsonParser.any().from("\"\"").toString());
		assertEquals("a", JsonParser.any().from("\"a\"").toString());
	}

	@Test
	public void testArrayWithEverything() throws JsonParserException {
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
	public void testObjectWithEverything() throws JsonParserException {
		// TODO: Is this deterministic if we use string keys?
		JsonObject o = JsonParser.object().from(
				"{\"abc\":123, \"def\":456.0, \"ghi\":[true, false], \"jkl\":null, \"mno\":true}");

		assertEquals("{jkl=null, abc=123, ghi=[true, false], def=456.0, mno=true}", o.toString());

		assertEquals(123, o.getInt("abc"));
		assertEquals(456, o.getInt("def"));
		assertEquals(true, o.getArray("ghi").getBoolean(0));
		assertEquals(null, o.get("jkl"));
		assertTrue(o.isNull("jkl"));
		assertTrue(o.getBoolean("mno"));
	}

	@Test
	public void testStringEscapes() throws JsonParserException {
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
	public void testStringEscapesAroundBufferBoundary() throws JsonParserException {
		String base = "";
		for (int i = 0; i < JsonParser.BUFFER_SIZE + 1024; i++) {
			base += " ";
			assertEquals("\u0055", JsonParser.any().from(base + "\"\\u0055\""));
		}
	}

	@Test
	public void testNumbers() throws JsonParserException {
		String[] testCases = new String[] { "0", "1", "-0", "-1", "0.1", "1.1", "-0.1", "0.10", "-0.10", "0e1", "0e0",
				"-0e-1", "0.0e0", "-0.0e0" };
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
	public void testNegativeZero() throws JsonParserException {
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
	public void testBasicNumbers() throws JsonParserException {
		for (int i = -100; i <= +100; i++) {
			assertEquals(i, (int)(Integer)JsonParser.any().from("" + i));
		}
	}

	@Test
	public void testBigint() throws JsonParserException {
		JsonObject o = JsonParser.object().from("{\"v\":123456789123456789123456789}");
		BigInteger bigint = (BigInteger)o.get("v");
		assertEquals("123456789123456789123456789", bigint.toString());
	}

	@Test
	public void testFailWrongType() {
		try {
			JsonParser.object().from("1");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 1, "did not contain the correct type");
		}
	}

	@Test
	public void testFailNull() {
		try {
			JsonParser.object().from("null");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 4, "did not contain the correct type");
		}
	}

	@Test
	public void testFailNoJson1() {
		try {
			JsonParser.object().from("");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 0);
		}
	}

	@Test
	public void testFailNoJson2() {
		try {
			JsonParser.object().from(" ");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailNoJson3() {
		try {
			JsonParser.object().from("  ");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailNumberEdgeCases() {
		String[] edgeCases = { "-", ".", "e", "01", "-01", "+01", "01.1", "-01.1", "+01.1", ".1", "-.1", "+.1", "+1",
				"0.", "-0.", "+0.", "0.e", "-0.e", "+0.e", "0e", "-0e", "+0e", "0e-", "-0e-", "+0e-", "0e+", "-0e+",
				"+0e+" };
		for (String edgeCase : edgeCases) {
			try {
				JsonParser.object().from(edgeCase);
				fail("Should have failed to parse: " + edgeCase);
			} catch (JsonParserException e) {
				testException(e, 1, 1);
			}

			try {
				JsonParser.object().from(edgeCase.toUpperCase());
				fail("Should have failed to parse: " + edgeCase.toUpperCase());
			} catch (JsonParserException e) {
				testException(e, 1, 1);
			}
		}
	}

	@Test
	public void testFailBustedNumber1() {
		try {
			// There's no 'f' in double, but it treats it as a new token
			JsonParser.object().from("123f");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 4);
		}
	}

	@Test
	public void testFailBustedNumber2() {
		try {
			// Badly formed number
			JsonParser.object().from("-1-1");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString1() {
		try {
			// Missing " at end
			JsonParser.object().from("\"abc");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString2() {
		try {
			// \n in middle of string
			JsonParser.object().from("\"abc\n\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 2, 1);
		}
	}

	@Test
	public void testFailBustedString3() {
		try {
			// Bad escape "\x" in middle of string
			JsonParser.object().from("\"abc\\x\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 6);
		}
	}

	@Test
	public void testFailBustedString4() {
		try {
			// Bad escape "\\u123x" in middle of string
			JsonParser.object().from("\"\\u123x\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 7);
		}
	}

	@Test
	public void testFailBustedString5() {
		try {
			// Incomplete unicode escape
			JsonParser.object().from("\"\\u222\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 7);
		}
	}

	@Test
	public void testFailBustedString6() {
		try {
			// String that terminates halfway through a unicode escape
			JsonParser.object().from("\"\\u222");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 6);
		}
	}

	@Test
	public void testFailBustedString7() {
		try {
			// String that terminates halfway through a regular escape
			JsonParser.object().from("\"\\");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailArrayTrailingComma1() {
		try {
			JsonParser.object().from("[,]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailArrayTrailingComma2() {
		try {
			JsonParser.object().from("[1,]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 4);
		}
	}

	@Test
	public void testFailObjectTrailingComma1() {
		try {
			JsonParser.object().from("{,}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailObjectTrailingComma2() {
		try {
			JsonParser.object().from("{\"abc\":123,}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 12);
		}
	}

	@Test
	public void testFailObjectBadKey1() {
		try {
			JsonParser.object().from("{true:1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailObjectBadKey2() {
		try {
			JsonParser.object().from("{2:1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailObjectBadColon1() {
		try {
			JsonParser.object().from("{\"abc\":}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 8);
		}
	}

	@Test
	public void testFailObjectBadColon2() {
		try {
			JsonParser.object().from("{\"abc\":1:}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 9);
		}
	}

	@Test
	public void testFailObjectBadColon3() {
		try {
			JsonParser.object().from("{:\"abc\":1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailBadKeywords1() {
		try {
			JsonParser.object().from("truef");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'truef'");
		}
	}

	@Test
	public void testFailBadKeywords2() {
		try {
			JsonParser.object().from("true1");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 5);
		}
	}

	@Test
	public void testFailBadKeywords3() {
		try {
			JsonParser.object().from("tru");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'tru'");
		}
	}

	@Test
	public void testFailBadKeywords4() {
		try {
			JsonParser.object().from("[truef,true]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2, "'truef'");
		}
	}

	@Test
	public void testFailBadKeywords5() {
		try {
			JsonParser.object().from("grue");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'grue'");
		}
	}

	@Test
	public void testFailBadKeywords6() {
		try {
			JsonParser.object().from("trueeeeeeeeeeeeeeeeeeee");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'trueeeeeeeeeeee'");
		}
	}

	@Test
	public void testFailBadKeywords7() {
		try {
			JsonParser.object().from("g");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'g'");
		}
	}

	@Test
	public void testFailTrailingCommaMultiline() {
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
	public void testFailTrailingCommaUTF8() {
		ByteArrayInputStream in1 = new ByteArrayInputStream("{\n\"abc\":123,\"def\":456,}".getBytes(Charset
				.forName("UTF-8")));
		ByteArrayInputStream in2 = new ByteArrayInputStream(
				"{\n\"\ub123\ub124\ub125\":123,\"def\":456,}".getBytes(Charset.forName("UTF-8")));
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
	public void testEncodingUTF8() throws JsonParserException {
		Charset charset = Charset.forName("UTF8");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	public void testEncodingUTF16LE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-16LE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	public void testEncodingUTF16BE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-16BE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	public void testEncodingUTF32LE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-32LE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	public void testEncodingUTF32BE() throws JsonParserException {
		Charset charset = Charset.forName("UTF-32BE");
		testEncoding(charset);
		testEncodingBOM(charset);
	}

	@Test
	public void testIllegalUTF8Bytes() {
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
	public void failureTestsFromYui() throws IOException {
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
	public void tortureTest() throws JsonParserException, IOException {
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
	public void tortureTestUrl() throws JsonParserException {
		JsonObject o = JsonParser.object().from(getClass().getClassLoader().getResource("sample.json"));
		assertNotNull(o.getObject("a").getArray("b\uecee\u8324\u007a\\\ue768.N"));
	}

	@Test
	public void tortureTestStream() throws JsonParserException {
		JsonObject o = JsonParser.object().from(getClass().getClassLoader().getResourceAsStream("sample.json"));
		assertNotNull(o.getObject("a").getArray("b\uecee\u8324\u007a\\\ue768.N"));
	}

	/**
	 * Tests from json.org: http://www.json.org/JSON_checker/
	 * 
	 * Skips two tests that don't match reality (ie: Chrome).
	 */
	@Test
	public void jsonOrgTest() throws IOException {
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
		Charset utf8 = Charset.forName("UTF8");
		String s = new String(out.toByteArray(), utf8);
		return s;
	}

	private void testException(JsonParserException e, int linePos, int charPos) {
		assertEquals("line " + linePos + " char " + charPos,
				"line " + e.getLinePosition() + " char " + e.getCharPosition());
	}

	private void testException(JsonParserException e, int linePos, int charPos, String inError) {
		assertEquals("line " + linePos + " char " + charPos,
				"line " + e.getLinePosition() + " char " + e.getCharPosition());
		assertTrue("Error did not contain '" + inError + "': " + e.getMessage(), e.getMessage().contains(inError));
	}
}
