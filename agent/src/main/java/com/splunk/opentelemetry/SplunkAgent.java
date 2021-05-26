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

package com.splunk.opentelemetry;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;
import java.util.Locale;

public class SplunkAgent {
  public static void premain(final String agentArgs, final Instrumentation inst) {
    agentmain(agentArgs, inst);
  }

  public static void agentmain(final String agentArgs, final Instrumentation inst) {
    addSplunkAccessTokenToOtlpHeaders();
    addCustomLoggingConfiguration();

    OpenTelemetryAgent.agentmain(agentArgs, inst);
  }

  private static void addSplunkAccessTokenToOtlpHeaders() {
    String accessToken = getConfig("splunk.access.token");
    if (accessToken != null) {
      String userOtlpHeaders = getConfig("otel.exporter.otlp.headers");
      String otlpHeaders =
          (userOtlpHeaders == null ? "" : userOtlpHeaders + ",") + "X-SF-TOKEN=" + accessToken;
      System.setProperty("otel.exporter.otlp.headers", otlpHeaders);
    }
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
    return Boolean.parseBoolean(getConfig("otel.javaagent.debug"));
  }

  private static String getConfig(String propertyName) {
    String value = System.getProperty(propertyName);
    if (value != null) {
      return value;
    }
    return System.getenv(propertyName.replace('.', '_').toUpperCase(Locale.ROOT));
  }
}
