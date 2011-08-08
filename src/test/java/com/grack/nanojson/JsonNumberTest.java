package com.grack.nanojson;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

/**
 * Attempts to test that numbers are correctly round-tripped.
 */
public class JsonNumberTest {
	@Test
	public void testBasicNumberRead() throws JsonParserException {
		JsonArray array = JsonParser.array().from("[1, 1.0, 1.00]");
		assertEquals(Integer.class, array.get(0).getClass());
		assertEquals(Double.class, array.get(1).getClass());
		assertEquals(Double.class, array.get(2).getClass());
	}

	@Test
	public void testBasicNumberWrite() {
		JsonArray array = JsonArray.from(1, 1.0, 1.0f);
		assertEquals("[1,1.0,1.0]", JsonWriter.string().array(array).close());
	}

	@Test
	public void testLargeIntRead() throws JsonParserException {
		JsonArray array = JsonParser.array().from("[-300000000,300000000]");
		assertEquals(Integer.class, array.get(0).getClass());
		assertEquals(-300000000, array.get(0));
		assertEquals(Integer.class, array.get(1).getClass());
		assertEquals(300000000, array.get(1));
	}

	@Test
	public void testLargeIntWrite() {
		JsonArray array = JsonArray.from(-300000000, 300000000);
		assertEquals("[-300000000,300000000]", JsonWriter.string().array(array).close());
	}

	@Test
	public void testLongRead() throws JsonParserException {
		JsonArray array = JsonParser.array().from("[-3000000000,3000000000]");
		assertEquals(Long.class, array.get(0).getClass());
		assertEquals(-3000000000L, array.get(0));
		assertEquals(Long.class, array.get(1).getClass());
		assertEquals(3000000000L, array.get(1));
	}

	@Test
	public void testLongWrite() {
		JsonArray array = JsonArray.from(1L, -3000000000L, 3000000000L);
		assertEquals("[1,-3000000000,3000000000]", JsonWriter.string().array(array).close());
	}

	@Test
	public void testBigIntRead() throws JsonParserException {
		JsonArray array = JsonParser.array().from("[-30000000000000000000,30000000000000000000]");
		assertEquals(BigInteger.class, array.get(0).getClass());
		assertEquals(new BigInteger("-30000000000000000000"), array.get(0));
		assertEquals(BigInteger.class, array.get(1).getClass());
		assertEquals(new BigInteger("30000000000000000000"), array.get(1));
	}

	@Test
	public void testBigIntWrite() {
		JsonArray array = JsonArray.from(BigInteger.ONE, new BigInteger("-30000000000000000000"), new BigInteger(
				"30000000000000000000"));
		assertEquals("[1,-30000000000000000000,30000000000000000000]", JsonWriter.string().array(array).close());
	}

	/**
	 * Test around the edges of the integral types
	 */
	@Test
	public void testAroundEdges() throws JsonParserException {
		JsonArray array = JsonArray.from(Integer.MAX_VALUE, ((long)Integer.MAX_VALUE) + 1, Integer.MIN_VALUE,
				((long)Integer.MIN_VALUE) - 1, Long.MAX_VALUE, BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),
				Long.MIN_VALUE, BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE));
		String json = JsonWriter.string().array(array).close();
		assertEquals("[2147483647,2147483648,-2147483648,-2147483649,9223372036854775807,9223372036854775808,-9223372036854775808,-9223372036854775809]", json);
		JsonArray array2 = JsonParser.array().from(json);
		String json2 = JsonWriter.string().array(array2).close();
		assertEquals(json, json2);
	}
}
