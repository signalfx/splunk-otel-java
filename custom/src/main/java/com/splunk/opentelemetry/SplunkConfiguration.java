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
import com.splunk.opentelemetry.javaagent.ConfigurationConstants;
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;
import java.util.HashMap;
import java.util.Map;

@AutoService(ConfigPropertySource.class)
public class SplunkConfiguration implements ConfigPropertySource {
  public static final String SPLUNK_ACCESS_TOKEN = ConfigurationConstants.SPLUNK_ACCESS_TOKEN;
  public static final String OTEL_EXPORTER_JAEGER_ENDPOINT = "otel.exporter.jaeger.endpoint";

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> config = new HashMap<>();

    // by default no metrics are exported
    config.put("otel.metrics.exporter", "none");
    // disable otel runtime-metrics instrumentation; we use micrometer metrics instead
    config.put("otel.instrumentation.runtime-metrics.enabled", "false");
    // just in case disable oshi metrics too
    config.put("otel.instrumentation.oshi.enabled", "false");

    // http://localhost:9080/v1/trace is the default endpoint for SmartAgent
    // http://localhost:14268/api/traces is the default endpoint for otel-collector
    config.put(OTEL_EXPORTER_JAEGER_ENDPOINT, "http://localhost:9080/v1/trace");

    // enable experimental instrumentation
    config.put("otel.instrumentation.spring-batch.enabled", "true");
    config.put("otel.instrumentation.spring-batch.item.enabled", "true");

    // disable span links in messaging instrumentations
    config.put("otel.instrumentation.common.experimental.suppress-messaging-receive-spans", "true");

    config.put("otel.traces.sampler", "always_on");
    return config;
  }
}
