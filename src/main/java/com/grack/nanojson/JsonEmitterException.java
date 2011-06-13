package com.grack.nanojson;

public class JsonEmitterException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public JsonEmitterException(String message) {
		super(message);
	}

	public JsonEmitterException(Throwable t) {
		super(t);
	}
}