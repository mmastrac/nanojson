/**
 * Copyright 2010 The nanojson Authors
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
import java.util.regex.Pattern;

/**
 * Simple JSON parser.
 * 
 * <pre>
 * Object json = {@link JsonParser}.parse("{\"a\":[true,false], \"b\":1}");
 * </pre>
 */
public class JsonParser {
	private final String input;
	private int index, linePos = 1, charPos = 0;

	private Token token;
	private int tokenStart, tokenLinePos, tokenCharPos;

	/**
	 * Regex representation of http://json.org/number.gif.
	 */
	private static final Pattern NUMBER_PATTERN = Pattern
			.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");

	/**
	 * The tokens available in JSON.
	 */
	private enum Token {
		EOF, NULL, TRUE, FALSE, STRING, NUMBER, COMMA, COLON, OBJECT_START, OBJECT_END, ARRAY_START, ARRAY_END
	}

	private JsonParser(String input) {
		this.input = input;
	}

	/**
	 * Parses a string into the appropriate root object for the given JSON.
	 */
	public static Object parse(String input) throws JsonParserException {
		return new JsonParser(input).parse();
	}

	/**
	 * Parse a single JSON value from the string, expecting an EOF at the end.
	 */
	private Object parse() throws JsonParserException {
		advanceToken();
		Object value = parseJsonValue();
		if (advanceToken() != Token.EOF)
			throw createParseException("Expected end of input, got " + token);
		return value;
	}

	/**
	 * Starts parsing a JSON value at the current token position.
	 */
	private Object parseJsonValue() throws JsonParserException {
		switch (token) {
		case EOF:
		case COLON:
		case COMMA:
		case ARRAY_END:
		case OBJECT_END:
			// None of these should appear when we're in the context of parsing
			// a JSON value
			throw createParseException("Expected JSON value, got " + token);
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

		throw createParseException("Internal error. Unhandled token "
				+ token);
	}

	/**
	 * Parses the current token as a number by passing it into
	 * {@link Double#parseDouble(String)}.
	 */
	private Object tokenAsNumber() throws JsonParserException {
		String number = input.substring(tokenStart, index);

		// Special zero handling to match JSON spec. Leading zero only allowed
		// if next character is . or e or E.
		if (!NUMBER_PATTERN.matcher(number).matches())
			throw createParseException("Mailformed number: " + number);

		try {
			return Double.parseDouble(number);
		} catch (NumberFormatException e) {
			throw createParseException("Malformed number: " + number);
		}
	}

	/**
	 * Parses the current token as a string. Assumes the tokenizer has left the
	 * quotes at the beginning and end of the string.
	 */
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

	/**
	 * Parses a JSON object from the current token. Assumes that a
	 * {@link Token#OBJECT_START} has been consumed.
	 */
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
					throw createParseException("Expected COMMA, got "
							+ token);
				advanceToken();

				if (token == Token.OBJECT_END)
					throw createParseException("Trailing comma in object");
			}

			if (token != Token.STRING)
				throw createParseException("Expected STRING, got " + token);
			
			String key = tokenAsString();
			if (advanceToken() != Token.COLON)
				throw createParseException("Expected COLON, got " + token);
			advanceToken();
			Object value = parseJsonValue();

