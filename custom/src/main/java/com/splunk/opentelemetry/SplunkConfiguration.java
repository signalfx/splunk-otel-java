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
import io.opentelemetry.javaagent.spi.config.PropertySource;
import java.util.HashMap;
import java.util.Map;

@AutoService(PropertySource.class)
public class SplunkConfiguration implements PropertySource {
  public static final String SPLUNK_ACCESS_TOKEN = "splunk.access.token";
  public static final String OTEL_EXPORTER_JAEGER_ENDPOINT = "otel.exporter.jaeger.endpoint";

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> config = new HashMap<>();

    // by default no metrics are exported
    config.put("otel.metrics.exporter", "none");
    // disable otel runtime-metrics instrumentation; we use micrometer metrics instead
    config.put("otel.instrumentation.runtime-metrics.enabled", "false");

    // http://localhost:9080/v1/trace is the default endpoint for SmartAgent
    // http://localhost:14268/api/traces is the default endpoint for otel-collector
    config.put(OTEL_EXPORTER_JAEGER_ENDPOINT, "http://localhost:9080/v1/trace");

    // enable experimental instrumentation
    config.put("otel.instrumentation.spring-batch.enabled", "true");
    config.put("otel.instrumentation.spring-batch.item.enabled", "true");

    // metrics exporter sometimes logs "Broken pipe (Write failed)" at INFO; usually in docker-based
    // environments
    // most likely docker resets long-running connections and the exporter has to retry, and it
    // always succeeds after
    // retrying and metrics are exported correctly
    config.put(
        "io.opentelemetry.javaagent.slf4j.simpleLogger.log.com.signalfx.shaded.apache.http.impl.execchain.RetryExec",
        "WARN");

    return config;
  }
}
