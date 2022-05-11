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

package com.splunk.opentelemetry.testing;

import static java.util.Collections.emptyMap;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@AutoService(BeforeAgentListener.class)
public class TestMicrometerInstaller implements BeforeAgentListener {

  @Override
  public void beforeAgent(
      Config config, AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    Tags globalMetricsTags =
        config.getMap("splunk.testing.metrics.global-tags", emptyMap()).entrySet().stream()
            .map(e -> Tag.of(e.getKey(), e.getValue()))
            .reduce(Tags.empty(), Tags::and, Tags::concat);

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    registry.config().meterFilter(MeterFilter.commonTags(globalMetricsTags));
    Metrics.addRegistry(registry);
  }
}
