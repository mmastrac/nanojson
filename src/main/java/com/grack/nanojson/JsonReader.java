package com.grack.nanojson;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Streaming reader for JSON documents.
 */
public final class JsonReader {
	private JsonTokener tokener;
	private int token;
	private BitSet states = new BitSet();
	private int stateIndex = 0;
	private boolean inObject;
	private boolean first = true;
	private String key;

	/**
	 * The type of value that the {@link JsonReader} is positioned over.
	 */
	public enum Type {
		/**
		 * An object.
		 */
		OBJECT,
		/**
		 * An array.
		 */
		ARRAY,
		/**
		 * A string.
		 */
		STRING,
		/**
		 * A number.
		 */
		NUMBER,
		/**
		 * A boolean value (true or false).
		 */
		BOOLEAN,
		/**
		 * A null value.
		 */
		NULL,
	};

	/**
	 * Create a {@link JsonReader} from an {@link InputStream}.
	 */
	public static JsonReader from(InputStream in) throws JsonParserException {
		return new JsonReader(new JsonTokener(in));
	}

	/**
	 * Create a {@link JsonReader} from a {@link String}.
	 */
	public static JsonReader from(String s) throws JsonParserException {
		return new JsonReader(new JsonTokener(new StringReader(s)));
	}

	/**
	 * Internal constructor.
	 */
	JsonReader(JsonTokener tokener) throws JsonParserException {
		this.tokener = tokener;
		token = tokener.advanceToToken();
	}

	/**
	 * Returns to the array or object structure above the current one, and
	 * advances to the next key or value.
	 */
	public boolean xpop() throws JsonParserException {
		while (!next()) {
		}
		first = false;
		inObject = states.get(--stateIndex);
		return token != JsonTokener.TOKEN_EOF;
	}

	/**
	 * Returns the current type of the value.
	 */
	public Type current() throws JsonParserException {
		switch (token) {
		case JsonTokener.TOKEN_TRUE:
		case JsonTokener.TOKEN_FALSE:
			return Type.BOOLEAN;
		case JsonTokener.TOKEN_NULL:
			return Type.NULL;
		case JsonTokener.TOKEN_NUMBER:
			return Type.NUMBER;
		case JsonTokener.TOKEN_STRING:
			return Type.STRING;
		case JsonTokener.TOKEN_OBJECT_START:
			return Type.OBJECT;
		case JsonTokener.TOKEN_ARRAY_START:
			return Type.ARRAY;
		default:				
			throw createTokenMismatchException(JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_TRUE, 
					JsonTokener.TOKEN_FALSE, JsonTokener.TOKEN_NUMBER, JsonTokener.TOKEN_STRING);
		}
	}

	/**
	 * Starts reading an object at the current value.
	 */
	public void object() throws JsonParserException {
		if (token != JsonTokener.TOKEN_OBJECT_START)
			throw createTokenMismatchException(JsonTokener.TOKEN_OBJECT_START);
		states.set(stateIndex++, inObject);
		inObject = true;
		first = true;
	}

	/**
	 * Reads the key for the object at the current value. Does not advance to the next value.
	 */
	public String key() throws JsonParserException {
		if (!inObject)
			throw tokener.createParseException(null, "Not reading an object", true);
		return key;
	}

	/**
	 * Starts reading an array at the current value.
	 */
	public void array() throws JsonParserException {
		if (token != JsonTokener.TOKEN_ARRAY_START)
			throw createTokenMismatchException(JsonTokener.TOKEN_ARRAY_START);
		states.set(stateIndex++, inObject);
		inObject = false;
		first = true;
	}

	/**
	 * Returns the current value.
	 */
	public Object value() throws JsonParserException {
		switch (token) {
		case JsonTokener.TOKEN_TRUE:
			return true;
		case JsonTokener.TOKEN_FALSE:
			return false;
		case JsonTokener.TOKEN_NULL:
			return null;
		case JsonTokener.TOKEN_NUMBER:
			return number();
		case JsonTokener.TOKEN_STRING:
			return string();
		default:				
			throw createTokenMismatchException(JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_TRUE, JsonTokener.TOKEN_FALSE,
					JsonTokener.TOKEN_NUMBER, JsonTokener.TOKEN_STRING);
		}
	}

	/**
	 * Parses the current value as a null.
	 */
	public void nul() throws JsonParserException {
		if (token != JsonTokener.TOKEN_NULL)
			throw createTokenMismatchException(JsonTokener.TOKEN_NULL);
	}

	/**
	 * Parses the current value as a string.
	 */
	public String string() throws JsonParserException {
		if (token == JsonTokener.TOKEN_NULL)
			return null;
		if (token != JsonTokener.TOKEN_STRING)
			throw createTokenMismatchException(JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_STRING);
		return tokener.reusableBuffer.toString();
	}

