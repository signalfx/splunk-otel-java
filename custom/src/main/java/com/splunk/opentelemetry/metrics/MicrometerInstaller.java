package com.splunk.opentelemetry.metrics;

import com.google.auto.service.AutoService;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.signalfx.SignalFxMeterRegistry;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import java.lang.reflect.Field;
import java.util.Collections;
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

    Metrics.addRegistry(signalFxRegistry);
  }

  @Override
  public void afterByteBuddyAgent() {
    // install some default JVM metrics
    new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
    new JvmGcMetrics().bindTo(Metrics.globalRegistry);
    new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
    new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
    new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
  }
}
