/**
 * Copyright 2010 The nanajson Authors
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple JSON parser.
 */
public class JsonParser {
	private final String input;
	private int index;

	private Token token;
	private int tokenStart;

	private enum Token {
		EOF, NULL, TRUE, FALSE, STRING, NUMBER, COMMA, COLON, OBJECT_START, OBJECT_END, ARRAY_START, ARRAY_END
	}

	private JsonParser(String input) {
		this.input = input;
	}

	public static Object parse(String input) throws JsonParserException {
		return new JsonParser(input).parse();
	}

	private Object parse() throws JsonParserException {
		advanceToken();
		Object value = parseJsonValue();
		if (advanceToken() != Token.EOF)
			throw new JsonParserException("Expected end of input, got " + token);
		return value;
	}

	private Object parseJsonValue() throws JsonParserException {
		switch (token) {
		case EOF:
		case COLON:
		case COMMA:
		case ARRAY_END:
		case OBJECT_END:
			throw new JsonParserException("Expected JSON value, got " + token);
		case TRUE:
			return Boolean.TRUE;
		case FALSE:
			return Boolean.FALSE;
		case NULL:
			return null;
		case STRING:
			return tokenAsString();
		case NUMBER:
			return tokenAsNumber();
		case ARRAY_START:
			return parseArray();
		case OBJECT_START:
			return parseObject();
		}

		throw new JsonParserException("Internal error. Unhandled token "
				+ token);
	}

	private Object tokenAsNumber() {
		return Double.parseDouble(input.substring(tokenStart, index));
	}

	private String tokenAsString() throws JsonParserException {
		StringBuilder s = new StringBuilder();
		for (int i = tokenStart + 1; i < index - 1; i++) {
			int c = input.charAt(i);
			if (c == '\\') {
				char escape = input.charAt(++i);
				switch (escape) {
				case 'n':
					s.append('\n');
					break;
				case 't':
					s.append('\t');
					break;
				case 'b':
					s.append('\b');
					break;
				case 'f':
					s.append('\f');
					break;
				case 'r':
					s.append('\r');
					break;
				case 'u':
					s.append((char) Integer.parseInt(
							input.substring(i + 1, i + 5), 16));
					i += 4;
					break;
				default:
					// Assume the tokenizer ensured that this was a valid escape
					s.append(escape);
				}
			} else
				s.append((char) c);
		}

		return s.toString();
	}

	private Map<String, Object> parseObject() throws JsonParserException {
		Map<String, Object> map = new HashMap<String, Object>();
		boolean first = true;
		while (true) {
			if (advanceToken() == Token.OBJECT_END)
				return map;

			if (first)
				first = false;
			else {
				if (token != Token.COMMA)
					throw new JsonParserException("Expected a comma");
				advanceToken();
			}

			String key = tokenAsString();
			if (advanceToken() != Token.COLON)
				throw new JsonParserException("Expected COLON, got " + token);
			advanceToken();
			Object value = parseJsonValue();

			map.put(key, value);
		}
	}

	private List<Object> parseArray() throws JsonParserException {
		List<Object> list = new ArrayList<Object>();
		boolean first = true;
		while (true) {
			if (advanceToken() == Token.ARRAY_END)
				return list;

			if (first)
				first = false;
			else {
				if (token != Token.COMMA)
					throw new JsonParserException("Expected a comma");
				advanceToken();
			}

			list.add(parseJsonValue());
		}
	}

	private void expect(String expected) throws JsonParserException {
		for (int i = 0; i < expected.length(); i++) {
			int c = advanceChar();
			if (!isAsciiLetter(c))
				throw new JsonParserException("Unexpected token");
			if (c != expected.charAt(i))
				throw new JsonParserException("Unexpected token");
		}

		if (isAsciiLetter(peekChar()))
			throw new JsonParserException("Unexpected token");
	}

	private Token advanceToken() throws JsonParserException {
		int c;
		do {
			c = advanceChar();
		} while (isWhitespace(c));

		tokenStart = index - 1;

		switch (c) {
		case -1:
			return token = Token.EOF;
		case '[':
			return token = Token.ARRAY_START;
		case ']':
			return token = Token.ARRAY_END;
		case ',':
			return token = Token.COMMA;
		case ':':
			return token = Token.COLON;
		case '{':
			return token = Token.OBJECT_START;
		case '}':
			return token = Token.OBJECT_END;
		case 't':
			expect("rue");
			return token = Token.TRUE;
		case 'f':
			expect("alse");
			return token = Token.FALSE;
		case 'n':
			expect("ull");
			return token = Token.NULL;
		case '\"':
			advanceTokenString();
			return token = Token.STRING;
		case '-':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			advanceTokenNumber();
			return token = Token.NUMBER;
		}

		throw new JsonParserException("Unexpected token");
	}

	private void advanceTokenNumber() {
		while (isDigitCharacter(peekChar()))
			advanceChar();
	}

	private void advanceTokenString() throws JsonParserException {
		while (true) {
			int c = stringChar();
			if (c == '\"')
				break;

			if (c == '\\') {
				int escape = stringChar();
				if (escape == 'u') {
					for (int i = 0; i < 4; i++)
						stringHexChar();
				} else if ("bfnrt/\\\"".indexOf(escape) == -1)
					throw new JsonParserException("Invalid escape: \\"
							+ (char) escape);
			}
		}
	}

	private int stringChar() throws JsonParserException {
		int c = advanceChar();
		if (c == -1)
			throw new JsonParserException(
					"String was not terminated before end of input");
		if (c < 32)
			throw new JsonParserException(
					"Strings may not contain control characters");
		return c;
	}

	private int stringHexChar() throws JsonParserException {
		int c = stringChar();
		if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
				|| (c >= 'A' && c <= 'F'))
			return c;
		throw new JsonParserException("Expected unicode hex escape character");
	}

	private boolean isDigitCharacter(int c) {
		return ((c >= '0' && c <= '9') || c == 'e' || c == 'E' || c == '.'
				|| c == '+' || c == '-');
	}

	private boolean isWhitespace(int c) {
		return c == ' ' || c == '\n' || c == '\r' || c == '\t';
	}

	private boolean isAsciiLetter(int c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
	}

	private int peekChar() {
		int i = index;
		if (i >= input.length())
			return -1;
		return input.charAt(i);
	}

	private int advanceChar() {
		int i = index++;
		if (i >= input.length())
			return -1;
		return input.charAt(i);
	}

	public static void main(String[] args) throws JsonParserException {
		System.out
				.println(JsonParser
						.parse("[\"a b c\\u0060d\", 1.23e6, null, false, {\"abc\":123, \"def\":\"abc\"}]"));
	}
}
