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
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.signalfx.SignalFxMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.ArrayList;
import java.util.List;

@AutoService(ComponentInstaller.class)
public class MicrometerInstaller implements ComponentInstaller {
  @Override
  public void beforeByteBuddyAgent() {
    List<Tag> tags = getGlobalTags();
    GlobalMetricsTags.set(tags);
    Metrics.addRegistry(createSplunkMeterRegistry());
  }

  private static List<Tag> getGlobalTags() {
    Attributes resourceAttributes = OpenTelemetrySdkAutoConfiguration.getResource().getAttributes();
    String environment = resourceAttributes.get(AttributeKey.stringKey("environment"));
    String service = resourceAttributes.get(ResourceAttributes.SERVICE_NAME);
    String runtime = resourceAttributes.get(ResourceAttributes.PROCESS_RUNTIME_NAME);
    Long pid = resourceAttributes.get(ResourceAttributes.PROCESS_PID);

    List<Tag> globalTags = new ArrayList<>(4);
    if (environment != null) {
      globalTags.add(Tag.of("deployment.environment", environment));
    }
    if (service != null) {
      globalTags.add(Tag.of("service", service));
    }
    if (runtime != null) {
      globalTags.add(Tag.of("runtime", runtime));
    }
    if (pid != null) {
      globalTags.add(Tag.of("process.pid", pid.toString()));
    }
    return globalTags;
  }

  private static SignalFxMeterRegistry createSplunkMeterRegistry() {
    SignalFxMeterRegistry signalFxRegistry =
        new SignalFxMeterRegistry(new SplunkMetricsConfig(), Clock.SYSTEM);
    NamingConvention signalFxNamingConvention = signalFxRegistry.config().namingConvention();
    signalFxRegistry.config().namingConvention(new OtelNamingConvention(signalFxNamingConvention));
    return signalFxRegistry;
  }
}
