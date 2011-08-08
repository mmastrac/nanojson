package com.grack.nanojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class JsonTypesTest {
	@Test
	public void testObjectInt() {
		JsonObject o = new JsonObject();
		o.put("key", 1);
		assertEquals(1, o.getInt("key"));
		assertEquals(1.0, o.getDouble("key"), 0.0001f);
		assertEquals(1, o.getNumber("key"));
		assertEquals(1, o.get("key"));

		assertEquals(null, o.getString("key"));
		assertEquals("foo", o.getString("key", "foo"));
		assertFalse(o.isNull("key"));
	}

	@Test
	public void testObjectString() {
		JsonObject o = new JsonObject();
		o.put("key", "1");
		assertEquals(0, o.getInt("key"));
		assertEquals(0, o.getDouble("key"), 0.0001f);
		assertEquals(null, o.getNumber("key"));
		assertEquals("1", o.get("key"));
		assertFalse(o.isNull("key"));
	}

	@Test
	public void testObjectNull() {
		JsonObject o = new JsonObject();
		o.put("key", null);
		assertEquals(0, o.getInt("key"));
		assertEquals(0, o.getDouble("key"), 0.0001f);
		assertEquals(null, o.getNumber("key"));
		assertEquals(null, o.get("key"));
		assertTrue(o.isNull("key"));
	}

	@Test
	public void testArrayInt() {
		JsonArray o = new JsonArray(Arrays.asList(null, null, null, null));
		o.set(3, 1);
		assertEquals(1, o.getInt(3));
		assertEquals(1.0, o.getDouble(3), 0.0001f);
		assertEquals(1, o.getNumber(3));
		assertEquals(1, o.get(3));

		assertEquals(null, o.getString(3));
		assertEquals("foo", o.getString(3, "foo"));
		assertFalse(o.isNull(3));
	}

	@Test
	public void testArrayString() {
		JsonArray o = new JsonArray(Arrays.asList(null, null, null, null));
		o.set(3, "1");
		assertEquals(0, o.getInt(3));
		assertEquals(0, o.getDouble(3), 0.0001f);
		assertEquals(null, o.getNumber(3));
		assertEquals("1", o.get(3));
		assertFalse(o.isNull(3));
	}

	@Test
	public void testArrayNull() {
		JsonArray o = new JsonArray(Arrays.asList(null, null, null, null));
		o.set(3, null);
		assertEquals(0, o.getInt(3));
		assertEquals(0, o.getDouble(3), 0.0001f);
		assertEquals(null, o.getNumber(3));
		assertEquals(null, o.get(3));
		assertTrue(o.isNull(3));
		assertTrue(o.has(3));
	}
	
	@Test
	public void testArrayBounds() {
		JsonArray o = new JsonArray(Arrays.asList(null, null, null, null));
		assertEquals(0, o.getInt(4));
		assertEquals(0, o.getDouble(4), 0.0001f);
		assertEquals(null, o.getNumber(4));
		assertEquals(null, o.get(4));
		assertFalse(o.isNull(4));
		assertFalse(o.has(4));
	}
}
