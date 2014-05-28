/**
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Simple JSON parser.
 * 
 * <pre>
 * Object json = {@link JsonParser}.any().from("{\"a\":[true,false], \"b\":1}");
 * Number json = ({@link Number}){@link JsonParser}.any().from("123.456e7");
 * JsonObject json = {@link JsonParser}.object().from("{\"a\":[true,false], \"b\":1}");
 * JsonArray json = {@link JsonParser}.array().from("[1, {\"a\":[true,false], \"b\":1}]");
 * </pre>
 */
public final class JsonParser {
	private static final int BUFFER_ROOM = 20;
	static final int BUFFER_SIZE = 32 * 1024;

	private int linePos = 1, rowPos, charOffset, utf8adjust;
	private int tokenLinePos, tokenCharPos, tokenCharOffset;
	private Object value;
	private Token token;
	private StringBuilder reusableBuffer = new StringBuilder();

	private boolean eof;
	private int index;
	private final Reader reader;
	private final char[] buffer;
	private int bufferLength;

	private static final char[] TRUE = { 'r', 'u', 'e' };
	private static final char[] FALSE = { 'a', 'l', 's', 'e' };
	private static final char[] NULL = { 'u', 'l', 'l' };
	private final boolean utf8;
	private boolean lazyNumbers;

	/**
	 * The tokens available in JSON.
	 */
	private enum Token {
		EOF(false), NULL(true), TRUE(true), FALSE(true), STRING(true), NUMBER(true), COMMA(false), COLON(false), //
		OBJECT_START(true), OBJECT_END(false), ARRAY_START(true), ARRAY_END(false);
		public boolean isValue;

		Token(boolean isValue) {
			this.isValue = isValue;
		}
	}

	/**
	 * A {@link Reader} that reads a UTF8 stream without decoding it for performance.
	 */
	private static final class PseudoUtf8Reader extends Reader {
		private final BufferedInputStream buffered;
		private byte[] buf = new byte[BUFFER_SIZE];

		PseudoUtf8Reader(BufferedInputStream buffered) {
			this.buffered = buffered;
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			int r = buffered.read(buf, off, len);
			for (int i = off; i < off + r; i++)
				cbuf[i] = (char)buf[i];
			return r;
		}

		@Override
		public void close() throws IOException {
		}
	}

	/**
	 * Returns a type-safe parser context for a {@link JsonObject}, {@link JsonArray} or "any" type from which you can
	 * parse a {@link String} or a {@link Reader}.
	 */
	public static final class JsonParserContext<T> {
		private final Class<T> clazz;
		private boolean lazyNumbers;

		JsonParserContext(Class<T> clazz) {
			this.clazz = clazz;
		}

		/**
		 * Parses numbers lazily, allowing us to defer some of the cost of
		 * number construction until later.
		 */
		public JsonParserContext<T> withLazyNumbers() {
			lazyNumbers = true;
			return this;
		}

		/**
		 * Parses the current JSON type from a {@link String}.
		 */
		public T from(String s) throws JsonParserException {
			return new JsonParser(false, lazyNumbers, new StringReader(s)).parse(clazz);
		}

		/**
		 * Parses the current JSON type from a {@link Reader}.
		 */
		public T from(Reader r) throws JsonParserException {
			return new JsonParser(false, lazyNumbers, r).parse(clazz);
		}

		/**
		 * Parses the current JSON type from a {@link URL}.
		 */
		public T from(URL url) throws JsonParserException {
			try {
				InputStream stm = url.openStream();
				try {
					return from(stm);
				} finally {
					stm.close();
				}
			} catch (IOException e) {
				throw new JsonParserException(e, "IOException opening URL", 1, 1, 0);
			}
		}

