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

  private static final String LOGGER_PREFIX = "io.opentelemetry.javaagent.slf4j.simpleLogger.log.";

  private static void addCustomLoggingConfiguration() {
    if (!isDebugMode()) {
      // metrics exporter sometimes logs "Broken pipe (Write failed)" at INFO; usually in
      // docker-based environments
      // most likely docker resets long-running connections and the exporter has to retry, and it
      // always succeeds after retrying and metrics are exported correctly
      // limit default logging level to WARN unless debug mode is on or the user has overridden it
      setLogLevelIfNotSet("org.apache.http.impl.execchain.RetryExec", "WARN");

      // Silence the following warning on jdk21, this warning should go away with an update to jfr
      // parser
      // [otel.javaagent 2023-10-09 14:38:17:665 +0000] [JFR Recording Sequencer] WARN
      // org.openjdk.jmc.flightrecorder.internal.parser.v1.ValueReaders$ReflectiveReader - Could not
      // find field with name 'virtual' in reader for 'thread'
      setLogLevelIfNotSet(
          "org.openjdk.jmc.flightrecorder.internal.parser.v1.ValueReaders$ReflectiveReader",
          "ERROR");
    }
  }

  private static void setLogLevelIfNotSet(String className, String logLevel) {
    String loggerProperty = LOGGER_PREFIX + className;
    String currentLogLevel = System.getProperty(loggerProperty);
    if (currentLogLevel == null) {
      System.setProperty(loggerProperty, logLevel);
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
