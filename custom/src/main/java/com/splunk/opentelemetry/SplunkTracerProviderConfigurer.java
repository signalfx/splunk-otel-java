package com.splunk.opentelemetry;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.middleware.MiddlewareAttributeSpanProcessor;
import io.opentelemetry.sdk.autoconfigure.spi.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@AutoService(SdkTracerProviderConfigurer.class)
public class SplunkTracerProviderConfigurer implements SdkTracerProviderConfigurer {
  @Override
  public void configure(SdkTracerProviderBuilder tracerProvider) {
    tracerProvider
        .setTraceConfig(TraceConfig.builder()
                .setMaxNumberOfAttributes(Integer.MAX_VALUE)
                .setMaxNumberOfEvents(Integer.MAX_VALUE)
                .setMaxNumberOfLinks(1000) // this is the default value actually
                .setMaxNumberOfAttributesPerEvent(Integer.MAX_VALUE)
                .setMaxNumberOfAttributesPerLink(Integer.MAX_VALUE)
                .setMaxLengthOfAttributeValues(TraceConfig.UNLIMITED_ATTRIBUTE_LENGTH)
                .build())
        .setSampler(Sampler.alwaysOn())
        .addSpanProcessor(new MiddlewareAttributeSpanProcessor());
  }
}
