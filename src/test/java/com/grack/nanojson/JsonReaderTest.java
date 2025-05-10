/*
 * Copyright 2011 The nanojson Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.grack.nanojson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.grack.nanojson.Users.Friend;

import org.junit.jupiter.api.Test;
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
	void object() throws JsonParserException {
		JsonReader reader = JsonReader.from("{\"a\":1}");
		assertEquals(JsonReader.Type.OBJECT, reader.current());
		reader.object();
		assertTrue(reader.next());
		assertEquals("a", reader.key());
		assertEquals(JsonReader.Type.NUMBER, reader.current());
		assertEquals(1, reader.intVal());
		assertFalse(reader.next());
	}

	/**
	 * Read a simple array.
	 */
	@Test
	void array() throws JsonParserException {
		JsonReader reader = JsonReader.from("[\"a\",1,null]");
		assertEquals(JsonReader.Type.ARRAY, reader.current());
		reader.array();
		assertTrue(reader.next());
		assertEquals(JsonReader.Type.STRING, reader.current());
		assertEquals("a", reader.string());
		
		assertTrue(reader.next());
		assertEquals(JsonReader.Type.NUMBER, reader.current());
		assertEquals(1, reader.intVal());

		assertTrue(reader.next());
		assertEquals(JsonReader.Type.NULL, reader.current());
		reader.nul();
		
		assertFalse(reader.next());
	}

	/**
	 * Assert all the things.
	 */
	@Test
	void nestedDetailed() throws JsonParserException {
		String json = createNestedJson();

		JsonReader reader = JsonReader.from(json);
		
		assertEquals(JsonReader.Type.OBJECT, reader.current());
		reader.object();
		
		assertTrue(reader.next());
		assertEquals("a", reader.key());
		assertEquals(JsonReader.Type.OBJECT, reader.current());
		reader.object();
		assertTrue(reader.next());

		assertEquals("b", reader.key());
		assertEquals(JsonReader.Type.ARRAY, reader.current());
		reader.array();

		for (int i = 0; i < 2; i++) {
			assertTrue(reader.next());
			assertEquals(JsonReader.Type.OBJECT, reader.current());
			reader.object();
	
			assertTrue(reader.next());
			assertEquals(i == 0 ? "a" : "c", reader.key());
			assertEquals(JsonReader.Type.NUMBER, reader.current());
			assertEquals(1, reader.intVal());
			assertTrue(reader.next());
			assertEquals(i == 0 ? "b" : "d", reader.key());
			assertEquals(JsonReader.Type.NUMBER, reader.current());
			assertEquals(2, reader.intVal());
			assertFalse(reader.next());
		}
		
		assertFalse(reader.next());
		assertTrue(reader.next());

		assertEquals("c", reader.key());
		assertEquals(JsonReader.Type.ARRAY, reader.current());
		reader.array();

		for (int i = 0; i < 3; i++) {
			assertTrue(reader.next());
			assertEquals(JsonReader.Type.STRING, reader.current());
			assertEquals("v" + i, reader.string());
		}
		
		assertFalse(reader.next());
	}

	/**
	 * Same test as {@link JsonReaderTest#nestedDetailed()}, less assertions to get a better
	 * feel for the API.
	 */
	@Test
	void nestedLight() throws JsonParserException {
		String json = createNestedJson();

		JsonReader reader = JsonReader.from(json);
		reader.object();
		assertTrue(reader.next());
		assertEquals("a", reader.key());
		reader.object();
		assertTrue(reader.next());
		assertEquals("b", reader.key());
		reader.array();

		for (int i = 0; i < 2; i++) {
			assertTrue(reader.next());

			reader.object();
			assertTrue(reader.next());
			assertEquals(i == 0 ? "a" : "c", reader.key());
			assertEquals(1, reader.intVal());
			assertTrue(reader.next());
			assertEquals(i == 0 ? "b" : "d", reader.key());
			assertEquals(2, reader.intVal());
			assertFalse(reader.next());
		}
		
		assertFalse(reader.next());
		assertTrue(reader.next());

		assertEquals("c", reader.key());
		reader.array();

		for (int i = 0; i < 3; i++) {
			assertTrue(reader.next());
			assertEquals("v" + i, reader.string());
		}
		
		assertFalse(reader.next());
		assertFalse(reader.next());
	}

	/**
	 * Test reading an multiple arrays (including an empty one) in a object.
	 */
	@Test
	void arraysInObject() throws JsonParserException {
		String json = createArraysInObject();
		JsonReader reader = JsonReader.from(json);

		reader.object();
		while (reader.next()) {
			reader.key();

			reader.array();
			while (reader.next())
				reader.intVal();
		}
	}

	private String createArraysInObject() {
		//@formatter:off
		String json = JsonWriter.string()
				.object()
					.array("a")
						.value(1)
						.value(3)
					.end()
					.array("b")
					.end()
					.array("c")
						.value(0)
					.end()
				.end()
			.done();
		//@formatter:on
		return json;
	}

	/**
	 * Test the {@link Users} class from java-json-benchmark.
	 */
	@Test
	void jsonBenchmarkUser() throws JsonParserException {
		JsonReader reader = JsonReader.from(getClass().getResourceAsStream("/users.json"));
		
		parseUsers(reader);
	}

	private static void parseUsers(JsonReader reader) throws JsonParserException {
        Users uc = new Users();
        uc.users = new ArrayList<>();
        reader.object();
        while (reader.next()) {
            if (reader.key().equals("users")) {
                reader.array();
                while (reader.next()) {
                    uc.users.add(parseUser(reader));
                }
            }
        }
	}

    private static User parseUser(JsonReader reader) throws JsonParserException {
        User u = new User();

        reader.object();

        while (reader.next()) {
            switch (reader.key()) {
                case "_id":
                    u._id = reader.string();
                    break;
                case "index":
                    u.index = reader.intVal();
                    break;
                case "guid":
                    u.guid = reader.string();
                    break;
                case "isActive":
                    u.isActive = reader.bool();
                    break;
                case "balance":
                    u.balance = reader.string();
                    break;
                case "picture":
                    u.picture = reader.string();
                    break;
                case "age":
                    u.age = reader.intVal();
                    break;
                case "eyeColor":
                    u.eyeColor = reader.string();
                    break;
                case "name":
                    u.name = reader.string();
                    break;
                case "gender":
                    u.gender = reader.string();
                    break;
                case "company":
                    u.company = reader.string();
                    break;
                case "email":
                    u.email = reader.string();
                    break;
                case "phone":
                    u.phone = reader.string();
                    break;
                case "address":
                    u.address = reader.string();
                    break;
                case "about":
                    u.about = reader.string();
                    break;
                case "registered":
                    u.registered = reader.string();
                    break;
                case "latitude":
                    u.latitude = reader.doubleVal();
                    break;
                case "longitude":
                    u.longitude = reader.doubleVal();
                    break;
                case "tags":
                    u.tags = new ArrayList<String>();
                    reader.array();
                    while (reader.next()) {
                        u.tags.add(reader.string());
                    }
                    break;
                case "friends":
                    u.friends = new ArrayList<Friend>();
                    reader.array();
                    while (reader.next()) {
                        reader.object();
                        Friend f = new Friend();
                        u.friends.add(f);
                        while (reader.next()) {
                            switch (reader.key()) {
                                case "id":
                                    f.id = reader.string();
                                    break;
                                case "name":
                                    f.name = reader.string();
                                    break;
								default:
									// Ignore unknown
									break;
                            }
                        }
                    }
                    break;
                case "greeting":
                    u.greeting = reader.string();
                    break;
                case "favoriteFruit":
                    u.favoriteFruit = reader.string();
                    break;
				default:
					// Ignore unknown
					break;
            }
        }

        return u;
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

	/**
	 * Entry point for test for profiling.
	 */
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
		 
		 for (int i = 0; i < 100000; i++) {
			 parseUsers(JsonReader.from(new ByteArrayInputStream(data)));
		 }
	}
}
