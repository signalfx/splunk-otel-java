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

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Metrics;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class MicrometerInstrumentationModule extends InstrumentationModule {
  public MicrometerInstrumentationModule() {
    super("micrometer");
  }

  @Override
  protected boolean defaultEnabled() {
    boolean metricsRegistryPresent = !Metrics.globalRegistry.getRegistries().isEmpty();
    return metricsRegistryPresent && super.defaultEnabled();
  }

  @Override
  public List<String> getMuzzleHelperClassNames() {
    return Arrays.asList(
        // Application Meter has to be the first one, other instruments extend it
        "com.splunk.opentelemetry.micrometer.ApplicationMeter",
        "com.splunk.opentelemetry.micrometer.ApplicationClock",
        "com.splunk.opentelemetry.micrometer.ApplicationCounter",
        "com.splunk.opentelemetry.micrometer.ApplicationDistributionSummary",
        "com.splunk.opentelemetry.micrometer.ApplicationFunctionCounter",
        "com.splunk.opentelemetry.micrometer.ApplicationFunctionTimer",
        "com.splunk.opentelemetry.micrometer.ApplicationGauge",
        "com.splunk.opentelemetry.micrometer.ApplicationLongTaskTimer",
        "com.splunk.opentelemetry.micrometer.ApplicationLongTaskTimer$ApplicationSample",
        "com.splunk.opentelemetry.micrometer.ApplicationMeterRegistry",
        "com.splunk.opentelemetry.micrometer.ApplicationTimer",
        "com.splunk.opentelemetry.micrometer.Bridging");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new MetricsInstrumentation());
  }
}
