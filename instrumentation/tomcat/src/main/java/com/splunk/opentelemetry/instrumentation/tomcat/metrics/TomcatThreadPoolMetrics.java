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

package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import io.opentelemetry.instrumentation.api.config.Config;
import org.apache.tomcat.util.net.AbstractEndpoint;

public final class TomcatThreadPoolMetrics {
  private static final String metricsImplementation =
      Config.get().getString("splunk.metrics.implementation");
  private static final boolean useOtelMetrics = "opentelemetry".equals(metricsImplementation);
  private static final boolean useMicrometerMetrics = "micrometer".equals(metricsImplementation);

  public static void registerMetrics(AbstractEndpoint<?, ?> endpoint) {
    if (useOtelMetrics) {
      TomcatThreadPoolOtelMetrics.registerMetrics(endpoint);
    }
    if (useMicrometerMetrics) {
      TomcatThreadPoolMicrometerMetrics.registerMetrics(endpoint);
    }
  }

  public static void unregisterMetrics(AbstractEndpoint<?, ?> endpoint) {
    if (useOtelMetrics) {
      TomcatThreadPoolOtelMetrics.unregisterMetrics(endpoint);
    }
    if (useMicrometerMetrics) {
      TomcatThreadPoolMicrometerMetrics.unregisterMetrics(endpoint);
    }
  }

  private TomcatThreadPoolMetrics() {}
}
