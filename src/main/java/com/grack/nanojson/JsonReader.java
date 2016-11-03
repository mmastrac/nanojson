package com.grack.nanojson;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;

import com.grack.nanojson.JsonParser.JsonParserContext;

public class JsonReader {
	public enum Type {
		OBJECT,
		ARRAY,
		STRING,
		NUMBER,
		BOOLEAN,
		NULL,
	};

	public JsonReader on(InputStream in) {
		
	}
	
	/**
	 * If the current array or object is finished parsing, returns true.
	 */
	public boolean done() {
		
	}
	
	/**
	 * Returns to the array or object structure above the current one.
	 */
	public boolean pop() {
		
	}

	public Type current() {
		return null;
	}
	
	public void object() {
		
	}


	public String key() {
		return null;
	}

	public void array() {
		
	}
	
	public Object value() {
		
	}
	
	public void nul() {
		
	}
	
	public String string() {
		
	}
	
	public boolean bool() {
		
	}
	
	public int intVal() {
		
	}
	
	public float floatVal() {
		
	}
	
	public double doubleVal() {
		
	}

	
}