		/**
		 * Parses the current JSON type from a {@link InputStream}. Detects the encoding from the input stream.
		 */
		public T from(InputStream stm) throws JsonParserException {
			final BufferedInputStream buffered = stm instanceof BufferedInputStream ? (BufferedInputStream)stm
					: new BufferedInputStream(stm);
			buffered.mark(4);

			try {
				Charset charset;
				int[] sig = new int[] { buffered.read(), buffered.read(), buffered.read(), buffered.read() };
				// Encoding detection based on http://www.ietf.org/rfc/rfc4627.txt
				if (sig[0] == 0xEF && sig[1] == 0xBB && sig[2] == 0xBF) {
					buffered.reset();
					buffered.read();
					buffered.read();
					buffered.read();
					return new JsonParser(true, lazyNumbers, new PseudoUtf8Reader(buffered)).parse(clazz);
				} else if (sig[0] == 0x00 && sig[1] == 0x00 && sig[2] == 0xFE && sig[3] == 0xFF) {
					charset = Charset.forName("UTF-32BE");
				} else if (sig[0] == 0xFF && sig[1] == 0xFE && sig[2] == 0x00 && sig[3] == 0x00) {
					charset = Charset.forName("UTF-32LE");
				} else if (sig[0] == 0xFE && sig[1] == 0xFF) {
					charset = Charset.forName("UTF-16BE");
					buffered.reset();
					buffered.read();
					buffered.read();
				} else if (sig[0] == 0xFF && sig[1] == 0xFE) {
					charset = Charset.forName("UTF-16LE");
					buffered.reset();
					buffered.read();
					buffered.read();
				} else if (sig[0] == 0 && sig[1] == 0 && sig[2] == 0 && sig[3] != 0) {
					charset = Charset.forName("UTF-32BE");
					buffered.reset();
				} else if (sig[0] != 0 && sig[1] == 0 && sig[2] == 0 && sig[3] == 0) {
					charset = Charset.forName("UTF-32LE");
					buffered.reset();
				} else if (sig[0] == 0 && sig[1] != 0 && sig[2] == 0 && sig[3] != 0) {
					charset = Charset.forName("UTF-16BE");
					buffered.reset();
				} else if (sig[0] != 0 && sig[1] == 0 && sig[2] != 0 && sig[3] == 0) {
					charset = Charset.forName("UTF-16LE");
					buffered.reset();
				} else {
					buffered.reset();
					return new JsonParser(true, lazyNumbers, new PseudoUtf8Reader(buffered)).parse(clazz);
				}

				return new JsonParser(false, lazyNumbers, new InputStreamReader(buffered, charset)).parse(clazz);
			} catch (IOException e) {
				throw new JsonParserException(e, "IOException while detecting charset", 1, 1, 0);
			}
		}
	}

	JsonParser(boolean utf8, boolean lazyNumbers, Reader reader) throws JsonParserException {
		this.utf8 = utf8;
		this.lazyNumbers = lazyNumbers;
		this.reader = reader;
		this.buffer = new char[BUFFER_SIZE];
		eof = refillBuffer();
	}

	/**
	 * Parses a {@link JsonObject} from a source.
	 * 
	 * <pre>
	 * JsonObject json = {@link JsonParser}.object().from("{\"a\":[true,false], \"b\":1}");
	 * </pre>
	 */
	public static JsonParserContext<JsonObject> object() {
		return new JsonParserContext<JsonObject>(JsonObject.class);
	}

	/**
	 * Parses a {@link JsonArray} from a source.
	 * 
	 * <pre>
	 * JsonArray json = {@link JsonParser}.array().from("[1, {\"a\":[true,false], \"b\":1}]");
	 * </pre>
	 */
	public static JsonParserContext<JsonArray> array() {
		return new JsonParserContext<JsonArray>(JsonArray.class);
	}

	/**
	 * Parses any object from a source. For any valid JSON, returns either a null (for the JSON string 'null'), a
	 * {@link String}, a {@link Number}, a {@link Boolean}, a {@link JsonObject} or a {@link JsonArray}.
	 * 
	 * <pre>
	 * Object json = {@link JsonParser}.any().from("{\"a\":[true,false], \"b\":1}");
	 * Number json = ({@link Number}){@link JsonParser}.any().from("123.456e7");
	 * </pre>
	 */
	public static JsonParserContext<Object> any() {
		return new JsonParserContext<Object>(Object.class);
	}

