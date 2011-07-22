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
}
