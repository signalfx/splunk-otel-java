package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ExtractorTest {
  private static final Map<String, String> thiz = new HashMap<>();
  private static final Set<String> param0 = new HashSet<>();
  private static final ClassAndMethod cm = ClassAndMethod.create(Object.class, "method");;

  static {
    thiz.put("key", "value");
    param0.add("present");
  }


  @Test
  public void testSpanKind() {
    NocodeSpanKindExtractor ex = new NocodeSpanKindExtractor();

    NocodeRules.Rule server = new NocodeRules.Rule("java.lang.Object", "method", "n", "SERVER", Collections.EMPTY_MAP);
    assertEquals(SpanKind.SERVER, ex.extract(new NocodeMethodInvocation(server, cm, null, null)));

    NocodeRules.Rule invalid = new NocodeRules.Rule("java.lang.Object", "method", "n", "INVALID", Collections.EMPTY_MAP);
    assertEquals(SpanKind.INTERNAL, ex.extract(new NocodeMethodInvocation(invalid, cm, null, null)));

    NocodeRules.Rule unspecified = new NocodeRules.Rule("java.lang.Object", "method", "n", null, Collections.EMPTY_MAP);
    assertEquals(SpanKind.INTERNAL, ex.extract(new NocodeMethodInvocation(unspecified, cm, null, null)));
  }

  @Test
  public void testSpanName() {
    NocodeSpanNameExtractor ex = new NocodeSpanNameExtractor();

    NocodeRules.Rule unspecified = new NocodeRules.Rule("java.lang.Object", "method", null, null, Collections.EMPTY_MAP);
    assertEquals("Object.method", ex.extract(new NocodeMethodInvocation(unspecified, cm, null, null)));

    NocodeRules.Rule specified = new NocodeRules.Rule("java.lang.Object", "method", "this.get(\"key\")", null, Collections.EMPTY_MAP);
    assertEquals("value", ex.extract(new NocodeMethodInvocation(specified, cm, thiz, null)));
  }

  @Test
  public void testAttributes() {
    NocodeAttributesExtractor ex = new NocodeAttributesExtractor();

    Map<String, String> attsSpecs = new HashMap<>();
    attsSpecs.put("key1", "this.get(\"key\").toString().substring(1)");
    attsSpecs.put("key2", "param0.contains(\"present\")");
    attsSpecs.put("invalid", "invalid syntax.)(=no value, key not present");
    NocodeRules.Rule rule = new NocodeRules.Rule("java.lang.Object","method", null, null, attsSpecs);
    AttributesBuilder ab = Attributes.builder();
    ex.onStart(ab, Context.current(), new NocodeMethodInvocation(rule, cm, thiz, new Object[]{param0}));
    Map<AttributeKey<?>, Object> atts = ab.build().asMap();
    System.out.println(atts);
    assertEquals(4, atts.size());
    assertEquals("alue", atts.get(AttributeKey.stringKey("key1")));
    assertEquals("true", atts.get(AttributeKey.stringKey("key2")));
    assertEquals("method", atts.get(AttributeKey.stringKey("code.function")));
    assertEquals("java.lang.Object", atts.get(AttributeKey.stringKey("code.namespace")));
    assertFalse(atts.containsKey(AttributeKey.stringKey("invalid")));
  }




}
