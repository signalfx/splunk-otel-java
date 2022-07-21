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

package com.splunk.opentelemetry.instrumentation.weblogic.metrics;

import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;

public final class WebLogicThreadPoolMetrics {
  private static final String metricsImplementation =
      InstrumentationConfig.get().getString("splunk.metrics.implementation");
  private static final boolean useOtelMetrics = "opentelemetry".equals(metricsImplementation);
  private static final boolean useMicrometerMetrics = "micrometer".equals(metricsImplementation);

  public static void initialize() {
    if (useOtelMetrics) {
      WebLogicThreadPoolOtelMetrics.initialize();
    }
    if (useMicrometerMetrics) {
      WebLogicThreadPoolMicrometerMetrics.initialize();
    }
  }

  private WebLogicThreadPoolMetrics() {}
}
