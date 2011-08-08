package com.grack.nanojson;

//@formatter:off
/**
 * Light-weight JSON emitter with state checking. Emits JSON to a String.
 * 
 * Create this class using {@link JsonWriter#string()}.
 * 
 * <pre>
 * String json = JsonEmitter.string()
 *     .object()
 *         .array("a")
 *             .value(1)
 *             .value(2)
 *         .end()
 *         .value("b", false)
 *         .value("c", true)
 *     .end()
 * .close();
 * </pre>
 */
//@formatter:on
public final class JsonStringWriter extends JsonWriterBase<JsonStringWriter> {
	JsonStringWriter() {
		super(new StringBuilder());
	}

	/**
	 * Completes this JSON writing session and returns the internal representation as a {@link String}.
	 */
	public String close() {
		super.closeInternal();
		return appendable.toString();
	}
}
