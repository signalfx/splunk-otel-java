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
  @Override
  public Map<String, String> getProperties() {
    Map<String, String> config = new HashMap<>();
    config.put("otel.exporter", "jaeger-thrift");
    // http://localhost:9080/v1/trace is the default endpoint for SmartAgent
    // http://localhost:14268/api/traces is the default endpoint for otel-collector
    config.put("otel.exporter.jaeger.endpoint", "http://localhost:9080/v1/trace");
    config.put("otel.propagators", "b3multi");

    String max = String.valueOf(Integer.MAX_VALUE);
    config.put("otel.config.max.attrs", max);
    config.put("otel.config.max.event.attrs", max);
    config.put("otel.config.max.link.attrs", max);

    // events and links create collections with provided sizes, so we shouldn't set them too high
    config.put("otel.config.max.events", "256");
    config.put("otel.config.max.links", "256");
    // -1 here means no attribute length limit
    config.put("otel.config.max.attr.length", "-1");

    return config;
  }
}
