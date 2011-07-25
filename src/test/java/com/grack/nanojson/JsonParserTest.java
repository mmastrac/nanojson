/**
 * Copyright 2010 The nanojson Authors
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

public class JsonParserTest {
	@Test
	public void testEmptyObject() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.parse("{}").getClass());
		assertEquals("{}", JsonParser.parse("{}").toString());
	}

	@Test
	public void testObjectOneElement() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.parse("{\"a\":1}").getClass());
		assertEquals("{a=1}", JsonParser.parse("{\"a\":1}").toString());
	}

	@Test
	public void testObjectTwoElements() throws JsonParserException {
		assertEquals(JsonObject.class, JsonParser.parse("{\"a\":1,\"b\":1}").getClass());
		assertEquals("{b=1, a=1}", JsonParser.parse("{\"a\":1,\"b\":1}").toString());
	}

	@Test
	public void testEmptyArray() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.parse("[]").getClass());
		assertEquals("[]", JsonParser.parse("[]").toString());
	}

	@Test
	public void testArrayOneElement() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.parse("[1]").getClass());
		assertEquals("[1]", JsonParser.parse("[1]").toString());
	}

	@Test
	public void testArrayTwoElements() throws JsonParserException {
		assertEquals(JsonArray.class, JsonParser.parse("[1,1]").getClass());
		assertEquals("[1, 1]", JsonParser.parse("[1,1]").toString());
	}

	@Test
	public void testBasicTypes() throws JsonParserException {
		assertEquals("true", JsonParser.parse("true").toString());
		assertEquals("false", JsonParser.parse("false").toString());
		assertNull(JsonParser.parse("null"));
		assertEquals("1", JsonParser.parse("1").toString());
		assertEquals("1.0", JsonParser.parse("1.0").toString());
		assertEquals("", JsonParser.parse("\"\"").toString());
		assertEquals("a", JsonParser.parse("\"a\"").toString());
	}

	@Test
	public void testArrayWithEverything() throws JsonParserException {
		JsonArray a = JsonParser.parseArray("[1, -1.0e6, \"abc\", [1,2,3], {\"abc\":123}, true, false]");
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
		JsonObject o = JsonParser
				.parseObject("{\"abc\":123, \"def\":456.0, \"ghi\":[true, false], \"jkl\":null, \"mno\":true}");

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
		assertEquals("\n", JsonParser.parse("\"\\n\""));
		assertEquals("\r", JsonParser.parse("\"\\r\""));
		assertEquals("\t", JsonParser.parse("\"\\t\""));
		assertEquals("\b", JsonParser.parse("\"\\b\""));
		assertEquals("\f", JsonParser.parse("\"\\f\""));
		assertEquals("/", JsonParser.parse("\"/\""));
		assertEquals("\\", JsonParser.parse("\"\\\\\""));
		assertEquals("\"", JsonParser.parse("\"\\\"\""));
		assertEquals("\0", JsonParser.parse("\"\\u0000\""));
		assertEquals("\u8000", JsonParser.parse("\"\\u8000\""));
		assertEquals("\uffff", JsonParser.parse("\"\\uffff\""));

		assertEquals("all together: \\/\n\r\t\b\f (fin)",
				JsonParser.parse("\"all together: \\\\\\/\\n\\r\\t\\b\\f (fin)\""));
	}

	@Test
	public void testNumbers() throws JsonParserException {
		String[] testCases = new String[] { "0", "1", "-0", "-1", "0.1", "1.1", "-0.1", "0.10", "-0.10", "0e1", "0e0",
				"-0e-1", "0.0e0", "-0.0e0" };
		for (String testCase : testCases) {
			Number n = (Number)JsonParser.parse(testCase);
			assertEquals(Double.parseDouble(testCase), n.doubleValue(), Double.MIN_NORMAL);
		}
	}

	@Test
	public void testBigint() throws JsonParserException {
		JsonParser.parse("{\"v\":123456789123456789123456789}");
	}

	@Test
	public void testFailNoJson1() {
		try {
			JsonParser.parse("");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 0);
		}
	}

	@Test
	public void testFailNoJson2() {
		try {
			JsonParser.parse(" ");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailNoJson3() {
		try {
			JsonParser.parse("  ");
			fail("Should have failed to parse");
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailNumberEdgeCases() {
		String[] edgeCases = { "01", "-01", "+01", ".1", "-.1", "+.1", "+1", "0.", "-0.", "+0.", "0.e", "-0.e", "+0.e",
				"0e", "-0e", "+0e" };
		for (String edgeCase : edgeCases) {
			try {
				JsonParser.parse(edgeCase);
				fail("Should have failed to parse: " + edgeCase);
			} catch (JsonParserException e) {
				testException(e, 1, 1);
			}
		}
	}

	@Test
	public void testFailBustedNumber1() {
		try {
			// There's no 'f' in double, but it treats it as a new token
			JsonParser.parse("123f");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 4);
		}
	}

	@Test
	public void testFailBustedNumber2() {
		try {
			// Badly formed number
			JsonParser.parse("-1-1");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString1() {
		try {
			// Missing " at end
			JsonParser.parse("\"abc");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString2() {
		try {
			// \n in middle of string
			JsonParser.parse("\"abc\n\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString3() {
		try {
			// Bad escape "\x" in middle of string
			JsonParser.parse("\"abc\\x\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString4() {
		try {
			// Bad escape "\\u123x" in middle of string
			JsonParser.parse("\"\\u123x\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString5() {
		try {
			// Incomplete unicode escape
			JsonParser.parse("\"\\uggg\"");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString6() {
		try {
			// String that terminates halfway through a unicode escape
			JsonParser.parse("\"\\uggg");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailBustedString7() {
		try {
			// String that terminates halfway through a regular escape
			JsonParser.parse("\"\\");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1);
		}
	}

	@Test
	public void testFailArrayTrailingComma1() {
		try {
			JsonParser.parse("[,]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailArrayTrailingComma2() {
		try {
			JsonParser.parse("[1,]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 4);
		}
	}

	@Test
	public void testFailObjectTrailingComma1() {
		try {
			JsonParser.parse("{,}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailObjectTrailingComma2() {
		try {
			JsonParser.parse("{\"abc\":123,}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 12);
		}
	}

	@Test
	public void testFailObjectBadKey1() {
		try {
			JsonParser.parse("{true:1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailObjectBadKey2() {
		try {
			JsonParser.parse("{2:1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailObjectBadColon1() {
		try {
			JsonParser.parse("{\"abc\":}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 8);
		}
	}

	@Test
	public void testFailObjectBadColon2() {
		try {
			JsonParser.parse("{\"abc\":1:}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 9);
		}
	}

	@Test
	public void testFailObjectBadColon3() {
		try {
			JsonParser.parse("{:\"abc\":1}");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2);
		}
	}

	@Test
	public void testFailBadKeywords1() {
		try {
			JsonParser.parse("truef");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'truef'");
		}
	}

	@Test
	public void testFailBadKeywords2() {
		try {
			JsonParser.parse("true1");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 5);
		}
	}

	@Test
	public void testFailBadKeywords3() {
		try {
			JsonParser.parse("tru");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'tru'");
		}
	}

	@Test
	public void testFailBadKeywords4() {
		try {
			JsonParser.parse("[truef,true]");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 2, "'truef'");
		}
	}

	@Test
	public void testFailBadKeywords5() {
		try {
			JsonParser.parse("grue");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'grue'");
		}
	}

	@Test
	public void testFailBadKeywords6() {
		try {
			JsonParser.parse("trueeeeeeeeeeeeeeeeeeee");
			fail();
		} catch (JsonParserException e) {
			testException(e, 1, 1, "'trueeeeeeeeeeee'");
		}
	}

	@Test
	public void testFailTrailingCommaMultiline() {
		String testString = "{\n\"abc\":123,\n\"def\":456,\n}";
		try {
			JsonParser.parse(testString);
			fail();
		} catch (JsonParserException e) {
			testException(e, 4, 1);
		}
	}

	@Test
	public void failureTestsFromYui() throws IOException {
		InputStream input = getClass().getClassLoader().getResourceAsStream("yui_fail_cases.txt");

		String[] failCases = readAsUtf8(input).split("\n");
		for (String failCase : failCases) {
			try {
				JsonParser.parse(failCase);
				fail("Should have failed, but didn't: " + failCase);
			} catch (JsonParserException e) {
			}
		}
	}

	@Test
	public void tortureTest() throws JsonParserException, IOException {
		InputStream input = getClass().getClassLoader().getResourceAsStream("sample.json");
		JsonObject o = JsonParser.parseObject(readAsUtf8(input));
		assertNotNull(o.get("a"));
		String json = JsonWriter.string().object(o).end();
		JsonObject o2 = JsonParser.parseObject(json);
		@SuppressWarnings("unused")
		String json2 = JsonWriter.string().object(o2).end();

		// This doesn't work - keys don't sort properly
		// assertEquals(json, json2);
	}

	/**
	 * Tests from json.org: http://www.json.org/JSON_checker/
	 * 
	 * Skips two tests that don't match reality (ie: Chrome).
	 */
	@Test
	public void jsonOrgTest() throws JsonParserException, IOException {
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
					JsonParser.parse(testCase);
				} catch (JsonParserException e) {
					e.printStackTrace();
					fail("Should not have failed " + ze.getName() + ": " + testCase);
				}
			else {
				try {
					JsonParser.parse(testCase);
					fail("Should have failed " + ze.getName() + ": " + testCase);
				} catch (JsonParserException e) {
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
