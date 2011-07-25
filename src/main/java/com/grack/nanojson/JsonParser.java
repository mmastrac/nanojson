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

import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;

/**
 * Simple JSON parser.
 * 
 * <pre>
 * Object json = {@link JsonParser}.parse("{\"a\":[true,false], \"b\":1}");
 * JsonObject json = {@link JsonParser}.parseObject("{\"a\":[true,false], \"b\":1}");
 * JsonArray json = {@link JsonParser}.parseArray("[1, {\"a\":[true,false], \"b\":1}]");
 * </pre>
 */
public final class JsonParser {
	private int linePos = 1, rowPos = 0;
	private boolean eof;
	private int index;
	private Object value;
	private Token token;
	private int tokenStart, tokenLinePos, tokenCharPos;
	private StringBuilder stringToken = new StringBuilder(20);
	private final Reader reader;
	private final char[] buffer;
	private int bufferLength;
	private static final char[] TRUE = { 'r', 'u', 'e' };
	private static final char[] FALSE = { 'a', 'l', 's', 'e' };
	private static final char[] NULL = { 'u', 'l', 'l' };

	/**
	 * The tokens available in JSON.
	 */
	enum Token {
		EOF(false), NULL(true), TRUE(true), FALSE(true), STRING(true), NUMBER(true), COMMA(false), COLON(false), //
		OBJECT_START(true), OBJECT_END(false), ARRAY_START(true), ARRAY_END(false);
		boolean isValue;

		Token(boolean isValue) {
			this.isValue = isValue;
		}
	}

	private JsonParser(Reader reader) throws JsonParserException {
		this.reader = reader;
		this.buffer = new char[32 * 1024];
		try {
			eof = refillBuffer();
		} catch (IOException e) {
			throw createParseException(e, "IOException");
		}
	}

	private JsonParser(String input) throws JsonParserException {
		this.reader = null;
		this.buffer = input.toCharArray();
		this.bufferLength = buffer.length;
		eof = (bufferLength == 0);
	}

	/**
	 * Parses a string into the appropriate root object for the given JSON.
	 */
	public static Object parse(String input) throws JsonParserException {
		return new JsonParser(input).parse();
	}

	/**
	 * Parses a string into a {@link JsonObject}.
	 */
	public static JsonObject parseObject(String input) throws JsonParserException {
		Object o = parse(input);
		if (o instanceof JsonObject)
			return ((JsonObject)o);

		throw new JsonParserException("JSON did not contain an object", 0, 0);
	}

	/**
	 * Parses a string into a {@link JsonArray}.
	 */
	public static JsonArray parseArray(String input) throws JsonParserException {
		Object o = parse(input);
		if (o instanceof JsonArray)
			return ((JsonArray)o);

		throw new JsonParserException("JSON did not contain an array", 0, 0);
	}

	/**
	 * Parses a string into the appropriate root object for the given JSON.
	 */
	public static Object parse(Reader reader) throws JsonParserException {
		return new JsonParser(reader).parse();
	}

	/**
	 * Parses a string into a {@link JsonObject}.
	 */
	public static JsonObject parseObject(Reader reader) throws JsonParserException {
		Object o = parse(reader);
		if (o instanceof JsonObject)
			return ((JsonObject)o);

		throw new JsonParserException("JSON did not contain an object", 0, 0);
	}

	/**
	 * Parses a string into a {@link JsonArray}.
	 */
	public static JsonArray parseArray(Reader reader) throws JsonParserException {
		Object o = parse(reader);
		if (o instanceof JsonArray)
			return ((JsonArray)o);

		throw new JsonParserException("JSON did not contain an array", 0, 0);
	}

	/**
	 * Parse a single JSON value from the string, expecting an EOF at the end.
	 */
	Object parse() throws JsonParserException {
		try {
			advanceToken();
			Object value = currentValue();
			if (advanceToken() != Token.EOF)
				throw createParseException("Expected end of input, got " + token);
			return value;
		} catch (IOException e) {
			throw createParseException(e, "IOException");
		}
	}

	/**
	 * Starts parsing a JSON value at the current token position.
	 */
	private Object currentValue() throws JsonParserException {
		// Only a value start token should appear when we're in the context of parsing a JSON value
		if (token.isValue)
			return value;
		throw createParseException("Expected JSON value, got " + token);
	}

	/**
	 * Expects a given string at the current position.
	 */
	private void expect(int first, char[] expected) throws JsonParserException, IOException {
		for (int i = 0; i < expected.length; i++) {
			if (advanceChar() != expected[i])
				throwHelpfulException(first, expected, i);
		}

		// The token should end with something other than an ASCII letter
		if (isAsciiLetter(peekChar()))
			throwHelpfulException(first, expected, expected.length);
	}

