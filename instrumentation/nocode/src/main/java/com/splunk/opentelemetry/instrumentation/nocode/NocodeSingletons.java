package com.splunk.opentelemetry.instrumentation.nocode;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;

public class NocodeSingletons {
  // FIXME so much copy and paste from Methods instrumentation
  private static final Instrumenter<ClassAndMethod, Void> INSTRUMENTER;

  static {
    CodeAttributesGetter cag = ClassAndMethod.codeAttributesGetter();

    INSTRUMENTER = Instrumenter.<ClassAndMethod,Void>builder(
        GlobalOpenTelemetry.get(),
        "com.splunk.opentelemetry.instrumentation.nocode",
        CodeSpanNameExtractor.create(cag)).addAttributesExtractor(CodeAttributesExtractor.create(cag))
        .buildInstrumenter(SpanKindExtractor.alwaysInternal());
  }

  public static Instrumenter<ClassAndMethod, Void> instrumentor() {
    return INSTRUMENTER;
  }
}
