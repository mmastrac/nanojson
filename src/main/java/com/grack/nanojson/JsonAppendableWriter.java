package com.grack.nanojson;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;

//@formatter:off
/**
 * Light-weight JSON emitter with state checking. Emits JSON to a {@link Appendable}.
 * 
 * Create this class with {@link JsonWriter#on(Appendable)} or {@link JsonWriter#on(OutputStream)}.
 * 
 *  <pre>
 * OutputStream out = ...;
 * JsonEmitter.on(out)
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
public final class JsonAppendableWriter extends JsonWriterBase<JsonAppendableWriter> {
	JsonAppendableWriter(Appendable appendable) {
		super(appendable);
	}

	public void close() throws IOException {
		super.closeInternal();
		if (appendable instanceof Flushable)
			((Flushable)appendable).flush();
	}
}
