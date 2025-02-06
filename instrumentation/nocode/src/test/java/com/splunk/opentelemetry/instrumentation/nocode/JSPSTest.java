package com.splunk.opentelemetry.instrumentation.nocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JSPSTest  {
  private static final Map<String, String> thiz = new HashMap<>();
  private static final Set<String> param0 = new HashSet<>();

  static {
    thiz.put("key", "value");
    param0.add("present");
  }

  @Test
  public void testBasicBehavior() {
    String[][] tests = new String[][] {
        // FIXME support bare "this"
        {"this.toString()",                     "{key=value}"},
        {"this.toString().length()",            "11"},
        {"this.get(\"key\")",                   "value"},
        {"this.get(\"key\").substring(1)",      "alue"},
        {"param0.isEmpty()",                    "false"},
    };
    for(String[] test : tests) {
      assertEquals(test[1], JSPS.evaluate(test[0], thiz, new Object[]{param0}), test[0]);
    }
  }

  // FIXME support escaping quotes in string?
  // FIXME test all supported types in type chains

  @Test
  public void testInvalidJspsReturnNull() {
    String[] invalids = new String[] {
        "nosuchvar",
        "nosuchvar.toString()",
        //  "this  .",  // FIXME currently passes
        // "this  .  ", // FIXME currently passes!
        "this.noSuchMethod()",
        "param1.toString()", // out of bounds
        "this.getOrDefault(\"key\", \"multiparamnotsupported\")",
        "this.get(\"noclosequote)",
        "this.get(\"nocloseparan\"",
        "this.noparens",
        "this.noparens.anotherMethod()",
        "this.wrongOrder)(",
        "this.get(NotALiteralParameter);",
        "this.get(12.2)",
        "this.get(this)",
        "this.get(\"NoSuchKey\")", // evals completely but returns null
    };
    for(String invalid : invalids) {
      String answer = JSPS.evaluate(invalid, thiz, new Object[]{param0});
      assertNull(answer, "Expected null for \"" + invalid + "\" but was \"" + answer + "\"");
    }
  }

  @Test
  public void testIntegerLiteralLongerThanOneDigit() {
    Map<String, String> o = new HashMap<>();
    o.put("key", "really long value");
    String jsps = "this.get(\"key\").substring(10)";
    assertEquals("g value", JSPS.evaluate(jsps, o, new Object[0]));
  }

  public static class TakeString {
    public String take(String s) {
      return s;
    }
  }
  public static class TakeObject {
    public String take(Object o) {
      return o.toString();
    }
  }

  public static class TakeBooleanPrimitive {
    public String take(boolean param) {
      return Boolean.toString(param);
    }
  }
  public static class TakeBoolean {
    public String take(Boolean param) {
      return param.toString();
    }
  }

  @Test
  public void testBooleanLiteralParamTypes() {
    TakeBooleanPrimitive b = new TakeBooleanPrimitive();
    TakeBoolean B = new TakeBoolean();
    TakeObject O = new TakeObject();
    assertEquals("true", JSPS.evaluate("this.take(true)", b, new Object[0]));
    assertEquals("false", JSPS.evaluate("this.take(false)", B, new Object[0]));
    assertEquals("true", JSPS.evaluate("this.take(true)", O, new Object[0]));
  }

  @Test
  public void testStringLiteralParamTypes() {
    TakeString S = new TakeString();
    TakeObject O = new TakeObject();
    assertEquals("a", JSPS.evaluate("this.take(\"a\")", S, new Object[0]));
    assertEquals("a", JSPS.evaluate("this.take(\"a\")", O, new Object[0]));
  }

  @Test
  public void testWhitespace() {
    String[] tests = new String[]{
        "this.get(\"key\").substring(1)",
        " this.get(\"key\").substring(1)",
        "this .get(\"key\").substring(1)",
        "this. get(\"key\").substring(1)",
        "this.get (\"key\").substring(1)",
        "this.get( \"key\").substring(1)",
        "this.get(\"key\" ).substring(1)",
        "this.get(\"key\") .substring(1)",
        "this.get(\"key\"). substring(1)",
        "this.get(\"key\").substring (1)",
        "this.get(\"key\").substring( 1)",
        "this.get(\"key\").substring(1 )",
    };
    for(String test : tests) {
      assertEquals("alue", JSPS.evaluate(test, thiz, new Object[]{param0}), test);

    }
  }

}
