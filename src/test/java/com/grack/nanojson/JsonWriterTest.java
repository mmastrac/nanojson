package com.grack.nanojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.junit.Test;

public class JsonWriterTest {
	/**
	 * Test emitting simple values.
	 */
	@Test
	public void testSimpleValues() {
		assertEquals("true", JsonWriter.string().value(true).close());
		assertEquals("null", JsonWriter.string().nul().close());
		assertEquals("1.0", JsonWriter.string().value(1.0).close());
		assertEquals("1.0", JsonWriter.string().value(1.0f).close());
		assertEquals("1", JsonWriter.string().value(1).close());
		assertEquals("\"abc\"", JsonWriter.string().value("abc").close());
	}

	/**
	 * Test various ways of writing null, as well as various situations.
	 */
	@Test
	public void testNull() {
		assertEquals("null", JsonWriter.string().value((String)null).close());
		assertEquals("null", JsonWriter.string().value((Number)null).close());
		assertEquals("null", JsonWriter.string().nul().close());
		assertEquals("[null]", JsonWriter.string().array().value((String)null).end().close());
		assertEquals("[null]", JsonWriter.string().array().value((Number)null).end().close());
		assertEquals("[null]", JsonWriter.string().array().nul().end().close());
		assertEquals("{\"a\":null}", JsonWriter.string().object().value("a", (String)null).end().close());
		assertEquals("{\"a\":null}", JsonWriter.string().object().value("a", (Number)null).end().close());
		assertEquals("{\"a\":null}", JsonWriter.string().object().nul("a").end().close());
	}
	
	/**
	 * Test escaping of chars < 256.
	 */
	@Test
	public void testStringControlCharacters() {
		StringBuilder chars = new StringBuilder();
		for (int i = 0; i < 0xa0; i++)
			chars.append((char)i);

		assertEquals(
				"\"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000b\\f\\r\\u000e\\u000f\\u0010"
						+ "\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d"
						+ "\\u001e\\u001f !\\\"#$%&'()*+,-./0123456789:;<=>?@"
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\\u0080\\u0081\\u0082"
						+ "\\u0083\\u0084\\u0085\\u0086\\u0087\\u0088\\u0089\\u008a\\u008b\\u008c\\u008d\\u008e\\u008f"
						+ "\\u0090\\u0091\\u0092\\u0093\\u0094\\u0095\\u0096\\u0097\\u0098\\u0099\\u009a\\u009b\\u009c"
						+ "\\u009d\\u009e\\u009f\"", JsonWriter.string(chars.toString()));
	}

	@Test
	public void testWriteToSystemOut() {
		JsonWriter.on(System.out)
			.object()
				.value("a", 1)
				.value("b", 2)
			.end()
		.close();
	}
	
	/**
	 * Test escaping of / when following < to handle &lt;/script&gt;.
	 */
	@Test
	public void testScriptEndEscaping() {
		assertEquals("\"<\\/script>\"", JsonWriter.string("</script>"));
		assertEquals("\"/script\"", JsonWriter.string("/script"));
	}

	/**
	 * Test a simple array.
	 */
	@Test
	public void testArray() {
		String json = JsonWriter.string().array().value(true).value(false).value(true).end().close();
		assertEquals("[true,false,true]", json);
	}

	/**
	 * Test a nested array.
	 */
	@Test
	public void testNestedArray() {
		String json = JsonWriter.string().array().array().array().value(true).value(false).value(true).end().end()
				.end().close();
		assertEquals("[[[true,false,true]]]", json);
	}

	/**
	 * Test a nested array.
	 */
	@Test
	public void testNestedArray2() {
		String json = JsonWriter.string().array().value(true).array().array().value(false).end().end().value(true)
				.end().close();
		assertEquals("[true,[[false]],true]", json);
	}

	/**
	 * Test a simple object.
	 */
	@Test
	public void testObject() {
		String json = JsonWriter.string().object().value("a", true).value("b", false).value("c", true).end().close();
		assertEquals("{\"a\":true,\"b\":false,\"c\":true}", json);
	}

	/**
	 * Test a nested object.
	 */
	@Test
	public void testNestedObject() {
		String json = JsonWriter.string().object().object("a").value("b", false).value("c", true).end().end().close();
		assertEquals("{\"a\":{\"b\":false,\"c\":true}}", json);
	}

	/**
	 * Test a nested object and array.
	 */
	@Test
	public void testNestedObjectArray() {
		//@formatter:off
		String json = JsonWriter.string()
				.object()
					.object("a")
						.array("b")
							.object()
								.value("a", 1)
								.value("b", 2)
							.end()
							.object()
								.value("c", 1.0)
								.value("d", 2.0)
							.end()
						.end()
						.value("c", JsonArray.from("a", "b", "c"))
					.end()
				.end()
			.close();
		//@formatter:on
		assertEquals("{\"a\":{\"b\":[{\"a\":1,\"b\":2},{\"c\":1.0,\"d\":2.0}],\"c\":[\"a\",\"b\",\"c\"]}}", json);
	}

	/**
	 * Tests the {@link Appendable} code.
	 */
	@Test
	public void testAppendable() {
		StringWriter writer = new StringWriter();
		JsonWriter.on(writer).object().value("abc", "def").end().close();
		assertEquals("{\"abc\":\"def\"}", writer.toString());
	}

	/**
	 * Tests the {@link OutputStream} code.
	 */
	@Test
	public void testOutputStream() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JsonWriter.on(out).object().value("abc", "def").end().close();
		assertEquals("{\"abc\":\"def\"}", new String(out.toByteArray(), Charset.forName("UTF-8")));
	}

	@Test
	public void testQuickJson() {
		assertEquals("true", JsonWriter.string(true));
	}

	@Test
	public void testQuickJsonArray() {
		assertEquals("[1,2,3]", JsonWriter.string(JsonArray.from(1, 2, 3)));
	}

	@Test
	public void testFailureNoKeyInObject() {
		try {
			JsonWriter.string().object().value(true).end().close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureNoKeyInObject2() {
		try {
			JsonWriter.string().object().value("a", 1).value(true).end().close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureKeyInArray() {
		try {
			JsonWriter.string().array().value("x", true).end().close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureKeyInArray2() {
		try {
			JsonWriter.string().array().value(1).value("x", true).end().close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureNotFullyClosed() {
		try {
			JsonWriter.string().array().value(1).close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureNotFullyClosed2() {
		try {
			JsonWriter.string().array().close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureEmpty() {
		try {
			JsonWriter.string().close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureEmpty2() {
		try {
			JsonWriter.string().end();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureMoreThanOneRoot() {
		try {
			JsonWriter.string().value(1).value(1).close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureMoreThanOneRoot2() {
		try {
			JsonWriter.string().array().value(1).end().value(1).close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

	@Test
	public void testFailureMoreThanOneRoot3() {
		try {
			JsonWriter.string().array().value(1).end().array().value(1).end().close();
			fail();
		} catch (JsonWriterException e) {
			// OK
		}
	}

}
