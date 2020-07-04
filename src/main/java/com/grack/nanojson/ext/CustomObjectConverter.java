package com.grack.nanojson.ext;

/**
 * Custom type to standard type converter
 */
public interface CustomObjectConverter {
	/**
	 * convert custom object to nanojson known types
	 * @param object
	 * @return
	 */
	Object convert(Object object);
}
