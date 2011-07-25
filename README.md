# nanojson

nanojson is a tiny, compliant JSON parser and writer for Java. 

## Get started

  * Build: `mvn clean compile test jar:jar`
  * Javadocs: `mvn javadoc:javadoc`, `open target/site/apidocs/index.html`

## Features

  * Fast (faster that Jackson when parsing from memory and in some streaming cases)
  * Strict error checking, reasonable error messages
  * Minimal object allocation
  * Minimal number of source lines: parser is under 500 lines, emitter is barely more than 300
  * Apache licensed
  * Well-documented

## Parsing example

There are three entry points for parsing: `JsonParser.parse()`, `JsonParser.parseObject`, and `JsonParser.parseArray`. They either return the parsed object or throw a `JsonParserException`.

    JsonObject obj = JsonParser.parseObject("{\"abc\":123}");
    JsonArray array = JsonParser.parseArray("[1,2,3]");

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
	
    -> {"a":[1,2],"b":false,"c":true}

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
	
	-> [true,{a:[true,false],b:"string"}]

You can also emit `JsonObject` and `JsonArray` objects into the tree using the object(...) and array(...) methods like so:

    JsonObject obj = JsonParser.parseObject("{\"a\":1}");
    JsonArray array = JsonParser.parseArray("[1,2,3]");
	String json = JsonWriter.string()
		.array()
			.object(obj)
			.array(array)
		.end()
	.end();
	
	-> [{a:1},[1,2,3]]

## Compliance

  * Passes all of the http://www.json.org/JSON_checker/ tests, minus the test that enforces results not be a string and one that tests nesting depth for arrays
  * Passes the sample JSON torture test from http://code.google.com/p/json-test-suite/
  * Passes the tests from the YUI browser JSON test suite
