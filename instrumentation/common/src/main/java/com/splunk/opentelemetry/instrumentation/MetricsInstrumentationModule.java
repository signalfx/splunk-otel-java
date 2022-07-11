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

package com.splunk.opentelemetry.instrumentation;

import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;

/**
 * Instrumentation can extend this class instead of InstrumentationModule if they require metrics to
 * be enabled (and Micrometer has at least one active registry).
 */
public abstract class MetricsInstrumentationModule extends InstrumentationModule {

  protected MetricsInstrumentationModule(
      String mainInstrumentationName, String... additionalInstrumentationNames) {
    super(mainInstrumentationName, additionalInstrumentationNames);
  }

  @Override
  public boolean defaultEnabled() {
    boolean metricsEnabled = Config.get().getBoolean("splunk.metrics.enabled", false);
    return metricsEnabled && super.defaultEnabled();
  }
}
