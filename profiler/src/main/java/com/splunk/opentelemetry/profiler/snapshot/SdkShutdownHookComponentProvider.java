package com.splunk.opentelemetry.profiler.snapshot;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SdkShutdownHookComponentProvider  implements ComponentProvider<SpanProcessor> {
  public final static String NAME = "sdk-shutdown-hook";

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    return new SdkShutdownHook();
  }
}
