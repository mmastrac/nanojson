package com.grack.nanojson;

import static org.junit.Assert.*;

import org.junit.Test;

public class JsonEmitterTest {
	/**
	 * Test emitting simple values.
	 */
	@Test
	public void testSimpleValues() {
		StringBuilder builder = new StringBuilder();
		JsonEmitter emitter;

		emitter = new JsonEmitter(builder);
		emitter.value(true).end();
		assertEquals("true", builder.toString());
		builder.setLength(0);

		emitter = new JsonEmitter(builder);
		emitter.value(null).end();
		assertEquals("null", builder.toString());
		builder.setLength(0);

		emitter = new JsonEmitter(builder);
		emitter.value(1.0).end();
		assertEquals("1.0", builder.toString());
		builder.setLength(0);

		emitter = new JsonEmitter(builder);
		emitter.value(1).end();
		assertEquals("1", builder.toString());
		builder.setLength(0);

		emitter = new JsonEmitter(builder);
		emitter.value("abc").end();
		assertEquals("\"abc\"", builder.toString());
		builder.setLength(0);
	}

	/**
	 * Test a simple array.
	 */
	@Test
	public void testArray() {
		StringBuilder builder = new StringBuilder();
		JsonEmitter emitter;

		emitter = new JsonEmitter(builder);
		emitter.startArray().value(true).value(false).value(true).endArray().end();
		assertEquals("[true,false,true]", builder.toString());
	}

	/**
	 * Test a nested array.
	 */
	@Test
	public void testNestedArray() {
		StringBuilder builder = new StringBuilder();
		JsonEmitter emitter;

		emitter = new JsonEmitter(builder);
		emitter.startArray().startArray().startArray().value(true).value(false)
				.value(true).endArray().endArray().endArray().end();
		assertEquals("[[[true,false,true]]]", builder.toString());
	}

	/**
	 * Test a nested array.
	 */
	@Test
	public void testNestedArray2() {
		StringBuilder builder = new StringBuilder();
		JsonEmitter emitter;

		emitter = new JsonEmitter(builder);
		emitter.startArray().value(true).startArray().startArray().value(false)
				.endArray().endArray().value(true).endArray().end();
		assertEquals("[true,[[false]],true]", builder.toString());
	}

	/**
	 * Test a simple object.
	 */
	@Test
	public void testObject() {
		StringBuilder builder = new StringBuilder();
		new JsonEmitter(builder).startObject().value("a", true)
				.value("b", false).value("c", true).endObject().end();
		assertEquals("{\"a\":true,\"b\":false,\"c\":true}", builder.toString());
	}

	/**
	 * Test a nested object.
	 */
	@Test
	public void testNestedObject() {
		StringBuilder builder = new StringBuilder();
		JsonEmitter emitter;

		emitter = new JsonEmitter(builder);
		emitter.startObject().startObject("a").value("b", false)
				.value("c", true).endObject().endObject().end();
		assertEquals("{\"a\":{\"b\":false,\"c\":true}}", builder.toString());
	}
}
