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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Test for the various JSON types.
 */
class JsonTypesTest {
	// CHECKSTYLE_OFF: MagicNumber
	// CHECKSTYLE_OFF: JavadocMethod
	@Test
	void objectInt() {
		JsonObject o = new JsonObject();
		o.put("key", 1);
		assertEquals(1, o.getInt("key"));
		assertEquals(1L, o.getLong("key"));
		assertEquals(1.0, o.getDouble("key"), 0.0001f);
		assertEquals(1.0f, o.getFloat("key"), 0.0001f);
		assertEquals(1, o.getNumber("key"));
		assertEquals(1, o.get("key"));

		assertNull(o.getString("key"));
		assertEquals("foo", o.getString("key", "foo"));
		assertFalse(o.isNull("key"));
	}

	@Test
	void objectString() {
		JsonObject o = new JsonObject();
		o.put("key", "1");
		assertEquals(0, o.getInt("key"));
		assertEquals(0L, o.getLong("key"));
		assertEquals(0, o.getDouble("key"), 0.0001f);
		assertEquals(0f, o.getFloat("key"), 0.0001f);
		assertNull(o.getNumber("key"));
		assertEquals("1", o.get("key"));
		assertFalse(o.isNull("key"));
	}

	@Test
	void objectNull() {
		JsonObject o = new JsonObject();
		o.put("key", null);
		assertEquals(0, o.getInt("key"));
		assertEquals(0L, o.getLong("key"));
		assertEquals(0, o.getDouble("key"), 0.0001f);
		assertEquals(0f, o.getFloat("key"), 0.0001f);
		assertNull(o.getNumber("key"));
		assertNull(o.get("key"));
		assertTrue(o.isNull("key"));
	}

	@Test
	void arrayInt() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		o.set(3, 1);
		assertEquals(1, o.getInt(3));
		assertEquals(1L, o.getLong(3));
		assertEquals(1.0, o.getDouble(3), 0.0001f);
		assertEquals(1.0f, o.getFloat(3), 0.0001f);
		assertEquals(1, o.getNumber(3));
		assertEquals(1, o.get(3));

		assertNull(o.getString(3));
		assertEquals("foo", o.getString(3, "foo"));
		assertFalse(o.isNull(3));
	}

	@Test
	void arrayString() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		o.set(3, "1");
		assertEquals(0, o.getInt(3));
		assertEquals(0L, o.getLong(3));
		assertEquals(0, o.getDouble(3), 0.0001f);
		assertEquals(0, o.getFloat(3), 0.0001f);
		assertNull(o.getNumber(3));
		assertEquals("1", o.get(3));
		assertFalse(o.isNull(3));
	}

	@Test
	void arrayNull() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		o.set(3, null);
		assertEquals(0, o.getInt(3));
		assertEquals(0, o.getDouble(3), 0.0001f);
		assertEquals(0, o.getFloat(3), 0.0001f);
		assertNull(o.getNumber(3));
		assertNull(o.get(3));
		assertTrue(o.isNull(3));
		assertTrue(o.has(3));
	}

	@Test
	void arrayBounds() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		assertEquals(0, o.getInt(4));
		assertEquals(0, o.getDouble(4), 0.0001f);
		assertEquals(0, o.getFloat(4), 0.0001f);
		assertNull(o.getNumber(4));
		assertNull(o.get(4));
		assertFalse(o.isNull(4));
		assertFalse(o.has(4));
	}

	@Test
	void jsonArrayBuilder() {
		// @formatter:off
		JsonArray a = JsonArray.builder().value(true).value(1.0).value(1.0f)
				.value(1).value(new BigInteger("1234567890")).value("hi")
				.object().value("abc", 123).end().array().value(1).nul().end()
				.array(JsonArray.from(1, 2, 3))
				.object(JsonObject.builder().nul("a").done()).done();
		// @formatter:on

		assertEquals(
				"[true,1.0,1.0,1,1234567890,\"hi\",{\"abc\":123},[1,null],[1,2,3],{\"a\":null}]",
				JsonWriter.string(a));
	}

	@Test
	void jsonObjectBuilder() {
		// @formatter:off
		JsonObject a = JsonObject
				.builder()
				.value("bool", true)
				.value("double", 1.0)
				.value("float", 1.0f)
				.value("int", 1)
				.value("bigint", new BigInteger("1234567890"))
				.value("string", "hi")
				.nul("null")
				.object("object")
				.value("abc", 123)
				.end()
				.array("array")
				.value(1)
				.nul()
				.end()
				.array("existingArray", JsonArray.from(1, 2, 3))
				.object("existingObject",
						JsonObject.builder().nul("a").done())
				.done();
		// @formatter:on

		String[] bits = new String[] { "\"bigint\":1234567890", "\"int\":1",
				"\"string\":\"hi\"",
				"\"existingObject\":{\"a\":null}",
				"\"existingArray\":[1,2,3]", "\"object\":{\"abc\":123}",
				"\"bool\":true", "\"double\":1.0", "\"float\":1.0",
				"\"null\":null", "\"array\":[1,null]" };

		String s = JsonWriter.string(a);
		
		for (String bit : bits) {
			assertTrue(s.contains(bit));
		}
	}

	@Test
	void jsonArrayBuilderFailCantCloseRoot() {
		assertThrows(JsonWriterException.class, () ->
			JsonArray.builder().end());
	}

	@Test
	void jsonArrayBuilderFailCantAddKeyToArray() {
		assertThrows(JsonWriterException.class, () ->
			JsonArray.builder().value("abc", 1));
	}

	@Test
	void jsonArrayBuilderFailCantAddNonKeyToObject() {
		assertThrows(JsonWriterException.class, () ->
			JsonObject.builder().value(1));
	}

	@Test
	void jsonKeyOrder() {
		JsonObject a = JsonObject
			.builder()
			.value("key01", 1)
			.value("key02", 2)
			.value("key03", 3)
			.value("key04", 4)
			.done();

		assertArrayEquals(
			new String [] {
				"key01",
				"key02",
				"key03",
				"key04"
			},
			a.keySet().toArray(new String[0]));
	}
}