	/**
	 * Parse a single JSON value from the string, expecting an EOF at the end.
	 */
	<T> T parse(Class<T> clazz) throws JsonParserException {
		advanceToken();
		Object parsed = currentValue();
		if (advanceToken() != Token.EOF)
			throw createParseException(null, "Expected end of input, got " + token, true);
		if (clazz != Object.class && (parsed == null || !clazz.isAssignableFrom(parsed.getClass())))
			throw createParseException(null, "JSON did not contain the correct type, expected " + clazz.getSimpleName()
					+ ".", true);
		return clazz.cast(parsed);
	}

	/**
	 * Starts parsing a JSON value at the current token position.
	 */
	private Object currentValue() throws JsonParserException {
		// Only a value start token should appear when we're in the context of parsing a JSON value
		if (token.isValue)
			return value;
		throw createParseException(null, "Expected JSON value, got " + token, true);
	}

	/**
	 * Consumes a token, first eating up any whitespace ahead of it. Note that number tokens are not necessarily valid
	 * numbers.
	 */
	private Token advanceToken() throws JsonParserException {
		int c = advanceChar();
		while (isWhitespace(c))
			c = advanceChar();

		tokenLinePos = linePos;
		tokenCharPos = index + charOffset - rowPos - utf8adjust;
		tokenCharOffset = charOffset + index;

		switch (c) {
		case -1:
			return token = Token.EOF;
		case '[': // Inlined function to avoid additional stack
			JsonArray list = new JsonArray();
			if (advanceToken() != Token.ARRAY_END)
				while (true) {
					list.add(currentValue());
					if (advanceToken() == Token.ARRAY_END)
						break;
					if (token != Token.COMMA)
						throw createParseException(null, "Expected a comma or end of the array instead of " + token,
								true);
					if (advanceToken() == Token.ARRAY_END)
						throw createParseException(null, "Trailing comma found in array", true);
				}
			value = list;
			return token = Token.ARRAY_START;
		case ']':
			return token = Token.ARRAY_END;
		case ',':
			return token = Token.COMMA;
		case ':':
			return token = Token.COLON;
		case '{': // Inlined function to avoid additional stack
			JsonObject map = new JsonObject();
			if (advanceToken() != Token.OBJECT_END)
				while (true) {
					if (token != Token.STRING)
						throw createParseException(null, "Expected STRING, got " + token, true);
					String key = (String)value;
					if (advanceToken() != Token.COLON)
						throw createParseException(null, "Expected COLON, got " + token, true);
					advanceToken();
					map.put(key, currentValue());
					if (advanceToken() == Token.OBJECT_END)
						break;
					if (token != Token.COMMA)
						throw createParseException(null, "Expected a comma or end of the object instead of " + token,
								true);
					if (advanceToken() == Token.OBJECT_END)
						throw createParseException(null, "Trailing object found in array", true);
				}
			value = map;
			return token = Token.OBJECT_START;
		case '}':
			return token = Token.OBJECT_END;
		case 't':
			consumeKeyword((char)c, TRUE);
			value = Boolean.TRUE;
			return token = Token.TRUE;
		case 'f':
			consumeKeyword((char)c, FALSE);
			value = Boolean.FALSE;
			return token = Token.FALSE;
		case 'n':
			consumeKeyword((char)c, NULL);
			value = null;
			return token = Token.NULL;
		case '\"':
			value = consumeTokenString();
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
			value = consumeTokenNumber((char)c);
			return token = Token.NUMBER;
		case '+':
		case '.':
			throw createParseException(null, "Numbers may not start with '" + (char)c + "'", true);
		default:
		}

		if (isAsciiLetter(c))
			throw createHelpfulException((char)c, null, 0);

		throw createParseException(null, "Unexpected character: " + (char)c, true);
	}

	/**
	 * Expects a given string at the current position.
	 */
	private void consumeKeyword(char first, char[] expected) throws JsonParserException {
		if (ensureBuffer(expected.length) < expected.length) {
			throw createHelpfulException(first, expected, 0);
		}

		for (int i = 0; i < expected.length; i++)
			if (buffer[index++] != expected[i])
				throw createHelpfulException(first, expected, i);

		fixupAfterRawBufferRead();

		// The token should end with something other than an ASCII letter
		if (isAsciiLetter(peekChar()))
			throw createHelpfulException(first, expected, expected.length);
	}

