package com.splunk.opentelemetry.instrumentation.nocode;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class NocodeAttributesExtractor implements AttributesExtractor<NocodeMethodInvocation, Void> {
  private final AttributesExtractor<ClassAndMethod, Void> codeExtractor;

  public NocodeAttributesExtractor() {
    codeExtractor = CodeAttributesExtractor.create(ClassAndMethod.codeAttributesGetter());
  }
  @Override
  public void onStart(AttributesBuilder attributesBuilder, Context context, NocodeMethodInvocation mi) {
    codeExtractor.onStart(attributesBuilder, context, mi.getClassAndMethod());

    Map<String, String> attributes = Collections.EMPTY_MAP;
    if (mi.getRule() != null) {
      attributes = mi.getRule().attributes;
    }
    for(String key : attributes.keySet()) {
      String jsps = attributes.get(key);
      String value = JSPS.evaluate(jsps, mi.getThiz(), mi.getParameters());
      if (value != null) {
        attributesBuilder.put(key, value);
      }
    }


  }

  @Override
  public void onEnd(AttributesBuilder attributesBuilder, Context context,
      NocodeMethodInvocation nocodeMethodInvocation, @Nullable Void unused,
      @Nullable Throwable throwable) {
    codeExtractor.onEnd(attributesBuilder, context, nocodeMethodInvocation.getClassAndMethod(), unused, throwable);


  }
}
