package com.splunk.opentelemetry.middleware;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.spi.TracerCustomizer;
import io.opentelemetry.sdk.trace.TracerSdkManagement;

@AutoService(TracerCustomizer.class)
public class MiddlewareTracerCustomizer implements TracerCustomizer {

  @Override
  public void configure(TracerSdkManagement tracerManagement) {
    tracerManagement.addSpanProcessor(new MiddlewareAttributeSpanProcessor());
  }

}
