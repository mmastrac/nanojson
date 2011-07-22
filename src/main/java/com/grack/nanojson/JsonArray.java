package com.grack.nanojson;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Extends an {@link ArrayList} with helper methods to determine the underlying JSON type of the list element.
 */
public class JsonArray extends ArrayList<Object> {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an empty {@link JsonArray} with the default capacity.
	 */
	public JsonArray() {
	}

	/**
	 * Creates an empty {@link JsonArray} with the default initial capacity.
	 */
	public JsonArray(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates an empty {@link JsonArray} from the given collection of objects.
	 */
	public JsonArray(Collection<? extends Object> collection) {
		super(collection);
	}

	/**
	 * Returns the underlying object at the given index, or null if it does not exist.
	 */
	public Object get(int key) {
		return key < size() ? super.get(key) : null;
	}

	/**
	 * Returns the {@link JsonArray} at the given index, or null if it does not exist or is the wrong type.
	 */
	public JsonArray getArray(int key) {
		return getArray(key, null);
	}

	/**
	 * Returns the {@link JsonArray} at the given index, or the default if it does not exist or is the wrong type.
	 */
	public JsonArray getArray(int key, JsonArray default_) {
		Object o = get(key);
		if (o instanceof JsonArray)
			return (JsonArray)get(key);
		return default_;
	}

	/**
	 * Returns the {@link Boolean} at the given index, or false if it does not exist or is the wrong type.
	 */
	public boolean getBoolean(int key) {
		return getBoolean(key, false);
	}

	/**
	 * Returns the {@link Boolean} at the given index, or the default if it does not exist or is the wrong type.
	 */
	public boolean getBoolean(int key, Boolean default_) {
		Object o = get(key);
		if (o instanceof Boolean)
			return (Boolean)o;
		return default_;
	}

	/**
	 * Returns the {@link Double} at the given index, or 0.0 if it does not exist or is the wrong type.
	 */
	public double getDouble(int key) {
		return getDouble(key, 0);
	}

	/**
	 * Returns the {@link Double} at the given index, or the default if it does not exist or is the wrong type.
	 */
	public double getDouble(int key, double default_) {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).doubleValue();
		return default_;
	}

	/**
	 * Returns the {@link Integer} at the given index, or 0 if it does not exist or is the wrong type.
	 */
	public int getInt(int key) {
		return getInt(key, 0);
	}

	/**
	 * Returns the {@link Integer} at the given index, or the default if it does not exist or is the wrong type.
	 */
	public int getInt(int key, int default_) {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).intValue();
		return default_;
	}

	/**
	 * Returns the {@link Number} at the given index, or null if it does not exist or is the wrong type.
	 */
	public Number getNumber(int key) {
		return getNumber(key, null);
	}

	/**
	 * Returns the {@link Number} at the given index, or the default if it does not exist or is the wrong type.
	 */
	public Number getNumber(int key, Number default_) {
		Object o = get(key);
		if (o instanceof Number)
			return (Number)o;
		return default_;
	}

	/**
	 * Returns the {@link JsonObject} at the given index, or null if it does not exist or is the wrong type.
	 */
	public JsonObject getObject(int key) {
		return getObject(key, null);
	}

	/**
	 * Returns the {@link JsonObject} at the given index, or the default if it does not exist or is the wrong type.
	 */
	public JsonObject getObject(int key, JsonObject default_) {
		Object o = get(key);
		if (o instanceof JsonObject)
			return (JsonObject)get(key);
		return default_;
	}

	/**
	 * Returns the {@link String} at the given index, or null if it does not exist or is the wrong type.
	 */
	public String getString(int key) {
		return getString(key, null);
	}

	/**
	 * Returns the {@link String} at the given index, or the default if it does not exist or is the wrong type.
	 */
	public String getString(int key, String default_) {
		Object o = get(key);
		if (o instanceof String)
			return (String)get(key);
		return default_;
	}

	/**
	 * Returns true if the array has an element at that index (even if that element is null).
	 */
	public boolean has(int key) {
		return key < size();
	}

	/**
	 * Returns true if the array has a boolean element at that index.
	 */
	public boolean isBoolean(int key) {
		return get(key) instanceof Boolean;
	}

	/**
	 * Returns true if the array has a null element at that index.
	 */
	public boolean isNull(int key) {
		return key < size() && get(key) == null;
	}

	/**
	 * Returns true if the array has a number element at that index.
	 */
	public boolean isNumber(int key) {
		return get(key) instanceof Number;
	}

	/**
	 * Returns true if the array has a string element at that index.
	 */
	public boolean isString(int key) {
		return get(key) instanceof String;
	}

	void emit(String key, JsonEmitter emitter) {
		if (key == null)
			emitter.startArray();
		else
			emitter.startArray(key);

		for (Object o : this) {
			if (o instanceof String)
				emitter.value((String)o);
			else if (o instanceof Integer)
				emitter.value((Integer)o);
			else if (o instanceof Number)
				emitter.value(((Number)o).doubleValue());
			else if (o instanceof Boolean)
				emitter.value((Boolean)o);
			else if (o instanceof JsonArray)
				((JsonArray)o).emit(null, emitter);
			else if (o instanceof JsonObject)
				((JsonObject)o).emit(null, emitter);
			else
				emitter.nul();
		}

		emitter.endArray();
	}
}