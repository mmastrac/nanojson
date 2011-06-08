nanojson is a tiny, compliant JSON parser for Java. 

Features:

  * Strict error checking, reasonable error messages
  * Minimal object allocation
  * One file

Example usage:

    Map<String, Object> map = (Map<String, Object>)JsonParser.parse("{\"abc\":123}");

Compliance:

  * Passes all of the http://www.json.org/JSON_checker/ tests, minus the test that enforces results not be a string and one that tests nesting depth for arrays
  * Passes the sample JSON torture test from http://code.google.com/p/json-test-suite/
  * Passes the tests from the YUI browser JSON test suite