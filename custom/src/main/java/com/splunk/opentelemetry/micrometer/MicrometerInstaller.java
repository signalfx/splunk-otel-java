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

package com.splunk.opentelemetry.micrometer;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.signalfx.SignalFxMeterRegistry;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(BeforeAgentListener.class)
public class MicrometerInstaller implements BeforeAgentListener {

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = autoConfiguredOpenTelemetrySdk.getConfig();
    Resource resource = autoConfiguredOpenTelemetrySdk.getResource();
    SplunkMetricsConfig splunkMetricsConfig = new SplunkMetricsConfig(config, resource);

    if (splunkMetricsConfig.enabled()) {
      Tags globalTags = new GlobalTagsBuilder(resource).build();
      Metrics.addRegistry(createSplunkMeterRegistry(splunkMetricsConfig, globalTags));
    }
  }

  private static SignalFxMeterRegistry createSplunkMeterRegistry(
      SplunkMetricsConfig config, Tags globalTags) {
    SignalFxMeterRegistry signalFxRegistry = new SignalFxMeterRegistry(config, Clock.SYSTEM);
    NamingConvention signalFxNamingConvention = signalFxRegistry.config().namingConvention();
    signalFxRegistry
        .config()
        .namingConvention(new OtelNamingConvention(signalFxNamingConvention))
        .meterFilter(MeterFilter.commonTags(globalTags));
    return signalFxRegistry;
  }
}
