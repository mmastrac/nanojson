package com.grack.nanojson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonBuilderTest {
	@Test(expected = JsonWriterException.class)
	public void testFailureKeyInArray() {
		new JsonBuilder<>(new JsonArray()).key("a");
	}

	@Test(expected = JsonWriterException.class)
	public void testFailureKeyWhileKeyPending() {
		new JsonBuilder<>(new JsonObject()).key("a").key("b");
	}

	@Test
	public void testSeparateKeyWriting() {
		JsonObject actual = new JsonBuilder<>(new JsonObject()).key("a").value(1).key("b").value(2).done();
		JsonObject expected = new JsonObject();
		expected.put("a", 1);
		expected.put("b", 2);
		assertEquals(expected, actual);
	}
}
