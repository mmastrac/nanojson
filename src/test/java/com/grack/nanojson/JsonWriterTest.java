package com.grack.nanojson;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonWriterTest {
	@Test
	public void testJsonWriter() {
		String json = JsonWriter.string().array()
			.value(true)
			.value(true)
			.end()
		.end();
		
		assertEquals("[true,true]", json);
	}
	
	@Test
	public void testJsonWriterAppendable() {
		StringBuilder sb = new StringBuilder();
		String json = JsonWriter.write(sb).array()
			.value(true)
			.value(true)
			.end()
		.end().toString();
		
		assertEquals("[true,true]", json);
	}
	
	@Test
	public void testArrayAndObject() {
		String json = JsonWriter.string()
			.array()
				.value(true)
				.object()
					.array("a")
						.value(true)
						.value(false)
					.end()
					.value("b", "string")
				.end()
			.end()
		.end();
		
		assertEquals("[true,{\"a\":[true,false],\"b\":\"string\"}]", json);
	}
	
	@Test
	public void testJsonArray() {
		JsonArray a = new JsonArray();
		a.add("abc");
		a.add("def");
		String json = JsonWriter.string()
			.array()
				.array(a)
				.object()
					.value("b", "string")
				.end()
			.end()
		.end();
		
		assertEquals("[[\"abc\",\"def\"],{\"b\":\"string\"}]", json);
	}
	
	@Test
	public void testJsonObject() {
		JsonObject o = new JsonObject();
		o.put("abc", "def");
		String json = JsonWriter.string()
			.array()
				.object(o)
				.object()
					.value("b", "string")
				.end()
			.end()
		.end();
		
		assertEquals("[{\"abc\":\"def\"},{\"b\":\"string\"}]", json);
	}
	
	@Test
	public void testObjectStructure() {
		JsonArray a = new JsonArray();
		a.add("abc");
		a.add("def");

		JsonObject o = new JsonObject();
		o.put("abc", a);
		
		String json = JsonWriter.string().object(o).end();
			
		assertEquals("{\"abc\":[\"abc\",\"def\"]}", json);
	}
}
