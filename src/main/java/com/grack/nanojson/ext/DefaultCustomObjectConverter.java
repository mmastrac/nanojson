package com.grack.nanojson.ext;

import com.grack.nanojson.JsonWriterException;

public class DefaultCustomObjectConverter implements CustomObjectConverter {

	@Override
	public Object convert(Object object) {
		throw new JsonWriterException("Unable to handle type: "
				+ object.getClass());
	}

}
