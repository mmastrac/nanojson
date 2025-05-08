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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Test for the various JSON types.
 */
public class JsonTypesTest {
	// CHECKSTYLE_OFF: MagicNumber
	// CHECKSTYLE_OFF: JavadocMethod
	@Test
	public void testObjectInt() {
		JsonObject o = new JsonObject();
		o.put("key", 1);
		assertEquals(1, o.getInt("key"));
		assertEquals(1L, o.getLong("key"));
		assertEquals(1.0, o.getDouble("key"), 0.0001f);
		assertEquals(1.0f, o.getFloat("key"), 0.0001f);
		assertEquals(1, o.getNumber("key"));
		assertEquals(1, o.get("key"));

		assertEquals(null, o.getString("key"));
		assertEquals("foo", o.getString("key", "foo"));
		assertFalse(o.isNull("key"));
	}

	@Test
	public void testObjectIteratorTracking() {
		JsonObject o = new JsonObject();
		o.put("test", 1);
		o.put("test3", 2);
		o.put("test2", "test");
		Iterable<Map.Entry<String, Integer>> iterable = o.asIterable(Integer.class);
		Iterator<Map.Entry<String, Integer>> iterator = iterable.iterator();
		assertTrue(iterator.hasNext());
		Map.Entry<String, Integer> entry1 = iterator.next();
		assertNotNull(entry1);
		assertEquals(1, entry1.getValue().intValue());
		assertEquals("test", entry1.getKey());
		assertTrue(iterator.hasNext());
		Map.Entry<String, Integer> entry2 = iterator.next();
		assertNotNull(entry2);
		assertEquals(2, entry2.getValue().intValue());
		assertEquals("test3", entry2.getKey());
		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
		o.put("test4", "test");
		o.put("test2", 4);
		iterator = iterable.iterator();
		iterator.next();
		iterator.next();
		iterator.next();
		assertFalse(iterator.hasNext());
 	}
	@Test
	public void testObjectIteratorCoModification() {
		JsonObject o = new JsonObject();
		o.put("test3", 4.3);
		Iterator<Map.Entry<String, Number>> iterator = o.asIterable(Number.class).iterator();
		o.put("test5", 1.3);
		assertThrows(ConcurrentModificationException.class, iterator::next);
	}

	@Test
	public void testArrayIteratorCoModification() {
		JsonArray array = new JsonArray(Collections.singletonList(1));
		Iterator<Integer> it = array.asIterable(Integer.class).iterator();
		array.add("ddd");
		assertThrows(ConcurrentModificationException.class, it::next);

	}

	@Test
	public void testObjectString() {
		JsonObject o = new JsonObject();
		o.put("key", "1");
		assertEquals(0, o.getInt("key"));
		assertEquals(0L, o.getLong("key"));
		assertEquals(0, o.getDouble("key"), 0.0001f);
		assertEquals(0f, o.getFloat("key"), 0.0001f);
		assertEquals(null, o.getNumber("key"));
		assertEquals("1", o.get("key"));
		assertFalse(o.isNull("key"));
	}

	@Test
	public void testObjectNull() {
		JsonObject o = new JsonObject();
		o.put("key", null);
		assertEquals(0, o.getInt("key"));
		assertEquals(0L, o.getLong("key"));
		assertEquals(0, o.getDouble("key"), 0.0001f);
		assertEquals(0f, o.getFloat("key"), 0.0001f);
		assertEquals(null, o.getNumber("key"));
		assertEquals(null, o.get("key"));
		assertTrue(o.isNull("key"));
	}

	@Test
	public void testArrayInt() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		o.set(3, 1);
		assertEquals(1, o.getInt(3));
		assertEquals(1L, o.getLong(3));
		assertEquals(1.0, o.getDouble(3), 0.0001f);
		assertEquals(1.0f, o.getFloat(3), 0.0001f);
		assertEquals(1, o.getNumber(3));
		assertEquals(1, o.get(3));

		assertEquals(null, o.getString(3));
		assertEquals("foo", o.getString(3, "foo"));
		assertFalse(o.isNull(3));
	}

	@Test
	public void testArrayString() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		o.set(3, "1");
		assertEquals(0, o.getInt(3));
		assertEquals(0L, o.getLong(3));
		assertEquals(0, o.getDouble(3), 0.0001f);
		assertEquals(0, o.getFloat(3), 0.0001f);
		assertEquals(null, o.getNumber(3));
		assertEquals("1", o.get(3));
		assertFalse(o.isNull(3));
	}

	@Test
	public void testArrayNull() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		o.set(3, null);
		assertEquals(0, o.getInt(3));
		assertEquals(0, o.getDouble(3), 0.0001f);
		assertEquals(0, o.getFloat(3), 0.0001f);
		assertEquals(null, o.getNumber(3));
		assertEquals(null, o.get(3));
		assertTrue(o.isNull(3));
		assertTrue(o.has(3));
	}

	@Test
	public void testArrayBounds() {
		JsonArray o = new JsonArray(Arrays.asList((String) null, null, null,
				null));
		assertEquals(0, o.getInt(4));
		assertEquals(0, o.getDouble(4), 0.0001f);
		assertEquals(0, o.getFloat(4), 0.0001f);
		assertEquals(null, o.getNumber(4));
		assertEquals(null, o.get(4));
		assertFalse(o.isNull(4));
		assertFalse(o.has(4));
	}

	@Test
	public void testJsonArrayBuilder() {
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
	public void testJsonObjectBuilder() {
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

	@Test(expected = JsonWriterException.class)
	public void testJsonArrayBuilderFailCantCloseRoot() {
		JsonArray.builder().end();
	}

	@Test(expected = JsonWriterException.class)
	public void testJsonArrayBuilderFailCantAddKeyToArray() {
		JsonArray.builder().value("abc", 1);
	}

	@Test(expected = JsonWriterException.class)
	public void testJsonArrayBuilderFailCantAddNonKeyToObject() {
		JsonObject.builder().value(1);
	}

	@Test
	public void testJsonKeyOrder() {
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
