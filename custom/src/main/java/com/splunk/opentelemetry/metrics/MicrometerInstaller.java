package com.splunk.opentelemetry.metrics;

import com.google.auto.service.AutoService;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.splunk.opentelemetry.javaagent.bootstrap.GlobalMetricsTags;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.signalfx.SignalFxMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ComponentInstaller.class)
public class MicrometerInstaller implements ComponentInstaller {
  @Override
  public void beforeByteBuddyAgent() {
    SignalFxMeterRegistry signalFxRegistry =
        new SignalFxMeterRegistry(new SplunkMetricsConfig(), Clock.SYSTEM);
    NamingConvention signalFxNamingConvention = signalFxRegistry.config().namingConvention();
    signalFxRegistry.config().namingConvention(new OtelNamingConvention(signalFxNamingConvention));

    // TODO: agent does not respond?
    try {
      Logger errorLogger = LoggerFactory.getLogger(MicrometerInstaller.class);

      Field exceptionListeners =
          SignalFxMeterRegistry.class.getDeclaredField("onSendErrorHandlerCollection");
      exceptionListeners.setAccessible(true);
      Set<OnSendErrorHandler> listener =
          Collections.singleton(
              error ->
                  errorLogger.error(
                      "Cannot send metrics: {}", error.getMessage(), error.getException()));
      exceptionListeners.set(signalFxRegistry, listener);
    } catch (Throwable ignored) {
    }

    setGlobalTags();
    Metrics.addRegistry(signalFxRegistry);
  }

  private static void setGlobalTags() {
    Attributes resourceAttributes = Resource.getDefault().getAttributes();
    List<Tag> globalTags =
        Arrays.asList(
            Tag.of("sf_environment", resourceAttributes.get(AttributeKey.stringKey("environment"))),
            Tag.of("sf_service", resourceAttributes.get(ResourceAttributes.SERVICE_NAME)));
    GlobalMetricsTags.set(globalTags);
  }

  @Override
  public void afterByteBuddyAgent() {
    List<Tag> tags = GlobalMetricsTags.get();
    // install some default JVM metrics
    new ClassLoaderMetrics(tags).bindTo(Metrics.globalRegistry);
    new JvmGcMetrics(tags).bindTo(Metrics.globalRegistry);
    // the two duration are default values
    new JvmHeapPressureMetrics(tags, Duration.ofMinutes(5), Duration.ofMinutes(1)).bindTo(Metrics.globalRegistry);
    new JvmMemoryMetrics(tags).bindTo(Metrics.globalRegistry);
    new JvmThreadMetrics(tags).bindTo(Metrics.globalRegistry);
  }
}
