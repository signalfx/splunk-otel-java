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

package com.splunk.opentelemetry.resource;

import static com.splunk.opentelemetry.resource.StringListShortener.truncate;
import static io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS;
import static io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes.PROCESS_COMMAND_LINE;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.resources.ProcessResource;
import io.opentelemetry.instrumentation.resources.ProcessRuntimeResource;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class TruncateCommandLineResourceDetector implements ComponentProvider<Resource> {
  private static final Logger logger =
      Logger.getLogger(TruncateCommandLineResourceDetector.class.getName());

  private static final int MAX_COMMAND_LINE_LENGTH = 255;

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "truncate_command_line_process";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    Resource resource = ProcessResource.get().merge(ProcessRuntimeResource.get());

    List<String> commandArgs = resource.getAttribute(PROCESS_COMMAND_ARGS);
    if (commandArgs != null) {
      logger.fine(() -> "Truncate resource attribute " + PROCESS_COMMAND_ARGS.getKey());
      List<String> newCommandArgs = truncate(commandArgs, MAX_COMMAND_LINE_LENGTH);
      if (newCommandArgs != null) {
        resource =
            resource.merge(Resource.create(Attributes.of(PROCESS_COMMAND_ARGS, newCommandArgs)));
      }
    }

    String commandLine = resource.getAttribute(PROCESS_COMMAND_LINE);
    if (commandLine != null && commandLine.length() > MAX_COMMAND_LINE_LENGTH) {
      logger.fine(() -> "Truncate resource attribute " + PROCESS_COMMAND_LINE.getKey());
      String newCommandLine = commandLine.substring(0, MAX_COMMAND_LINE_LENGTH - 3) + "...";
      resource =
          resource.merge(Resource.create(Attributes.of(PROCESS_COMMAND_LINE, newCommandLine)));
    }

    return resource;
  }
}
