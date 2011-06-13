package com.grack.nanojson;

import java.io.IOException;

/**
 * {@link JsonWriter} is a more structured JSON writer that uses generics to
 * ensure that you can't write invalid JSON. This class is useful for writing
 * JSON with well-known structure.
 * 
 * Caveat: because of its use of generics, it cannot be used to write JSON where
 * the object/array nesting structure is not known at compile time.
 * 
 * This class wraps a {@link JsonEmitter} internally.
 */
public class JsonWriter {
	private interface Context {
	}

	private interface RootContext extends Context, Appendable {
	}

	public interface RootStringContext extends RootContext {
		String end();
	}

	public interface RootValueContext<T extends RootContext> extends Context {
		/**
		 * Writes the only value for this writer context.
		 */
		T value(String s);

		/**
		 * Writes the only value for this writer context.
		 */
		T value(int i);

		/**
		 * Writes the only value for this writer context.
		 */
		T value(boolean b);

		/**
		 * Writes the only value for this writer context.
		 */
		T value(double d);

		/**
		 * Starts writing the root array value for this writer context.
		 */
		ArrayContext<T> array();

		/**
		 * Starts writing the root object value for this writer context.
		 */
		ObjectContext<T> object();
	}

	public interface ArrayContext<T extends Context> extends Context {
		ArrayContext<T> value(String s);

		ArrayContext<T> value(int i);

		ArrayContext<T> value(boolean b);

		ArrayContext<T> value(double d);

		ArrayContext<ArrayContext<T>> array();

		ObjectContext<ArrayContext<T>> object();

		T end();
	}

	public interface ObjectContext<T extends Context> extends Context {
		ObjectContext<T> value(String key, String s);

		ObjectContext<T> value(String key, int i);

		ObjectContext<T> value(String key, boolean b);

		ObjectContext<T> value(String key, double d);

		ArrayContext<ObjectContext<T>> array(String key);

		ObjectContext<ObjectContext<T>> object(String key);

		T end();
	}

	private static class RootStringContextImpl implements RootStringContext {
		private StringBuilder builder = new StringBuilder();

		private RootStringContextImpl() {
		}

		public String end() {
			return builder.toString();
		}

		@Override
		public Appendable append(CharSequence csq) throws IOException {
			return builder.append(csq);
		}

		@Override
		public Appendable append(char c) throws IOException {
			return builder.append(c);
		}

		@Override
		public Appendable append(CharSequence csq, int start, int end)
				throws IOException {
			return builder.append(csq, start, end);
		}
	}

	/**
	 * Implementation for the various emit methods. Generics handle the
	 * specialization of this class into {@link RootValueContext},
	 * {@link ObjectContext} and {@link ArrayContext}.
	 */
	private abstract static class ContextImpl<T extends Context> implements
			Context {
		protected final JsonEmitter emitter;
		private final T t;

		/**
		 * Workaround for inability to pass "this" to a superconstructor.
		 */
		@SuppressWarnings("unchecked")
		public ContextImpl(JsonEmitter emitter) {
			this.t = (T) this;
			this.emitter = emitter;
		}

		public ContextImpl(T t, JsonEmitter emitter) {
			this.t = t;
			this.emitter = emitter;
		}

		public T value(String s) {
			emitter.value(s);
			return t;
		}

		public T value(int i) {
			emitter.value(i);
			return t;
		}

		public T value(boolean b) {
			emitter.value(b);
			return t;
		}

		public T value(double d) {
			emitter.value(d);
			return t;
		}

		public T value(String key, String s) {
			emitter.value(key, s);
			return t;
		}

		public T value(String key, int i) {
			emitter.value(key, i);
			return t;
		}

		public T value(String key, boolean b) {
			emitter.value(key, b);
			return t;
		}

		public T value(String key, double d) {
			emitter.value(key, d);
			return t;
		}

		public ArrayContext<T> array() {
			emitter.startArray();
			return new ArrayContextImpl<T>(t, emitter);
		}

		public ObjectContext<T> object() {
			emitter.startObject();
			return new ObjectContextImpl<T>(t, emitter);
		}

		public ArrayContext<T> array(String key) {
			emitter.startArray(key);
			return new ArrayContextImpl<T>(t, emitter);
		}

		public ObjectContext<T> object(String key) {
			emitter.startObject(key);
			return new ObjectContextImpl<T>(t, emitter);
		}
	}

	private static class RootValueContextImpl<T extends RootContext> extends
			ContextImpl<T> implements RootValueContext<T> {
		public RootValueContextImpl(T t) {
			super(t, new JsonEmitter(t));
		}
	}

	private static class ArrayContextImpl<T extends Context> extends
			ContextImpl<ArrayContext<T>> implements ArrayContext<T> {
		private final T t;

		public ArrayContextImpl(T t, JsonEmitter emitter) {
			super(emitter);
			this.t = t;
		}

		@Override
		public T end() {
			emitter.endArray();
			return t;
		}
	}

	private static class ObjectContextImpl<T extends Context> extends
			ContextImpl<ObjectContext<T>> implements ObjectContext<T> {
		private final T t;

		public ObjectContextImpl(T t, JsonEmitter emitter) {
			super(emitter);
			this.t = t;
		}

		@Override
		public T end() {
			emitter.endObject();
			return t;
		}
	}

	/**
	 * Starts writing a {@link String}.
	 */
	public static RootValueContext<RootStringContext> string() {
		return new RootValueContextImpl<RootStringContext>(
				new RootStringContextImpl());
	}
}