	/**
	 * Steps through to the end of the current number token (a non-digit token).
	 */
	private Number consumeTokenNumber(char c) throws JsonParserException {
		reusableBuffer.setLength(0);
		reusableBuffer.append(c);
		boolean isDouble = false;

		outer: while (true) {
			int n = ensureBuffer(BUFFER_ROOM);
			if (n == 0)
				break outer;

			for (int i = 0; i < n; i++) {
				char next = buffer[index];
				if (!isDigitCharacter(next))
					break outer;

				isDouble |= next == '.' || next == 'e' || next == 'E';
				reusableBuffer.append(next);
				index++;
			}
		}
		fixupAfterRawBufferRead();

		String number = reusableBuffer.toString();

		try {
			if (isDouble) {
				// Special zero handling to match JSON spec. Leading zero is only allowed if next character is . or e
				if (number.charAt(0) == '0') {
					if (number.charAt(1) == '.') {
						if (number.length() == 2)
							throw createParseException(null, "Malformed number: " + number, true);
					} else if (number.charAt(1) != 'e' && number.charAt(1) != 'E')
						throw createParseException(null, "Malformed number: " + number, true);
				}
				if (number.charAt(0) == '-') {
					if (number.charAt(1) == '0') {
						if (number.charAt(2) == '.') {
							if (number.length() == 3)
								throw createParseException(null, "Malformed number: " + number, true);
						} else if (number.charAt(2) != 'e' && number.charAt(2) != 'E')
							throw createParseException(null, "Malformed number: " + number, true);
					} else if (number.charAt(1) == '.') {
						throw createParseException(null, "Malformed number: " + number, true);
					}
				}

				return lazyNumbers ? new JsonLazyNumber(number) : Double.parseDouble(number);
			}

			// Special zero handling to match JSON spec. No leading zeros allowed for integers.
			if (number.charAt(0) == '0') {
				if (number.length() == 1)
					return 0;
				throw createParseException(null, "Malformed number: " + number, true);
			}
			if (number.length() > 1 && number.charAt(0) == '-' && number.charAt(1) == '0') {
				if (number.length() == 2)
					return -0.0;
				throw createParseException(null, "Malformed number: " + number, true);
			}

			if (lazyNumbers)
				return new JsonLazyNumber(number);

			// HACK: Attempt to parse using the approximate best type for this
			boolean firstMinus = number.charAt(0) == '-';
			int length = firstMinus ? number.length() - 1 : number.length();
			if (length < 10 || (length == 10 && number.charAt(firstMinus ? 1 : 0) < '2')) // 2 147 483 647
				return Integer.parseInt(number);
			if (length < 19 || (length == 19 && number.charAt(firstMinus ? 1 : 0) < '9')) // 9 223 372 036 854 775 807
				return Long.parseLong(number);
			return new BigInteger(number);
		} catch (NumberFormatException e) {
			throw createParseException(e, "Malformed number: " + number, true);
		}
	}

