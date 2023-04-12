package com.splunk.opentelemetry.logging;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.logging.simple.Slf4jSimpleLoggingCustomizer;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;

@AutoService(LoggingCustomizer.class)
public class SplunkSlf4jLoggingCustomizer implements LoggingCustomizer {
  private final Slf4jSimpleLoggingCustomizer delegate = new Slf4jSimpleLoggingCustomizer();

  @Override
  public String name() {
    return delegate.name();
  }

  @Override
  public void init() {
    addCustomLoggingConfiguration();

    delegate.init();
  }

  @Override
  public void onStartupSuccess() {
    delegate.onStartupSuccess();
  }

  @Override
  public void onStartupFailure(Throwable throwable) {
    delegate.onStartupFailure(throwable);
  }

  private static final String METRICS_RETRY_LOGGER_PROPERTY =
      "io.opentelemetry.javaagent.slf4j.simpleLogger.log.com.signalfx.shaded.apache.http.impl.execchain.RetryExec";

  private static void addCustomLoggingConfiguration() {
    // metrics exporter sometimes logs "Broken pipe (Write failed)" at INFO; usually in
    // docker-based environments
    // most likely docker resets long-running connections and the exporter has to retry, and it
    // always succeeds after retrying and metrics are exported correctly
    // limit default logging level to WARN unless debug mode is on or the user has overridden it
    String metricsRetryLoggerLevel = System.getProperty(METRICS_RETRY_LOGGER_PROPERTY);
    if (metricsRetryLoggerLevel == null && !isDebugMode()) {
      System.setProperty(METRICS_RETRY_LOGGER_PROPERTY, "WARN");
    }
  }

  private static boolean isDebugMode() {
    String value = System.getProperty("otel.javaagent.debug");
    if (value == null) {
      value = System.getenv("OTEL_JAVAAGENT_DEBUG");
    }
    return Boolean.parseBoolean(value);
  }
}
