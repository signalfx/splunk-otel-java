package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

class SdkShutdownHook implements SpanProcessor {
  @Override
  public CompletableResultCode shutdown() {
    List<CompletableResultCode> results = new ArrayList<>();
    results.add(close(StackTraceSamplerProvider.INSTANCE.get()));
    results.add(close(StagingAreaProvider.INSTANCE.get()));
    results.add(close(StackTraceExporterProvider.INSTANCE.get()));
    return CompletableResultCode.ofAll(results);
  }

  private CompletableResultCode close(Closeable closeable) {
    try {
      closeable.close();
      return CompletableResultCode.ofSuccess();
    } catch (Exception e) {
      return CompletableResultCode.ofExceptionalFailure(e);
    }
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
