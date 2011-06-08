nanojson is a tiny, compliant JSON parser for Java. 

Features:

  * Strict error checking, reasonable error messages
  * Minimal object allocation
  * One file

Example usage:

    Map<String, Object> map = (Map<String, Object>)JsonParser.parse("{\"abc\":123}");