	private void throwHelpfulException(int c, char[] expected, int i) throws JsonParserException, IOException {
		// Build the first part of the token
		String token = (char)c + (expected == null ? "" : new String(expected, 0, i));

		// Consume the whole pseudo-token to make a better error message
		while (isAsciiLetter(peekChar()) && (index - tokenStart) < 15)
			token += (char)advanceChar();
		throw createParseException("Unexpected token '" + token + "'"
				+ (expected == null ? "" : ". Did you mean '" + (char)c + new String(expected) + "'?"));
	}

	/**
	 * Consumes a token, first eating up any whitespace ahead of it. Note that number tokens are not necessarily valid
	 * numbers.
	 */
	private Token advanceToken() throws JsonParserException, IOException {
		int c = advanceChar();
		while (isWhitespace(c))
			c = advanceChar();

		tokenStart = index - 1;
		tokenLinePos = linePos;
		tokenCharPos = index - rowPos;

		switch (c) {
		case -1:
			return token = Token.EOF;
		case '[': { // Inline to avoid additional stack
			JsonArray list = new JsonArray();
			if (advanceToken() != Token.ARRAY_END)
				while (true) {
					list.add(currentValue());
					if (advanceToken() == Token.ARRAY_END)
						break;
					if (token != Token.COMMA)
						throw createParseException("Expected a comma or end of the array instead of " + token);
					if (advanceToken() == Token.ARRAY_END)
						throw createParseException("Trailing comma found in array");
				}
			value = list;
			return token = Token.ARRAY_START;
		}
		case ']':
			return token = Token.ARRAY_END;
		case ',':
			return token = Token.COMMA;
		case ':':
			return token = Token.COLON;
		case '{': { // Inline to avoid additional stack
			JsonObject map = new JsonObject();
			if (advanceToken() != Token.OBJECT_END)
				while (true) {
					if (token != Token.STRING)
						throw createParseException("Expected STRING, got " + token);
					String key = (String)value;
					if (advanceToken() != Token.COLON)
						throw createParseException("Expected COLON, got " + token);
					advanceToken();
					map.put(key, currentValue());
					if (advanceToken() == Token.OBJECT_END)
						break;
					if (token != Token.COMMA)
						throw createParseException("Expected a comma or end of the object instead of " + token);
					if (advanceToken() == Token.OBJECT_END)
						throw createParseException("Trailing object found in array");
				}
			value = map;
			return token = Token.OBJECT_START;
		}
		case '}':
			return token = Token.OBJECT_END;
		case 't':
			expect(c, TRUE);
			value = Boolean.TRUE;
			return token = Token.TRUE;
		case 'f':
			expect(c, FALSE);
			value = Boolean.FALSE;
			return token = Token.FALSE;
		case 'n':
			expect(c, NULL);
			value = null;
			return token = Token.NULL;
		case '\"':
			value = advanceTokenString();
			return token = Token.STRING;
		case '-':
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
			value = advanceTokenNumber(c);
			return token = Token.NUMBER;
		case '+':
			throw createParseException("Numbers may not start with '+'");
		}

		if (isAsciiLetter(peekChar()))
			throwHelpfulException(c, null, 0);

		throw createParseException("Unexpected character: " + (char)c);
	}

	/**
	 * Steps through to the end of the current number token (a non-digit token).
	 */
	private Number advanceTokenNumber(int c) throws JsonParserException, IOException {
		stringToken.setLength(0);
		stringToken.append((char)c);
		boolean isDouble = false;
		while (isDigitCharacter(peekChar())) {
			char next = (char)advanceChar();
			isDouble |= next == '.' || next == 'e' || next == 'E';
			stringToken.append(next);
		}

		String number = stringToken.toString();

		try {
			if (isDouble) {
				// Special zero handling to match JSON spec. Leading zero is only allowed if next character is . or e
				if (number.charAt(0) == '0') {
					if (number.charAt(1) == '.') {
						if (number.length() == 2)
							throw createParseException("Malformed number: " + number);
					} else if (number.charAt(1) != 'e' && number.charAt(1) != 'E')
						throw createParseException("Malformed number: " + number);
				}
				if (number.length() > 1 && number.charAt(0) == '-') {
					if (number.charAt(1) == '0') {
						if (number.charAt(2) == '.') {
							if (number.length() == 3)
								throw createParseException("Malformed number: " + number);
						} else if (number.charAt(2) != 'e' && number.charAt(2) != 'E')
							throw createParseException("Malformed number: " + number);
					} else if (number.charAt(1) == '.') {
						throw createParseException("Malformed number: " + number);
					}
				}

				return Double.parseDouble(number);
			}

			// Special zero handling to match JSON spec. No leading zeros allowed for integers.
			if (number.charAt(0) == '0') {
				if (number.length() == 1)
					return 0;
				throw createParseException("Malformed number: " + number);
			}
			if (number.length() > 1 && number.charAt(0) == '-' && number.charAt(1) == '0') {
				if (number.length() == 2)
					return 0;
				throw createParseException("Malformed number: " + number);
			}

			// HACK: Attempt to parse using the approximate best type for this
			int length = number.charAt(0) == '-' ? number.length() - 1 : number.length();
			if (length < 10) // 214 748 364 7
				return Integer.parseInt(number);
			if (length < 19) // 9 223 372 036 854 775 807
				return Long.parseLong(number);
			return new BigInteger(number);
		} catch (NumberFormatException e) {
			throw createParseException(e, "Malformed number: " + number);
		}
	}

