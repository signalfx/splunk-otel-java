package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.nocode.NocodeRules;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;

public class NocodeSpanNameExtractor implements SpanNameExtractor<NocodeMethodInvocation> {
  private final SpanNameExtractor<ClassAndMethod> defaultNamer;

  public NocodeSpanNameExtractor() {
    this.defaultNamer = CodeSpanNameExtractor.create(ClassAndMethod.codeAttributesGetter());
  }

  @Override
  public String extract(NocodeMethodInvocation mi) {
    NocodeRules.Rule rule = mi.getRule();
    if (rule != null && rule.spanName != null) {
      // FIXME might want to allow string literal as a "statement" so that
      // the name could be hardcoded to something?
      String name = JSPS.evaluate(rule.spanName, mi.getThiz(), mi.getParameters());
      if (name != null) {
        return name;
      }
    }
    return defaultNamer.extract(mi.getClassAndMethod());
  }
}
