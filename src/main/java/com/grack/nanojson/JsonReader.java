package com.grack.nanojson;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.BitSet;

import com.grack.nanojson.JsonTokener.Token;

/**
 * Streaming reader for JSON documents.
 */
public final class JsonReader {
	private JsonTokener tokener;
	private Token token;
	private BitSet states = new BitSet();
	private int stateIndex = 0;
	private boolean inObject;
	private boolean first = true;
	private String key;
	private JsonLazyNumber reusableNumber = new JsonLazyNumber(null);

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
		advance();
	}

	/**
	 * If the current array or object is finished parsing, returns true.
	 */
	public boolean done() throws JsonParserException {
		if (token == Token.ARRAY_END || token == Token.OBJECT_END)
			return true;
		return false;
	}

	/**
	 * Returns to the array or object structure above the current one, and
	 * advances to the next key or value.
	 * 
	 * If the object or array is not yet {@link #done()}, the remaining tokens
	 * are discarded until it is.
	 */
	public boolean pop() throws JsonParserException {
		while (!done()) {
			advance();
		}
		first = false;
		inObject = states.get(--stateIndex);
		advance();
		return token != Token.EOF;
	}

	/**
	 * Returns the current type of the value.
	 */
	public Type current() throws JsonParserException {
		switch (token) {
		case TRUE:
		case FALSE:
			return Type.BOOLEAN;
		case NULL:
			return Type.NULL;
		case NUMBER:
			return Type.NUMBER;
		case STRING:
			return Type.STRING;
		case OBJECT_START:
			return Type.OBJECT;
		case ARRAY_START:
			return Type.ARRAY;
		default:				
			throw createTokenMismatchException(Token.NULL, Token.TRUE, 
					Token.FALSE, Token.NUMBER, Token.STRING);
		}
	}

	/**
	 * Starts reading an object at the current value.
	 */
	public void object() throws JsonParserException {
		if (token != Token.OBJECT_START)
			throw createTokenMismatchException(Token.OBJECT_START);
		states.set(stateIndex++, inObject);
		inObject = true;
		first = true;
		advance();
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
		if (token != Token.ARRAY_START)
			throw createTokenMismatchException(Token.ARRAY_START);
		states.set(stateIndex++, inObject);
		inObject = false;
		first = true;
		advance();
	}

	/**
	 * Returns the current value and advances to the next key or value.
	 */
	public Object value() throws JsonParserException {
		switch (token) {
		case TRUE:
			return true;
		case FALSE:
			return false;
		case NULL:
			return null;
		case NUMBER:
			return number();
		case STRING:
			return string();
		default:				
			throw createTokenMismatchException(Token.NULL, Token.TRUE, 
					Token.FALSE, Token.NUMBER, Token.STRING);
		}
	}

	/**
	 * Parses the current value as a null and advances to the next key or value.
	 */
	public void nul() throws JsonParserException {
		if (token != Token.NULL)
			throw createTokenMismatchException(Token.NULL);
		advance();
	}

	/**
	 * Parses the current value as a string and advances to the next key or value.
	 */
	public String string() throws JsonParserException {
		if (token == Token.NULL) {
			nul();
			return null;
		}
		if (token != Token.STRING)
			throw createTokenMismatchException(Token.NULL, Token.STRING);
		String s = tokener.consumeTokenString();
		advance();
		return s;
	}

	/**
	 * Parses the current value as a boolean and advances to the next key or value.
	 */
	public boolean bool() throws JsonParserException {
		boolean b;
		if (token == Token.TRUE)
			b = true;
		else if (token == Token.FALSE)
			b = false;
		else
			throw createTokenMismatchException(Token.TRUE, Token.FALSE);
		advance();
		return b;
	}

	/**
	 * Parses the current value as a {@link Number} and advances to the next key or value.
	 */
	public Number number() throws JsonParserException {
		if (token == Token.NULL) {
			nul();
			return null;
		}
		return number(new JsonLazyNumber(null));
	}

	private Number number(JsonLazyNumber n) throws JsonParserException {
		if (token != Token.NUMBER)
			throw createTokenMismatchException(Token.NULL, Token.NUMBER);
		Number number = tokener.consumeTokenNumber(n);
		advance();
		return number;
	}

	/**
	 * Parses the current value as an integer and advances to the next key or value.
	 */
	public int intVal() throws JsonParserException {
		return number(reusableNumber).intValue();
	}

	/**
	 * Parses the current value as a float and advances to the next key or value.
	 */
	public float floatVal() throws JsonParserException {
		return number(reusableNumber).floatValue();
	}

	/**
	 * Parses the current value as a double and advances to the next key or value.
	 */
	public double doubleVal() throws JsonParserException {
		return number(reusableNumber).doubleValue();
	}

	/**
	 * In an object, we read (and hold on to) the key and leave the
	 * {@link JsonTokener} pointed at the value.
	 * 
	 * In an array, we skip over {@link Token#COMMA} tokens and leave it pointed
	 * at the value.
	 */
	private void advance() throws JsonParserException {
		token = tokener.advanceToToken();

		if (stateIndex == 0) {
			if (first) {
				if (!token.isValue)
					throw createTokenMismatchException(Token.TRUE, Token.FALSE, Token.NULL,
							Token.NUMBER, Token.STRING, Token.ARRAY_START, Token.OBJECT_START);
				return;
			}
			
			if (token != Token.EOF)
				throw createTokenMismatchException(Token.EOF);
			return;
		}
		
		if (inObject) {
			if (token == Token.OBJECT_END)
				return;
			
			if (!first) {
				if (token != Token.COMMA)
					throw createTokenMismatchException(Token.COMMA, Token.OBJECT_END);
				token = tokener.advanceToToken();
			}

			if (token != Token.STRING)
				throw createTokenMismatchException(Token.STRING);
			key = tokener.consumeTokenString();
			if ((token = tokener.advanceToToken()) != Token.COLON)
				throw createTokenMismatchException(Token.COLON);
			token = tokener.advanceToToken();
		} else {
			if (token == Token.ARRAY_END)
				return;
			if (!first) {
				if (token != Token.COMMA)
					throw createTokenMismatchException(Token.COMMA, Token.ARRAY_END);
				token = tokener.advanceToToken();
			}
		}

		if (!token.isValue)
			throw createTokenMismatchException(Token.TRUE, Token.FALSE, Token.NULL,
					Token.NUMBER, Token.STRING, Token.ARRAY_START, Token.OBJECT_START);

		first = false;
	}
	
	private JsonParserException createTokenMismatchException(Token... t) {
		return tokener.createParseException(null, "mismatch (expected " + Arrays.toString(t) + ", was " + token + ")",
				true);
	}
}
