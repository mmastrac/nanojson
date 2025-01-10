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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Extends a {@link LinkedHashMap} with helper methods to determine the underlying JSON type of the map element.
 */
public class JsonObject extends LinkedHashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an empty {@link JsonObject} with the default capacity.
	 */
	public JsonObject() {
	}

	/**
	 * Creates a {@link JsonObject} from an existing {@link Map}.
	 */
	public JsonObject(Map<? extends String, ?> map) {
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
	 * Creates a {@link JsonBuilder} for a {@link JsonObject}.
	 */
	public static JsonBuilder<JsonObject> builder() {
		return new JsonBuilder<>(new JsonObject());
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
			return (JsonArray) o;
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
	 * Returns the {@link Float} at the given key, or 0.0f if it does not exist or is the wrong type.
	 */
	public float getFloat(String key) {
		return getFloat(key, 0);
	}

	/**
	 * Returns the {@link Float} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public float getFloat(String key, float default_) {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).floatValue();
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
	 * Returns the {@link Long} at the given key, or 0 if it does not exist or is the wrong type.
	 */
	public long getLong(String key) {
		return getLong(key, 0);
	}

	/**
	 * Returns the {@link Long} at the given key, or the default if it does not exist or is the wrong type.
	 */
	public long getLong(String key, long default_) {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).longValue();
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
			return (JsonObject) o;
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
			return (String) o;
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

	/**
	 * Performs the given action for each entry in this map where the value represents a JsonObject
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachObject(BiConsumer<String, JsonObject> consumer) {
		forEach((key, value) -> {
			if (value instanceof JsonObject) consumer.accept(key, (JsonObject) value);
		});
	}

	/**
	 * Performs the given action for each entry in this map where the value represents a JsonArray
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachArray(BiConsumer<String, JsonArray> consumer) {
		forEach((key, value) -> {
			if (value instanceof JsonArray) consumer.accept(key, (JsonArray) value);
		});
	}

	/**
	 * Performs the given action for each entry in this map where the value represents a Boolean
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachBoolean(BiConsumer<String, Boolean> consumer) {
		forEach((key, value) -> {
			if (value instanceof Boolean) consumer.accept(key, (Boolean) value);
		});
	}

	/**
	 * Performs the given action for each entry in this map where the value represents a Number
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachNumber(BiConsumer<String, Number> consumer) {
		forEach((key, value) -> {
			if (value instanceof Number) consumer.accept(key, (Number) value);
		});
	}

	/**
	 * Performs the given action for each entry in this map where the value represents an Int
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachInt(BiConsumer<String, Integer> consumer) {
		forEach((key, value) -> {
			if (value instanceof Number) consumer.accept(key, ((Number) value).intValue());
		});
	}

	/**
	 * Performs the given action for each entry in this map where the value represents a Long
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachLong(BiConsumer<String, Long> consumer) {
		forEach((key, value) -> {
			if (value instanceof Number) consumer.accept(key, ((Number) value).longValue());
		});
	}

	/**
	 * Performs the given action for each entry in this map where the value represents a Float
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachFloat(BiConsumer<String, Float> consumer) {
		forEach((key, value) -> {
			if (value instanceof Number) consumer.accept(key, ((Number) value).floatValue());
		});
	}

	/**
	 * Performs the given action for each entry in this map where the value represents a Double
	 * until all entries have been processed or the action throws an exception.
	 * Actions are performed in the order of entry set iteration.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param consumer the action to perform
	 */
	public void forEachDouble(BiConsumer<String, Double> consumer) {
		forEach((key, value) -> {
			if (value instanceof Number) consumer.accept(key, ((Number) value).doubleValue());
		});
	}
}
