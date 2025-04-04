package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Supplier;

class SdkShutdownHook implements SpanProcessor {
  private final Supplier<Closer> closer;

  SdkShutdownHook(Supplier<Closer> closer) {
    this.closer = closer;
  }

  @Override
  public CompletableResultCode shutdown() {
    return closer.get().close();
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {}

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