	/**
	 * Parses the current value as a boolean.
	 */
	public boolean bool() throws JsonParserException {
		if (token == JsonTokener.TOKEN_TRUE)
			return true;
		else if (token == JsonTokener.TOKEN_FALSE)
			return false;
		else
			throw createTokenMismatchException(JsonTokener.TOKEN_TRUE, JsonTokener.TOKEN_FALSE);
	}

	/**
	 * Parses the current value as a {@link Number}.
	 */
	public Number number() throws JsonParserException {
		if (token == JsonTokener.TOKEN_NULL)
			return null;
		return new JsonLazyNumber(tokener.reusableBuffer.toString(), tokener.isDouble);
	}

	/**
	 * Parses the current value as a long.
	 */
	public long longVal() throws JsonParserException {
		String s = tokener.reusableBuffer.toString();
		return tokener.isDouble ? (long)Double.parseDouble(s) : Long.parseLong(s);
	}

	/**
	 * Parses the current value as an integer.
	 */
	public int intVal() throws JsonParserException {
		String s = tokener.reusableBuffer.toString();
		return tokener.isDouble ? (int)Double.parseDouble(s) : Integer.parseInt(s);
	}

	/**
	 * Parses the current value as a float.
	 */
	public float floatVal() throws JsonParserException {
		String s = tokener.reusableBuffer.toString();
		return Float.parseFloat(s);
	}

	/**
	 * Parses the current value as a double.
	 */
	public double doubleVal() throws JsonParserException {
		String s = tokener.reusableBuffer.toString();
		return Double.parseDouble(s);
	}

	/**
	 * Advance to the next value in this array or object. If no values remain,
	 * return to the parent array or object.
	 * 
	 * @return true if we still have values to read in this array or object,
	 *         false if we have completed this object (and implicitly moved back
	 *         to the parent array or object)
	 */
	public boolean next() throws JsonParserException {
		if (stateIndex == 0) {
			throw tokener.createParseException(null, "Unabled to call next() at the root", true); 
		}
		
		int n, c = -1;
		int state = (first ? 0 : 1) + (inObject ? 0 : 2);
		first = false;
		boolean value = false;
		
		loop:
		do {
			n = tokener.ensureBuffer(JsonTokener.BUFFER_ROOM);
			for (int i = 0; i < n; i++) {
				c = tokener.advanceCharFast();
				if (tokener.isWhitespace(c))
					continue;
				int ns = -1;
				
				switch (state) {
				case 0: // object first
					if (c == '}') {
						break loop;
					}
					if (c == '"') {
						tokener.consumeTokenString();
						key = tokener.reusableBuffer.toString();
						state = 5;
						continue loop;
					}
					break;
				case 1: // object non-first
					if (c == '}') {
						break loop;
					}
					if (c == ',') {
						ns = 4;
						break;
					}
					break;
				case 2: // array first
					if (c == ']') {
						break loop;
					}
					value = true;
					break loop;
				case 3: // array non-first
					if (c == ']') {
						break loop;
					}
					if (c == ',') {
						ns = 6;
						break;
					}
					value = true;
					break loop;
				case 4: // object key
					if (c == '"') {
						tokener.consumeTokenString();
						key = tokener.reusableBuffer.toString();
						state = 5;
						continue loop;
					}
					break;
				case 5: // object colon
					if (c == ':') {
						ns = 6;
						break;
					}
					break;
				case 6: // array value
					value = true;
					break loop;
				}
				state = ns;
			}
		} while (n > 0);
		
		if (value) {
			switch (c) {
			case '[':
				token = JsonTokener.TOKEN_ARRAY_START;
				break;
			case '{':
				token = JsonTokener.TOKEN_OBJECT_START;
				break;
			case 't':
				tokener.consumeKeyword((char) c, JsonTokener.TRUE);
				token = JsonTokener.TOKEN_TRUE;
				break;
			case 'f':
				tokener.consumeKeyword((char) c, JsonTokener.FALSE);
				token = JsonTokener.TOKEN_FALSE;
				break;
			case 'n':
				tokener.consumeKeyword((char) c, JsonTokener.NULL);
				token = JsonTokener.TOKEN_NULL;
				break;
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
				tokener.consumeTokenNumber((char)c);
				token = JsonTokener.TOKEN_NUMBER;
				break;
			case '"':
				tokener.consumeTokenString();
				token = JsonTokener.TOKEN_STRING;
				break;
			}
		} else {
			inObject = states.get(--stateIndex);
		}
		
		tokener.fixupAfterRawBufferRead();
		
		return value;
	}
	
	private JsonParserException createTokenMismatchException(int... t) {
		return tokener.createParseException(null, "mismatch (expected " + Arrays.toString(t) + ", was " + token + ")",
				true);
	}
}
