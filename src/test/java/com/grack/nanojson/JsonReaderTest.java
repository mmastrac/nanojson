package com.grack.nanojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.Test;

import com.grack.nanojson.Users.Friend;
import com.grack.nanojson.Users.User;

/**
 * Tests for {@link JsonReader}.
 */
public class JsonReaderTest {
	// CHECKSTYLE_OFF: MagicNumber
	/**
	 * Read a simple object.
	 */
	@Test
	public void testObject() throws JsonParserException {
		JsonReader reader = JsonReader.from("{\"a\":1}");
		assertEquals(JsonReader.Type.OBJECT, reader.current());
		reader.object();
		assertFalse(reader.done());
		assertEquals("a", reader.key());
		assertFalse(reader.done());
		assertEquals(JsonReader.Type.NUMBER, reader.current());
		assertEquals(1, reader.intVal());
		assertTrue(reader.done());
		assertFalse(reader.pop());
	}

	/**
	 * Read a simple array.
	 */
	@Test
	public void testArray() throws JsonParserException {
		JsonReader reader = JsonReader.from("[\"a\",1,null]");
		assertEquals(JsonReader.Type.ARRAY, reader.current());
		reader.array();
		assertFalse(reader.done());
		assertEquals(JsonReader.Type.STRING, reader.current());
		assertEquals("a", reader.string());
		
		assertFalse(reader.done());
		assertEquals(JsonReader.Type.NUMBER, reader.current());
		assertEquals(1, reader.intVal());

		assertFalse(reader.done());
		assertEquals(JsonReader.Type.NULL, reader.current());
		reader.nul();
		
		assertTrue(reader.done());
		assertFalse(reader.pop());
	}
	
	/**
	 * Assert all the things.
	 */
	@Test
	public void testNestedDetailed() throws JsonParserException {
		String json = createNestedJson();

		JsonReader reader = JsonReader.from(json);
		
		assertEquals(JsonReader.Type.OBJECT, reader.current());
		reader.object();
		
		assertEquals("a", reader.key());
		assertEquals(JsonReader.Type.OBJECT, reader.current());
		reader.object();
		
		assertEquals("b", reader.key());
		assertEquals(JsonReader.Type.ARRAY, reader.current());
		reader.array();

		for (int i = 0; i < 2; i++) {
			assertFalse(reader.done());
			assertEquals(JsonReader.Type.OBJECT, reader.current());
			reader.object();
	
			assertFalse(reader.done());
			assertEquals(JsonReader.Type.NUMBER, reader.current());
			assertEquals(1, reader.intVal());
			assertFalse(reader.done());
			assertEquals(JsonReader.Type.NUMBER, reader.current());
			assertEquals(2, reader.intVal());
			assertTrue(reader.done());
	
			assertTrue(reader.pop());
		}
		
		assertTrue(reader.done());
		assertTrue(reader.pop());
	
		assertEquals("c", reader.key());
		assertEquals(JsonReader.Type.ARRAY, reader.current());
		reader.array();

		for (int i = 0; i < 3; i++) {
			assertFalse(reader.done());
			assertEquals(JsonReader.Type.STRING, reader.current());
			assertEquals("v" + i, reader.string());
		}
		
		assertTrue(reader.done());
		assertTrue(reader.pop());
		
		assertTrue(reader.pop());
		assertFalse(reader.pop());
	}
	
	/**
	 * Same test as {@link JsonReaderTest#testNestedDetailed()}, less assertions to get a better
	 * feel for the API.
	 */
	@Test
	public void testNestedLight() throws JsonParserException {
		String json = createNestedJson();

		JsonReader reader = JsonReader.from(json);
		reader.object();
		
		assertEquals("a", reader.key());
		reader.object();
		
		assertEquals("b", reader.key());
		reader.array();

		for (int i = 0; i < 2; i++) {
			reader.object();
			assertEquals(1, reader.intVal());
			assertEquals(2, reader.intVal());
			assertTrue(reader.pop());
		}
		
		assertTrue(reader.pop());
	
		assertEquals("c", reader.key());
		reader.array();

		for (int i = 0; i < 3; i++) {
			assertEquals("v" + i, reader.string());
		}
		
		assertTrue(reader.pop());		
		assertTrue(reader.pop());
		assertFalse(reader.pop());
	}
	
	/**
	 * Test the {@link Users} class from java-json-benchmark.
	 */
	@Test
	public void testJsonBenchmarkUser() throws JsonParserException {
		JsonReader reader = JsonReader.from(getClass().getResourceAsStream("/users.json"));
		
		parseUsers(reader);
	}

	private static void parseUsers(JsonReader reader) throws JsonParserException {
		Users users = new Users();
		
		reader.object();
		while (!reader.done()) {
			if (reader.key().equals("users")) {
				users.users = new ArrayList<User>();
				reader.array();
				while (!reader.done()) {
					reader.object();
					User u = new User();
					users.users.add(u);
					
					while (!reader.done()) {
						switch (reader.key()) {
						case "_id":
							u._id = reader.string();
							break;
						case "age":
							u.age = reader.intVal();
							break;
						case "isActive":
							u.isActive = reader.bool();
							break;
						case "tags":
							u.tags = new ArrayList<String>();
							reader.array();
							while (!reader.done()) {
								u.tags.add(reader.string());
							}
							reader.pop();
							break;
						case "friends":
							u.friends = new ArrayList<Friend>();
							reader.array();
							while (!reader.done()) {
								reader.object();
								Friend f = new Friend();
								u.friends.add(f);
								while (!reader.done()) {
									switch (reader.key()) {
									case "id":
										f.id = reader.string();
										break;
									case "name":
										f.name = reader.string();
										break;
									default:
										fail();
									}
								}
								reader.pop();
							}
							break;
						default:
							fail();
						}
					}
					
					reader.pop();
				}
				reader.pop();
			}
		}
		reader.pop();
	}

	/**
	 * Useful method to generate a deeply nested JSON object.
	 */
	private String createNestedJson() {
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
						.value("c", JsonArray.from("v0", "v1", "v2"))
					.end()
				.end()
			.done();
		//@formatter:on
		return json;
	}
	
	public static void main(String[] args) throws IOException, JsonParserException {
		 InputStream stm = JsonReaderTest.class.getResourceAsStream("/users.json");
		 ByteArrayOutputStream out = new ByteArrayOutputStream();
		 byte[] buf = new byte[10 * 1024];
		 while (true) {
			 int n = stm.read(buf);
			 if (n <= 0)
				 break;
			 out.write(buf, 0, n);
		 }
		 
		 byte[] data = out.toByteArray();
		 
		 for (int i = 0; i < 10000; i++) {
			 parseUsers(JsonReader.from(new ByteArrayInputStream(data)));
		 }
	}
}
