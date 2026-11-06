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

package com.splunk.opentelemetry.jmx;

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.isDeclarativeConfig;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(AgentListener.class)
public class JmxMetricsInitializer implements AgentListener, AutoCloseable {
  private static final Logger logger = Logger.getLogger(JmxMetricsInitializer.class.getName());
  private static final String CONFIG_PATH = "distribution.splunk.java.jmx";

  private JmxMetricsService metricsService;
  private Thread shutdownHook;

  @Override
  public synchronized void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    if (metricsService != null || !isDeclarativeConfig(sdk)) {
      return;
    }

    DeclarativeConfigProperties config = JmxMetricsConfigurationCustomizerProvider.getJmxConfig();
    if (config == null) {
      return;
    }

    try {
      JmxMetricsService service = new JmxMetricsService(sdk.getOpenTelemetrySdk());
      service.start(config);
      metricsService = service;
      shutdownHook = new Thread(this::close, "splunk-jmx-shutdown");
      Runtime.getRuntime().addShutdownHook(shutdownHook);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unable to start JMX metrics from " + CONFIG_PATH, e);
      close();
    }
  }

  @Override
  public synchronized void close() {
    if (metricsService != null) {
      try {
        metricsService.close();
      } catch (Exception e) {
        logger.log(Level.WARNING, "Unable to stop JMX metrics collection", e);
      } finally {
        metricsService = null;
      }
    }
    if (shutdownHook != null) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException ignored) {
        // Shutdown is already in progress.
      } finally {
        shutdownHook = null;
      }
    }
  }
}
