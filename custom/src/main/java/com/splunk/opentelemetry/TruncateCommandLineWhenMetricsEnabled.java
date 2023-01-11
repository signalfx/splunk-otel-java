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

package com.splunk.opentelemetry;

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_FULL_COMMAND_LINE;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.function.BiFunction;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class TruncateCommandLineWhenMetricsEnabled implements AutoConfigurationCustomizerProvider {

  private static final Logger logger =
      Logger.getLogger(TruncateCommandLineWhenMetricsEnabled.class.getName());

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addResourceCustomizer(new CommandLineTruncator());
  }

  @Override
  public int order() {
    // Need to run late so that we can override other resource customizers
    return 9999;
  }

  @VisibleForTesting
  static class CommandLineTruncator implements BiFunction<Resource, ConfigProperties, Resource> {
    @Override
    public Resource apply(Resource existing, ConfigProperties config) {
      boolean metricsEnabled = config.getBoolean(METRICS_ENABLED_PROPERTY, false);
      boolean forceFullCommandline = config.getBoolean(METRICS_FULL_COMMAND_LINE, false);
      if (!metricsEnabled || forceFullCommandline) {
        return existing;
      }

      String commandLine = existing.getAttribute(ResourceAttributes.PROCESS_COMMAND_LINE);
      if (commandLine == null || commandLine.length() < 256) {
        return existing;
      }

      logger.warning("Metrics are enabled. Truncating process.command_line resource attribute.");
      String newCommandLine = commandLine.substring(0, 250) + "[...]";
      return existing.merge(
          Resource.create(Attributes.of(ResourceAttributes.PROCESS_COMMAND_LINE, newCommandLine)));
    }
  }
}