	/**
	 * Steps through to the end of the current string token (the unescaped double quote).
	 */
	private String consumeTokenString() throws JsonParserException {
		reusableBuffer.setLength(0);
		outer: while (true) {
			if (ensureBuffer(BUFFER_ROOM) == 0)
				throw createParseException(null, "String was not terminated before end of input", true);
	
			char c = stringChar();
			
			if (utf8 && (c & 0x80) != 0) {
				// If it's a UTF-8 codepoint, we know it won't have special meaning
				consumeTokenStringUtf8Char(c);
				continue outer;
			}

			switch (c) {
			case '\"':
				fixupAfterRawBufferRead();
				return reusableBuffer.toString();
			case '\\':
				char escape = buffer[index++];
				switch (escape) {
				case 'b':
					reusableBuffer.append('\b');
					break;
				case 'f':
					reusableBuffer.append('\f');
					break;
				case 'n':
					reusableBuffer.append('\n');
					break;
				case 'r':
					reusableBuffer.append('\r');
					break;
				case 't':
					reusableBuffer.append('\t');
					break;
				case '"':
				case '/':
				case '\\':
					reusableBuffer.append(escape);
					break;
				case 'u':
					int escaped = 0;

					for (int i = 0; i < 4; i++) {
						escaped <<= 4;
						int digit = buffer[index++];
						if (digit >= '0' && digit <= '9') {
							escaped |= (digit - '0');
						} else if (digit >= 'A' && digit <= 'F') {
							escaped |= (digit - 'A') + 10;
						} else if (digit >= 'a' && digit <= 'f') {
							escaped |= (digit - 'a') + 10;
						} else {
							throw createParseException(null, "Expected unicode hex escape character: " + (char)digit
									+ " (" + digit + ")", false);
						}
					}

					reusableBuffer.append((char)escaped);
					break;
				default:
					throw createParseException(null, "Invalid escape: \\" + escape, false);
				}
				break;
			default:
				reusableBuffer.append(c);
			}
			
			if (index > bufferLength) {
				index = bufferLength; // Reset index to last valid location
				throw createParseException(null,
						"EOF encountered in the middle of a string escape",
						false);
			}
		}
	}

	@SuppressWarnings("fallthrough")
	private void consumeTokenStringUtf8Char(char c) throws JsonParserException {
		ensureBuffer(5);

		// Hand-UTF8-decoding
		switch (c & 0xf0) {
		case 0x80:
		case 0x90:
		case 0xa0:
		case 0xb0:
			throw createParseException(null,
					"Illegal UTF-8 continuation byte: 0x" + Integer.toHexString(c & 0xff), false);
		case 0xc0:
			// Check for illegal C0 and C1 bytes
			if ((c & 0xe) == 0)
				throw createParseException(null, "Illegal UTF-8 byte: 0x" + Integer.toHexString(c & 0xff),
						false);
			// fall-through
		case 0xd0:
			c = (char)((c & 0x1f) << 6 | (buffer[index++] & 0x3f));
			reusableBuffer.append(c);
			utf8adjust++;
			break;
		case 0xe0:
			c = (char)((c & 0x0f) << 12 | (buffer[index++] & 0x3f) << 6 | (buffer[index++] & 0x3f));
			reusableBuffer.append(c);
			utf8adjust += 2;
			break;
		case 0xf0:
			if ((c & 0xf) >= 5)
				throw createParseException(null, "Illegal UTF-8 byte: 0x" + Integer.toHexString(c & 0xff),
						false);

			// Extended char
			switch ((c & 0xc) >> 2) {
			case 0:
			case 1:
				reusableBuffer.appendCodePoint((c & 7) << 18 | (buffer[index++] & 0x3f) << 12
						| (buffer[index++] & 0x3f) << 6 | (buffer[index++] & 0x3f));
				utf8adjust += 3;
				break;
			case 2:
				// TODO: \uFFFD (replacement char)
				int codepoint = (c & 3) << 24 | (buffer[index++] & 0x3f) << 18 | (buffer[index++] & 0x3f) << 12
						| (buffer[index++] & 0x3f) << 6 | (buffer[index++] & 0x3f);
				throw createParseException(null,
						"Unable to represent codepoint 0x" + Integer.toHexString(codepoint)
								+ " in a Java string", false);
			case 3:
				codepoint = (c & 1) << 30 | (buffer[index++] & 0x3f) << 24 | (buffer[index++] & 0x3f) << 18
						| (buffer[index++] & 0x3f) << 12 | (buffer[index++] & 0x3f) << 6
						| (buffer[index++] & 0x3f);
				throw createParseException(null,
						"Unable to represent codepoint 0x" + Integer.toHexString(codepoint)
								+ " in a Java string", false);
			default:
				assert false : "Impossible";
			}
			break;
		default:
			// Regular old byte
			break;
		}
		if (index > bufferLength)
			throw createParseException(null, "UTF-8 codepoint was truncated", false);
	}

