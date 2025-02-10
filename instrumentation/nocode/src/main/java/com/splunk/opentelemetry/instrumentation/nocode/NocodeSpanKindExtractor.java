package com.splunk.opentelemetry.instrumentation.nocode;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;

public class NocodeSpanKindExtractor implements SpanKindExtractor<NocodeMethodInvocation> {
  @Override
  public SpanKind extract(NocodeMethodInvocation mi) {
    if (mi.getRule() == null || mi.getRule().spanKind == null) {
      return SpanKind.INTERNAL;
    }
    return SpanKind.valueOf(mi.getRule().spanKind.toUpperCase());
  }
}
