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
import com.splunk.opentelemetry.javaagent.bootstrap.GlobalMetricsTags;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.signalfx.SignalFxMeterRegistry;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(ComponentInstaller.class)
public class MicrometerInstaller implements ComponentInstaller {
  @Override
  public void beforeByteBuddyAgent() {
    Resource resource = OpenTelemetrySdkAutoConfiguration.getResource();
    GlobalMetricsTags.set(new GlobalTagsBuilder(resource).build());
    Metrics.addRegistry(createSplunkMeterRegistry(resource));
  }

  private static SignalFxMeterRegistry createSplunkMeterRegistry(Resource resource) {
    SignalFxMeterRegistry signalFxRegistry =
        new SignalFxMeterRegistry(new SplunkMetricsConfig(Config.get(), resource), Clock.SYSTEM);
    NamingConvention signalFxNamingConvention = signalFxRegistry.config().namingConvention();
    signalFxRegistry.config().namingConvention(new OtelNamingConvention(signalFxNamingConvention));
    return signalFxRegistry;
  }
}