			map.put(key, value);
		}
	}

	/**
	 * Parses a JSON array from the current token. Assumes that a
	 * {@link Token#ARRAY_START} has been consumed.
	 */
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
					throw createParseException("Expected a comma, got "
							+ token);
				advanceToken();
			}

			list.add(parseJsonValue());
		}
	}

	/**
	 * Expects a given string at the current position.
	 */
	private void expect(String expected) throws JsonParserException {
		for (int i = 0; i < expected.length() - 1; i++) {
			int c = advanceChar();
			if (c != expected.charAt(i + 1)) {
				// Consume the whole pseudo-token to make a better error message
				while (isAsciiLetter(peekChar()))
					advanceChar();
				throw createParseException("Unexpected token '"
						+ input.substring(tokenStart,
								Math.min(index, input.length()))
						+ "'. Did you mean '" + expected + "'?");
			}
		}

		// The token should end with something other than an ASCII letter
		if (isAsciiLetter(peekChar())) {
			// Consume the whole pseudo-token to make a better error message
			while (isAsciiLetter(peekChar()))
				advanceChar();
			throw createParseException("Unexpected token '"
					+ input.substring(tokenStart,
							Math.min(index, input.length()))
					+ "'. Did you mean '" + expected + "'?");
		}
	}

	/**
	 * Consumes a token, first eating up any whitespace ahead of it. Note that
	 * number tokens are not necessarily valid numbers.
	 */
	private Token advanceToken() throws JsonParserException {
		int c;
		do {
			c = advanceChar();
		} while (isWhitespace(c));

		tokenStart = index - 1;
		tokenLinePos = linePos;
		tokenCharPos = charPos;

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
			expect("true");
			return token = Token.TRUE;
		case 'f':
			expect("false");
			return token = Token.FALSE;
		case 'n':
			expect("null");
			return token = Token.NULL;
		case '\"':
			advanceTokenString();
			return token = Token.STRING;
		case '-':
		case '+':
		case '.':
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

		if (isAsciiLetter(peekChar())) {
			// Consume the whole pseudo-token to make a better error message
			while (isAsciiLetter(peekChar()))
				advanceChar();
			throw createParseException("Unexpected unquoted token '"
					+ input.substring(tokenStart,
							Math.min(index, input.length())) + "'");
		}

		throw createParseException("Unexpected character: " + (char) c);
	}

	/**
	 * Steps through to the end of the current number token (a non-digit token).
	 */
	private void advanceTokenNumber() {
		while (isDigitCharacter(peekChar()))
			advanceChar();
	}

	/**
	 * Steps through to the end of the current string token (the unescaped
	 * double quote).
	 */
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
					throw createParseException("Invalid escape: \\"
							+ (char) escape);
			}
		}
	}

	/**
	 * Advances a character, throwing if it is illegal in the context of a JSON
	 * string.
	 */
	private int stringChar() throws JsonParserException {
		int c = advanceChar();
		if (c == -1)
			throw createParseException(
					"String was not terminated before end of input");
		if (c < 32)
			throw createParseException(
					"Strings may not contain control characters: 0x" + Integer.toString(c, 16));
		return c;
	}

	/**
	 * Advances a character, throwing if it is illegal in the context of a JSON
	 * string hex unicode escape.
	 */
	private int stringHexChar() throws JsonParserException {
		int c = stringChar();
		if (isHexCharacter(c))
			return c;
		throw createParseException("Expected unicode hex escape character");
	}

	/**
	 * Quick test for hex digit characters.
	 */
	private boolean isHexCharacter(int c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
				|| (c >= 'A' && c <= 'F');
	}

	/**
	 * Quick test for digit characters.
	 */
	private boolean isDigitCharacter(int c) {
		return (c >= '0' && c <= '9') || c == 'e' || c == 'E' || c == '.'
				|| c == '+' || c == '-';
	}

	/**
	 * Quick test for whitespace characters.
	 */
	private boolean isWhitespace(int c) {
		return c == ' ' || c == '\n' || c == '\r' || c == '\t';
	}

	/**
	 * Quick test for ASCII letter characters.
	 */
	private boolean isAsciiLetter(int c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
	}

	/**
	 * Peek one char ahead, don't advance, returns {@link Token#EOF} on end of
	 * input.
	 */
	private int peekChar() {
		int i = index;
		if (i >= input.length())
			return -1;
		return input.charAt(i);
	}

	/**
	 * Advance one character ahead, or return {@link Token#EOF} on end of input.
	 */
	private int advanceChar() {
		int i = index++;
		if (i >= input.length())
			return -1;
		char c = input.charAt(i);
		if (c == '\n') {
			linePos++;
			charPos = 0;
		} else {
			charPos++;
		}
		
		return c;
	}

	/**
	 * Creates a {@link JsonParserException} and fills it from the current line and char position.
	 */
	private JsonParserException createParseException(String message) {
		return new JsonParserException(message + " on line " + tokenLinePos + ", char " + tokenCharPos, tokenLinePos, tokenCharPos);
	}
}
