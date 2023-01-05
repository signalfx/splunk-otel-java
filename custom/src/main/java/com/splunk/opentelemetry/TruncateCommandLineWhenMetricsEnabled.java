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
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.logging.Logger;

@AutoService(ResourceProvider.class)
public class TruncateCommandLineWhenMetricsEnabled implements ConditionalResourceProvider {

  private static final Logger logger =
      Logger.getLogger(TruncateCommandLineWhenMetricsEnabled.class.getName());

  private String commandLine;

  @Override
  public boolean shouldApply(ConfigProperties config, Resource existing) {
    boolean metricsEnabled = config.getBoolean(METRICS_ENABLED_PROPERTY, false);
    boolean forceFullCommandline = config.getBoolean(METRICS_FULL_COMMAND_LINE, false);
    boolean wantTruncate = metricsEnabled && !forceFullCommandline;
    if (wantTruncate) {
      logger.warning(
          "Warning: Metrics are enabled, so process.command_line resource attribute is being truncated.");
      commandLine = existing.getAttribute(ResourceAttributes.PROCESS_COMMAND_LINE);
      if (commandLine.length() >= 256) {
        commandLine = commandLine.substring(0, 250) + "[...]";
      }
    }
    return wantTruncate;
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return Resource.create(Attributes.of(ResourceAttributes.PROCESS_COMMAND_LINE, commandLine));
  }

  @Override
  public int order() {
    // Need to run late so that we can override the existing commandline
    return 9999;
  }
}
