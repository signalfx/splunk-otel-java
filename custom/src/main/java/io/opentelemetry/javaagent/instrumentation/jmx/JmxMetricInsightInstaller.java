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

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jmx;

import com.splunk.opentelemetry.jmx.JmxMetricInsightSupervisor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.api.incubator.config.internal.DeclarativeConfigUtil;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distribution replacement for the upstream JMX startup listener.
 *
 * <p>The package and class name must match upstream. The agent build keeps this class ahead of the
 * upstream copy, and the upstream {@link AgentListener} service descriptor registers it.
 */
public class JmxMetricInsightInstaller implements AgentListener {
  private static final Logger logger = Logger.getLogger(JmxMetricInsightInstaller.class.getName());
  private static final String INSTRUMENTATION_NAME = "jmx";

  @Override
  public synchronized void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    DeclarativeConfigProperties config =
        DeclarativeConfigUtil.getInstrumentationConfig(
            GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME);

    JmxMetricInsightSupervisor supervisor =
        JmxMetricInsightSupervisor.initialize(sdk.getOpenTelemetrySdk(), config);

    try {
      supervisor.start();
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unable to start JMX metrics", e);
    }
  }
}
