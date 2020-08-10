package com.signalfx.opentelemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class SplunkSpanExporter implements SpanExporter {
  private final SpanExporter delegate;

  public SplunkSpanExporter(SpanExporter delegate) {
    this.delegate = Objects.requireNonNull(delegate, "Delegate span exporter cannot be null");
  }

  @Override
  public ResultCode export(Collection<SpanData> spans) {
    return delegate.export(spans.stream().map(SpanDataWrapper::new).collect(toList()));
  }

  @Override
  public ResultCode flush() {
    return delegate.flush();
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }
}
