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

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@AutoService(ConfigPropertySource.class)
public class SplunkConfiguration implements ConfigPropertySource {
  public static final String SPLUNK_ACCESS_TOKEN = "splunk.access.token";
  public static final String OTEL_EXPORTER_JAEGER_ENDPOINT = "otel.exporter.jaeger.endpoint";
  public static final String PROFILER_ENABLED_PROPERTY = "splunk.profiler.enabled";
  public static final String PROFILER_MEMORY_ENABLED_PROPERTY = "splunk.profiler.memory.enabled";
  public static final String SPLUNK_REALM_PROPERTY = "splunk.realm";
  public static final String SPLUNK_REALM_NONE = "none";

  private final Function<String, String> systemPropertyProvider;
  private final Function<String, String> envVariableProvider;

  public SplunkConfiguration() {
    this(System::getProperty, System::getenv);
  }

  // visible for tests
  SplunkConfiguration(
      Function<String, String> systemPropertyProvider,
      Function<String, String> envVariableProvider) {
    this.systemPropertyProvider = systemPropertyProvider;
    this.envVariableProvider = envVariableProvider;
  }

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> config = new HashMap<>();

    config.put("otel.traces.sampler", "always_on");

    // by default no metrics are exported
    config.put("otel.metrics.exporter", "none");

    String realm = getRealm();
    if (!SPLUNK_REALM_NONE.equals(realm)) {
      config.put("otel.exporter.otlp.traces.endpoint", "https://ingest." + realm + ".signalfx.com");
      config.put(
          OTEL_EXPORTER_JAEGER_ENDPOINT, "https://ingest." + realm + ".signalfx.com/v2/trace");
    } else {
      // http://localhost:9080/v1/trace is the default endpoint for SmartAgent
      // http://localhost:14268/api/traces is the default endpoint for otel-collector
      config.put(OTEL_EXPORTER_JAEGER_ENDPOINT, "http://localhost:9080/v1/trace");
    }

    // instrumentation settings

    // disable span links in messaging instrumentations
    config.put("otel.instrumentation.common.experimental.suppress-messaging-receive-spans", "true");

    // disable logging instrumentations - we're not currently sending logs (yet)
    config.put("otel.instrumentation.java-util-logging.enabled", "false");
    config.put("otel.instrumentation.log4j-appender.enabled", "false");
    config.put("otel.instrumentation.logback-appender.enabled", "false");
    // disable otel micrometer instrumentation, we use our own for now
    config.put("otel.instrumentation.micrometer.enabled", "false");
    // disable oshi metrics too, just in case
    config.put("otel.instrumentation.oshi.enabled", "false");
    // disable otel runtime-metrics instrumentation; we use micrometer metrics instead
    config.put("otel.instrumentation.runtime-metrics.enabled", "false");
    // enable spring batch instrumentation
    config.put("otel.instrumentation.spring-batch.enabled", "true");
    config.put("otel.instrumentation.spring-batch.item.enabled", "true");

    return config;
  }

  private String getRealm() {
    String value = systemPropertyProvider.apply(SPLUNK_REALM_PROPERTY);
    if (value == null) {
      value = envVariableProvider.apply("SPLUNK_REALM");
    }
    if (value == null) {
      return SPLUNK_REALM_NONE;
    }
    return value;
  }
}
