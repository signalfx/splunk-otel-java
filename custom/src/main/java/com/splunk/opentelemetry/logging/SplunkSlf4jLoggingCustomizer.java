/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.logging;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.logging.simple.Slf4jSimpleLoggingCustomizer;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;

@AutoService(LoggingCustomizer.class)
public class SplunkSlf4jLoggingCustomizer implements LoggingCustomizer {
  private final Slf4jSimpleLoggingCustomizer delegate = new Slf4jSimpleLoggingCustomizer();

  @Override
  public String name() {
    return delegate.name();
  }

  @Override
  public void init(EarlyInitAgentConfig earlyConfig) {
    addCustomLoggingConfiguration();

    delegate.init(earlyConfig);
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
      "org.apache.http.impl.execchain.RetryExec";

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
