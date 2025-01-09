package com.grack.nanojson;

/**
 * An interface for classes that can be converted into valid Json values.
 */
public interface JsonWritable {
  /**
   * Creates a view of this object as a valid Json Type.
   *
   * @return an instance of Map, Collection, String, Number or Boolean or {@code null}
   */
  Object toJsonValue();
}
