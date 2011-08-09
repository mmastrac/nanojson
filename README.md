# nanojson

nanojson is a tiny, fast, and compliant JSON parser and writer for Java. 

## Get started

  * Build: `mvn clean compile test jar:jar`
  * Javadocs: `mvn javadoc:javadoc && open target/site/apidocs/index.html`

## Features

### Fast

  * Minimal object allocation
  * Fastest Java JSON in many cases: faster that Jackson when parsing from memory and in some streaming cases:

    .    benchmark    ms linear runtime
    NanojsonString  2.70 *****
    NanojsonStream  4.08 ********
     JacksonString  3.00 ******
     JacksonStream  3.64 *******
        GsonString 11.79 *************************
        GsonStream 13.82 ******************************
        
### Tiny

  * Minimal number of source lines: full parser around 600 lines, writer is barely more than 400
  * Tiny jar: less than 20kB

### Robust

  * Strict error checking, reasonable error messages
  * Well-tested: code-coverage-directed tests, passes more than 100 tests, including those from YUI and json.org

### Easy to use

  * Well-documented
  * Apache licensed

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
`StringBuilder`, `Writer`, `PrintStream`, and `CharBuffer`.

`JsonWriter` has a straightforward interface: `value` methods for writing JSON literals such as numbers and strings, and `array` and `object`
for managing array and object contexts. `array`, `object` and the `value` methods each have two overloads: one with a key prefix for writing
objects and the other for writing raw JSON values or within an array.

    String json = JsonWriter.string()
      .object()
         .array("a")
           .value(1)
           .value(2)
         .end()
         .value("b", false)
         .value("c", true)
      .end()
    .close();
	
    -> {"a":[1,2],"b":false,"c":true}

You can also quickly convert a `JsonArray`, a `JsonObject`, or any JSON primitive to a string:

    JsonArray array = ...
    String json = JsonWriter.string(array);

If you attempt to write invalid JSON, `JsonWriter` will throw a runtime `JsonWriterException`.

## Compliance

  * Passes all of the http://www.json.org/JSON_checker/ tests, minus the test that enforces results not be a string and one that tests nesting depth for arrays
  * Passes the sample JSON torture test from http://code.google.com/p/json-test-suite/
  * Passes the tests from the YUI browser JSON test suite
