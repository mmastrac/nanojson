# nanojson

nanojson is a tiny, compliant JSON parser and writer for Java. 

## Features

  * Strict error checking, reasonable error messages
  * Minimal object allocation
  * One file, barely more than 400 lines
  * Apache licensed

## Parsing example

There is one entry point for parsing: `JsonParser.parse()`. It either returns the parsed object or throws a `JsonParserException`.

    Map<String, Object> map = (Map<String, Object>)JsonParser.parse("{\"abc\":123}");

Errors can be quickly located by using `getLinePosition` and `getCharPosition` on `JsonParserException`:

    {
      "abc":123,
      "def":456,
    }

    com.grack.nanojson.JsonParserException: Trailing comma in object on line 4, char 1

## Writing examples

There are two styles of JSON writing in nanojson, represented by two classes. 

### JsonEmitter

`JsonEmitter` is a simple, stateful JSON emitter that outputs to anything implementing the Java `Appendable` interface. This includes
`StringBuilder`s, `Writer`s, `PrintStream`s and `CharBuffer`s.

`JsonEmitter` has a straightforward interface: `value` methods for writing JSON literals such as numbers and strings and `[start|end][Array|Object]`
for managing array and object contexts. `startArray`, `startObject` and the `value` methods each have two overloads: one with a key prefix for writing
objects and the other for writing raw JSON values or within an array.

	StringBuilder builder = new StringBuilder();
	new JsonEmitter(builder)
     	.startObject()
         	.startArray("a")
             	.value(1)
             	.value(2)
            .endArray()
			.value("b", false)
			.value("c", true)
		.endObject()
	.end();
	assertEquals("{\"a\":true,\"b\":false,\"c\":true}", builder.toString());

If you attempt to write invalid JSON, `JsonEmitter` will throw a runtime `JsonEmitterException`.

### JsonWriter

`JsonWriter` is a more structured JSON writer that uses generics to ensure that you can't write invalid JSON. This class is useful for writing JSON
with well-known structure. 

Caveat: because of its use of generics, it cannot be used to write JSON where the object/array nesting structure is not known at compile time.

	String json = JsonWriter.string()
		.array()
			.value(true)
			.object()
				.array("a")
					.value(true)
					.value(false)
				.end()
				.value("b", "string")
			.end()
		.end()
	.end();
	
	[true,{a:[true,false],b:"string"}]

## Compliance

  * Passes all of the http://www.json.org/JSON_checker/ tests, minus the test that enforces results not be a string and one that tests nesting depth for arrays
  * Passes the sample JSON torture test from http://code.google.com/p/json-test-suite/
  * Passes the tests from the YUI browser JSON test suite
