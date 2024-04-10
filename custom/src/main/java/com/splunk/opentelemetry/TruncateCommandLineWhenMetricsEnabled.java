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

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_FULL_COMMAND_LINE;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    private static final int MAX_LENGTH = 255;

    @Override
    public Resource apply(Resource existing, ConfigProperties config) {
      boolean forceFullCommandline = config.getBoolean(METRICS_FULL_COMMAND_LINE, false);
      if (forceFullCommandline) {
        return existing;
      }

      Resource resource = existing;
      if (resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS) != null) {
        List<String> newCommandArgs =
            truncate(resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS));
        if (newCommandArgs != null) {
          resource =
              resource.merge(
                  Resource.create(
                      Attributes.of(
                          ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS, newCommandArgs)));
        }
      }

      String commandLine = resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE);
      if (commandLine != null && commandLine.length() > MAX_LENGTH) {
        String newCommandLine = commandLine.substring(0, MAX_LENGTH - 3) + "...";
        resource =
            resource.merge(
                Resource.create(
                    Attributes.of(
                        ProcessIncubatingAttributes.PROCESS_COMMAND_LINE, newCommandLine)));
      }

      if (existing != resource) {
        logger.fine(
            "Metrics are enabled. Truncating process.command_line and process.command_args resource attributes.");
      }
      return resource;
    }

    private static List<String> truncate(List<String> list) {
      // Ensure that String representation of the List does not exceed max allowed length. List is
      // translated to String as a JSON array (it is surrounded in [], each element is surrounded in
      // double quotes, elements are separated with a comma).
      int maxLength = MAX_LENGTH - 4;
      List<String> result = new ArrayList<>();
      int totalLength = 0;
      for (Iterator<String> i = list.iterator(); i.hasNext(); ) {
        String s = i.next();
        int length = s.length();
        if (i.hasNext()) {
          // we assume that list elements are joined with ","
          length += 3;
        }
        if (totalLength + length <= maxLength) {
          result.add(s);
        } else {
          // if there is room for less than 3 chars we'll need to truncate the previous element
          if (maxLength - totalLength >= 3) {
            s = s.substring(0, Math.min(s.length(), maxLength - totalLength - 3)) + "...";
          } else {
            s = result.remove(result.size() - 1);
            // we just truncate the last 3 chars, this can make the end result slightly shorter
            // than the max length
            s = s.substring(0, Math.max(0, s.length() - 3)) + "...";
          }
          result.add(s);
          return result;
        }
        totalLength += length;
      }

      return null;
    }
  }
}
