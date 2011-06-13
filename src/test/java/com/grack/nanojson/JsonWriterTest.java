package com.grack.nanojson;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.grack.nanojson.JsonWriter.RootStringContext;
import com.grack.nanojson.JsonWriter.RootValueContext;

public class JsonWriterTest {
	@Test
	public void testJsonWriter() {
		RootValueContext<RootStringContext> r = JsonWriter.string();

		String json = r.array()
			.value(true)
			.value(true)
			.end()
		.end();
		
		assertEquals("[true,true]", json);
	}
	
	@Test
	public void testArrayAndObject() {
		RootValueContext<RootStringContext> r = JsonWriter.string();

		String json = r
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
