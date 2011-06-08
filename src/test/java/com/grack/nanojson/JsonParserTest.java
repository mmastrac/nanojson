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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

public class JsonParserTest {
	@Test
	public void testEmptyObject() throws JsonParserException {
		assertEquals(HashMap.class, JsonParser.parse("{}").getClass());
		assertEquals("{}", JsonParser.parse("{}").toString());
	}

	@Test
	public void testEmptyArray() throws JsonParserException {
		assertEquals(ArrayList.class, JsonParser.parse("[]").getClass());
		assertEquals("[]", JsonParser.parse("[]").toString());
	}

	@Test
	public void testBasicTypes() throws JsonParserException {
		assertEquals("true", JsonParser.parse("true").toString());
		assertEquals("false", JsonParser.parse("false").toString());
		assertNull(JsonParser.parse("null"));
		assertEquals("1.0", JsonParser.parse("1").toString());
		assertEquals("a", JsonParser.parse("\"a\"").toString());
	}

	@Test
	public void testArrayWithEverything() throws JsonParserException {
		assertEquals(
				"[1.0, -1000000.0, abc, [1.0, 2.0, 3.0], {abc=123.0}, true, false]",
				JsonParser
						.parse("[1, -1.0e6, \"abc\", [1,2,3], {\"abc\":123}, true, false]")
						.toString());
	}

	@Test
	public void testObjectWithEverything() throws JsonParserException {
		// TODO: Is this deterministic if we use string keys?
		assertEquals(
				"{jkl=null, abc=123.0, ghi=[true, false], def=456.0, mno=true}",
				JsonParser
						.parse("{\"abc\":123, \"def\":456, \"ghi\":[true, false], \"jkl\":null, \"mno\":true}")
						.toString());
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

		assertEquals("\n\r\t\b\f", JsonParser.parse("\"\\n\\r\\t\\b\\f\""));
	}
	
	@Test
	public void testFailBustedString1() {
		try {
			// Missing " at end
			JsonParser.parse("\"abc");
			fail();
		} catch (JsonParserException e) {
		}
	}

	@Test
	public void testFailBustedString2() {
		try {
			// \n in middle of string
			JsonParser.parse("\"abc\n\"");
			fail();
		} catch (JsonParserException e) {
		}
	}

	@Test
	public void testFailArrayTrailingComma1() {
		try {
			JsonParser.parse("[,]");
			fail();
		} catch (JsonParserException e) {
		}
	}

	@Test
	public void testFailArrayTrailingComma2() {
		try {
			JsonParser.parse("[1,]");
			fail();
		} catch (JsonParserException e) {
		}
	}

	@Test
	public void testFailObjectTrailingComma1() {
		try {
			JsonParser.parse("{,}");
			fail();
		} catch (JsonParserException e) {
		}
	}

	@Test
	public void testFailObjectTrailingComma2() {
		try {
			JsonParser.parse("{\"abc\":123,}");
			fail();
		} catch (JsonParserException e) {
		}
	}
	
	@Test
	public void tortureTest() throws JsonParserException, IOException {
		InputStream input = getClass().getResourceAsStream("sample.json");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] b = new byte[1024 * 1024];
		while (true) {
			int r = input.read(b);
			if (r <= 0)
				break;
			out.write(b, 0, r);
		}
		Charset utf8 = Charset.forName("UTF8");
		JsonParser.parse(new String(out.toByteArray(), utf8));
	}
}
