package com.grack.nanojson;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.grack.nanojson.Users.Friend;
import com.grack.nanojson.Users.User;

public class JsonReaderTest {
	/**
	 * Assert all the things.
	 */
	@Test
	public void testNestedDetailed() {
		String json = createNestedJson();

		JsonReader reader = JsonReader.on(json);
		
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
			assertEquals("v" + i, reader.string());
		}
		
		assertTrue(reader.done());
		assertTrue(reader.pop());
		
		assertTrue(reader.pop());
		assertTrue(reader.pop());
		assertFalse(reader.pop());
	}
	
	/**
	 * Same test as {@link JsonReaderTest#testNestedDetailed()}, less assertions to get a better
	 * feel for the API.
	 */
	@Test
	public void testNestedLight() {
		String json = createNestedJson();

		JsonReader reader = JsonReader.on(json);
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
		assertTrue(reader.pop());
		assertFalse(reader.pop());
	}
	
	@Test
	public void testJsonBenchmarkUser() {
		JsonReader reader = JsonReader.on(blah);
		
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
									}
								}
								reader.pop();
							}
						}
					}
					
					reader.pop();
				}
				reader.pop();
			}
		}
		reader.pop();
	}

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
}
