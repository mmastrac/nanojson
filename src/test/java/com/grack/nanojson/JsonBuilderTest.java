package com.grack.nanojson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JsonBuilderTest {
	@Test
	void failureKeyInArray() {
		assertThrows(JsonWriterException.class, () ->
			new JsonBuilder<>(new JsonArray()).key("a"));
	}

	@Test
	void failureKeyWhileKeyPending() {
		assertThrows(JsonWriterException.class, () ->
			new JsonBuilder<>(new JsonObject()).key("a").key("b"));
	}

	@Test
	void separateKeyWriting() {
		JsonObject actual = new JsonBuilder<>(new JsonObject()).key("a").value(1).key("b").value(2).done();
		JsonObject expected = new JsonObject();
		expected.put("a", 1);
		expected.put("b", 2);
		assertEquals(expected, actual);
	}
}
