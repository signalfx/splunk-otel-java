package com.splunk.opentelemetry.instrumentation.nocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// FIXME need tests for the instrumentation too!
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
        // Might be nice to support a bare "param0" or "this" but as a workaround can always use "this.toString()"
        {"this.toString()",                     "{key=value}"},
        {"this.toString().length()",            "11"},
        {"this.get(\"key\")",                   "value"},
        {"this.get(\"key\").substring(1)",      "alue"},
        {"param0.isEmpty()",                    "false"},
        {"param0.contains(\"present\")",        "true"},
    };
    for(String[] test : tests) {
      assertEquals(test[1], JSPS.evaluate(test[0], thiz, new Object[]{param0}), test[0]);
    }
  }

  @Test
  public void testInvalidJspsReturnNull() {
    String[] invalids = new String[] {
        "nosuchvar",
        "nosuchvar.toString()",
        "this  .",
        "this  .  ",
        "this.noSuchMethod()",
        "toString()",
        "this.toString()extrastuffatend",
        "this.toString()toString()",
        "param1.toString()", // out of bounds
        "param999.toString()",
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
        "param1.toString()", // no such param
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
  public static class TakeIntegerPrimitive {
    public String take(int param) {
      return Integer.toString(param);
    }
  }
  public static class TakeInteger {
    public String take(Integer param) {
      return param.toString();
    }
  }
  public static class TakeLongPrimitize {
    public String take(long param) {
      return Long.toString(param);
    }
  }
  public static class TakeLong {
    public String take(Long param) {
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
  public void testIntegerLiteralParamTypes() {
    TakeIntegerPrimitive i = new TakeIntegerPrimitive();
    TakeInteger I = new TakeInteger();
    TakeLongPrimitize l = new TakeLongPrimitize();
    TakeLong L = new TakeLong();
    TakeObject O = new TakeObject();
    assertEquals("13", JSPS.evaluate("this.take(13)", i, new Object[0]));
    assertEquals("13", JSPS.evaluate("this.take(13)", I, new Object[0]));
    assertEquals("13", JSPS.evaluate("this.take(13)", l, new Object[0]));
    assertEquals("13", JSPS.evaluate("this.take(13)", L, new Object[0]));
    assertEquals("13", JSPS.evaluate("this.take(13)", O, new Object[0]));
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

  public void testManyParams() {
    Object[] params = new Object[13];
    Arrays.fill(params, new Object());
    assertEquals("java.lang.Object", JSPS.evaluate("param12.getClass().getName()", new Object(), params));
  }


}
