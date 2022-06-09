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

package com.splunk.opentelemetry.instrumentation.liberty.metrics;

import io.opentelemetry.instrumentation.api.config.Config;

public final class LibertyThreadPoolMetrics {
  private static final boolean useOtelMetrics =
      Config.get().getBoolean("splunk.metrics.otel.enabled", false);
  private static final boolean useMicrometerMetrics =
      Config.get().getBoolean("splunk.metrics.micrometer.enabled", true);

  public static void initialize() {
    if (useOtelMetrics) {
      LibertyThreadPoolOtelMetrics.initialize();
    }
    if (useMicrometerMetrics) {
      LibertyThreadPoolMicrometerMetrics.initialize();
    }
  }

  private LibertyThreadPoolMetrics() {}
}