	/**
	 * Steps through to the end of the current string token (the unescaped double quote).
	 */
	private String advanceTokenString() throws JsonParserException, IOException {
		stringToken.setLength(0);
		while (true) {
			char c = stringChar();
			if (c == '\"')
				break;

			if (c == '\\') {
				int escape = advanceChar();
				switch (escape) {
				case 'b':
					stringToken.append('\b');
					break;
				case 'f':
					stringToken.append('\f');
					break;
				case 'n':
					stringToken.append('\n');
					break;
				case 'r':
					stringToken.append('\r');
					break;
				case 't':
					stringToken.append('\t');
					break;
				case '"':
				case '/':
				case '\\':
					stringToken.append((char)escape);
					break;
				case 'u':
					stringToken
							.append((char)(stringHexChar() << 12 | stringHexChar() << 8 | stringHexChar() << 4 | stringHexChar()));
					break;
				default:
					throw createParseException("Invalid escape: \\" + (char)escape);
				}
			} else
				stringToken.append(c);
		}
		return stringToken.toString();
	}

	/**
	 * Advances a character, throwing if it is illegal in the context of a JSON string.
	 */
	private char stringChar() throws JsonParserException, IOException {
		int c = advanceChar();
		if (c == -1)
			throw createParseException("String was not terminated before end of input");
		if (c < 32)
			throw createParseException("Strings may not contain control characters: 0x" + Integer.toString(c, 16));
		return (char)c;
	}

	/**
	 * Advances a character, throwing if it is illegal in the context of a JSON string hex unicode escape.
	 */
	private int stringHexChar() throws JsonParserException, IOException {
		int c = Character.digit(advanceChar(), 16);
		if (c == -1)
			throw createParseException("Expected unicode hex escape character");
		return c;
	}

	/**
	 * Quick test for digit characters.
	 */
	private boolean isDigitCharacter(int c) {
		return (c >= '0' && c <= '9') || c == 'e' || c == 'E' || c == '.' || c == '+' || c == '-';
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

	private boolean refillBuffer() throws JsonParserException, IOException {
		int r = reader.read(buffer, 0, buffer.length);
		if (r <= 0)
			return true;
		bufferLength = r;
		index = 0;
		tokenStart = 0;
		return false;
	}

	/**
	 * Peek one char ahead, don't advance, returns {@link Token#EOF} on end of input.
	 */
	private int peekChar() {
		if (eof)
			return -1;
		return buffer[index];
	}

	/**
	 * Advance one character ahead, or return {@link Token#EOF} on end of input.
	 */
	private int advanceChar() throws JsonParserException, IOException {
		if (eof)
			return -1;
		int c = buffer[index];
		if (c == '\n') {
			linePos++;
			rowPos = index + 1;
		}

		index++;
		if (index >= bufferLength)
			eof = (reader == null) ? true : refillBuffer();

		return c;
	}

	/**
	 * Creates a {@link JsonParserException} and fills it from the current line and char position.
	 */
	private JsonParserException createParseException(String message) {
		return new JsonParserException(message + " on line " + tokenLinePos + ", char " + tokenCharPos, tokenLinePos,
				tokenCharPos);
	}

	/**
	 * Creates a {@link JsonParserException} and fills it from the current line and char position.
	 */
	private JsonParserException createParseException(Exception e, String message) {
		return new JsonParserException(e, message + " on line " + tokenLinePos + ", char " + tokenCharPos,
				tokenLinePos, tokenCharPos);
	}
}
