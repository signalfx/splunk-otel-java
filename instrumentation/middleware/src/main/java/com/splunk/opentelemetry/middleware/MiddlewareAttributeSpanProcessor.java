package com.splunk.opentelemetry.middleware;

import com.splunk.opentelemetry.javaagent.shared.MiddlewareHolder;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class MiddlewareAttributeSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    String middlewareName = MiddlewareHolder.middlewareName.get();
    if (middlewareName != null) {
      span.setAttribute(MiddlewareAttributes.MIDDLEWARE_NAME.key, middlewareName);
    }
    String middlewareVersion = MiddlewareHolder.middlewareVersion.get();
    if (middlewareVersion != null) {
      span.setAttribute(MiddlewareAttributes.MIDDLEWARE_VERSION.key, middlewareVersion);
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }
}