	/**
	 * Advances a character, throwing if it is illegal in the context of a JSON string.
	 */
	private char stringChar() throws JsonParserException {
		char c = buffer[index++];
		if (c < 32) {
			// Need to ensure that we position this at the correct location for the error
			if (c == '\n') {
				linePos++;
				rowPos = index + 1 + charOffset;
				utf8adjust = 0;
			}
			throw createParseException(null,
					"Strings may not contain control characters: 0x" + Integer.toString(c, 16), false);
		}
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

	/**
	 * Returns true if EOF.
	 */
	private boolean refillBuffer() throws JsonParserException {
		try {
			int r = reader.read(buffer, 0, buffer.length);
			if (r <= 0) {
				return true;
			}
			charOffset += bufferLength;
			index = 0;
			bufferLength = r;
			return false;
		} catch (IOException e) {
			throw createParseException(e, "IOException", true);
		}
	}

	/**
	 * Peek one char ahead, don't advance, returns {@link Token#EOF} on end of input.
	 */
	private int peekChar() {
		return eof ? -1 : buffer[index];
	}

	/**
	 * Ensures that there is enough room in the buffer to directly access the next N chars via buffer[].
	 */
	private int ensureBuffer(int n) throws JsonParserException {
		// We're good here
		if (bufferLength - n >= index) {
			return n;
		}

		// Nope, we need to read more, but we also have to retain whatever buffer we have
		charOffset += index;
		bufferLength = bufferLength - index;
		System.arraycopy(buffer, index, buffer, 0, bufferLength);
		index = 0;
		try {
			while (buffer.length > bufferLength) {
				int r = reader.read(buffer, bufferLength, buffer.length - bufferLength);
				if (r <= 0) {
					return bufferLength - index;
				}
				bufferLength += r;
				if (bufferLength > n)
					return bufferLength - index;
			}

			// Should be impossible
			assert false : "Unexpected internal error";
			throw new IOException("Unexpected internal error");
		} catch (IOException e) {
			throw createParseException(e, "IOException", true);
		}
	}

	/**
	 * Advance one character ahead, or return {@link Token#EOF} on end of input.
	 */
	private int advanceChar() throws JsonParserException {
		if (eof)
			return -1;

		int c = buffer[index];
		if (c == '\n') {
			linePos++;
			rowPos = index + 1 + charOffset;
			utf8adjust = 0;
		}

		index++;

		// Prepare for next read
		if (index >= bufferLength)
			eof = refillBuffer();

		return c;
	}

	/**
	 * Helper function to fixup eof after reading buffer directly.
	 */
	private void fixupAfterRawBufferRead() throws JsonParserException {
		if (index >= bufferLength)
			eof = refillBuffer();
	}

	/**
	 * Throws a helpful exception based on the current alphanumeric token.
	 */
	private JsonParserException createHelpfulException(char first, char[] expected, int failurePosition)
			throws JsonParserException {
		// Build the first part of the token
		StringBuilder errorToken = new StringBuilder(first
				+ (expected == null ? "" : new String(expected, 0, failurePosition)));

		// Consume the whole pseudo-token to make a better error message
		while (isAsciiLetter(peekChar()) && errorToken.length() < 15)
			errorToken.append((char)advanceChar());

		return createParseException(null, "Unexpected token '" + errorToken + "'"
				+ (expected == null ? "" : ". Did you mean '" + first + new String(expected) + "'?"), true);
	}

	/**
	 * Creates a {@link JsonParserException} and fills it from the current line and char position.
	 */
	private JsonParserException createParseException(Exception e, String message, boolean tokenPos) {
		if (tokenPos)
			return new JsonParserException(e, message + " on line " + tokenLinePos + ", char " + tokenCharPos,
					tokenLinePos, tokenCharPos, tokenCharOffset);
		else {
			int charPos = Math.max(1, index + charOffset - rowPos - utf8adjust);
			return new JsonParserException(e, message + " on line " + linePos + ", char " + charPos, linePos, charPos,
					index + charOffset);
		}
	}
}
