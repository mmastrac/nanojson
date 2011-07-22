package com.grack.nanojson;

import java.util.HashMap;
import java.util.Map;

/**
 * Extends a {@link HashMap} with helper methods to determine the underlying JSON type of the map element.
 */
public class JsonObject extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an empty {@link JsonObject} with the default capacity.
	 */
	public JsonObject() {
	}

	/**
	 * Creates a {@link JsonObject} from an existing {@link Map}.
	 */
	public JsonObject(Map<? extends String, ? extends Object> map) {
		super(map);
	}

	/**
	 * Creates a {@link JsonObject} with the given initial capacity.
	 */
	public JsonObject(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a {@link JsonObject} with the given initial capacity and load factor.
	 */
	public JsonObject(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Returns the {@link JsonArray} at the given key, or null if it does not exist or is the wrong type.
	 */
	public JsonArray getArray(String key) {
		return getArray(key, null);
	}

	/**
	 * Returns the {@link JsonArray} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public JsonArray getArray(String key, JsonArray default_) {
		Object o = get(key);
		if (o instanceof JsonArray)
			return (JsonArray)get(key);
		return default_;
	}

	/**
	 * Returns the {@link Boolean} at the given key, or false if it does not exist or is the wrong type.
	 */
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	/**
	 * Returns the {@link Boolean} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public boolean getBoolean(String key, Boolean default_) {
		Object o = get(key);
		if (o instanceof Boolean)
			return (Boolean)o;
		return default_;
	}

	/**
	 * Returns the {@link Double} at the given key, or 0.0 if it does not exist or is the wrong type.
	 */
	public double getDouble(String key) {
		return getDouble(key, 0);
	}

	/**
	 * Returns the {@link Double} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public double getDouble(String key, double default_) {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).doubleValue();
		return default_;
	}

	/**
	 * Returns the {@link Integer} at the given key, or 0 if it does not exist or is the wrong type.
	 */
	public int getInt(String key) {
		return getInt(key, 0);
	}

	/**
	 * Returns the {@link Integer} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public int getInt(String key, int default_) {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).intValue();
		return default_;
	}

	/**
	 * Returns the {@link Number} at the given key, or null if it does not exist or is the wrong type.
	 */
	public Number getNumber(String key) {
		return getNumber(key, null);
	}

	/**
	 * Returns the {@link Number} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public Number getNumber(String key, Number default_) {
		Object o = get(key);
		if (o instanceof Number)
			return (Number)o;
		return default_;
	}

	/**
	 * Returns the {@link JsonObject} at the given key, or null if it does not exist or is the wrong type.
	 */
	public JsonObject getObject(String key) {
		return getObject(key, null);
	}

	/**
	 * Returns the {@link JsonObject} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public JsonObject getObject(String key, JsonObject default_) {
		Object o = get(key);
		if (o instanceof JsonObject)
			return (JsonObject)get(key);
		return default_;
	}

	/**
	 * Returns the {@link String} at the given key, or null if it does not exist or is the wrong type.
	 */
	public String getString(String key) {
		return getString(key, null);
	}

	/**
	 * Returns the {@link String} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public String getString(String key, String default_) {
		Object o = get(key);
		if (o instanceof String)
			return (String)get(key);
		return default_;
	}

	/**
	 * Returns true if the object has an element at that key (even if that element is null).
	 */
	public boolean has(String key) {
		return super.containsKey(key);
	}

	/**
	 * Returns true if the object has a boolean element at that key.
	 */
	public boolean isBoolean(String key) {
		return get(key) instanceof Boolean;
	}

	/**
	 * Returns true if the object has a null element at that key.
	 */
	public boolean isNull(String key) {
		return super.containsKey(key) && get(key) == null;
	}

	/**
	 * Returns true if the object has a number element at that key.
	 */
	public boolean isNumber(String key) {
		return get(key) instanceof Number;
	}

	/**
	 * Returns true if the object has a string element at that key.
	 */
	public boolean isString(String key) {
		return get(key) instanceof String;
	}

	void emit(String key, JsonEmitter emitter) {
		if (key == null)
			emitter.startObject();
		else
			emitter.startObject(key);

		for (Map.Entry<String, Object> entry : this.entrySet()) {
			Object o = entry.getValue();
			String k = entry.getKey();
			if (o instanceof String)
				emitter.value(k, (String)o);
			else if (o instanceof Integer)
				emitter.value(k, (Integer)o);
			else if (o instanceof Number)
				emitter.value(k, ((Number)o).doubleValue());
			else if (o instanceof Boolean)
				emitter.value(k, (Boolean)o);
			else if (o instanceof JsonArray)
				((JsonArray)o).emit(k, emitter);
			else if (o instanceof JsonObject)
				((JsonObject)o).emit(k, emitter);
			else
				emitter.nul(k);
		}

		emitter.endObject();
	}
}