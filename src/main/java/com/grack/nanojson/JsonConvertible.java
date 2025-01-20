package com.grack.nanojson;

/**
 * An interface for classes that can be converted into valid JSON values.
 */
public interface JsonConvertible {
  /**
   * Creates a view of this object as a valid JSON Type.
   *
   * @return an instance of Map, Collection, String, Number or Boolean or {@code null}
   */
  Object toJsonValue();
}
