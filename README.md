# nanojson

nanojson is a tiny, fast, and compliant JSON parser and writer for Java. 

## Get started

  * Build: `mvn clean compile test jar:jar`
  * Javadocs: `mvn javadoc:javadoc && open target/site/apidocs/index.html`

## Features

  * Fast (faster that Jackson when parsing from memory and in some streaming cases)
  * Strict error checking, reasonable error messages
  * Minimal object allocation
  * Minimal number of source lines: full parser around 600 lines, emitter is barely more than 300
  * Apache licensed
  * Well-documented
  * Well-tested (passes more than 100 tests, including those from YUI and json.org)
  * Multiple input sources: strings, streams, readers and URLs

## Parsing example

There are three entry points for parsing, depending on the type of JSON object you expect to parse: `JsonParser.object().from()`, `JsonParser.array().from()`, and `JsonParser.any().from()`. 
You pass them a `String` or a `Reader` and they will either return the parsed object of a given type or throw a `JsonParserException`.

    JsonObject obj = JsonParser.object().from("{\"abc\":123}");
    JsonArray array = JsonParser.array().from("[1,2,3]");
    Number number = (Number)JsonParser.any().from("123.456e7");

Errors can be quickly located by using `getLinePosition` and `getCharPosition` on `JsonParserException`:

    {
      "abc":123,
      "def":456,
    }

    com.grack.nanojson.JsonParserException: Trailing comma in object on line 4, char 1

## Writing example

`JsonWriter` is a simple, stateful JSON writer that can output to a `String`, or to anything implementing the Java `Appendable` interface. The latter includes 
`StringBuilder`s, `Writer`s, `PrintStream`s and `CharBuffer`s.

`JsonWriter` has a straightforward interface: `value` methods for writing JSON literals such as numbers and strings, and `array` and `object`
for managing array and object contexts. `array`, `object` and the `value` methods each have two overloads: one with a key prefix for writing
objects and the other for writing raw JSON values or within an array.

    StringBuilder builder = new StringBuilder();
    new JsonWriter(builder)
      .object()
         .array("a")
           .value(1)
           .value(2)
         .array()
         .value("b", false)
         .value("c", true)
      .object()
    .end();
	
    -> {"a":[1,2],"b":false,"c":true}

If you attempt to write invalid JSON, `JsonWriter` will throw a runtime `JsonWriterException`.

## Compliance

  * Passes all of the http://www.json.org/JSON_checker/ tests, minus the test that enforces results not be a string and one that tests nesting depth for arrays
  * Passes the sample JSON torture test from http://code.google.com/p/json-test-suite/
  * Passes the tests from the YUI browser JSON test suite
