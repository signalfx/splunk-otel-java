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

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.profiler.LogExporterBuilder.INSTRUMENTATION_LIBRARY_INFO;

import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.function.Function;

/** Turns a linked stack into a new LogData instance */
public class LogDataCreator implements Function<StackToSpanLinkage, LogData> {

  static final String PROFILING_SOURCE = "otel.profiling";

  private final LogDataCommonAttributes commonAttributes;
  private final Resource resource;

  public LogDataCreator(LogDataCommonAttributes commonAttributes, Resource resource) {
    this.commonAttributes = commonAttributes;
    this.resource = resource;
  }

  @Override
  public LogData apply(StackToSpanLinkage linkedStack) {
    Attributes attributes = commonAttributes.builder(linkedStack).build();

    LogDataBuilder logDataBuilder =
        LogDataBuilder.create(resource, INSTRUMENTATION_LIBRARY_INFO)
            .setEpoch(linkedStack.getTime())
            .setBody(linkedStack.getRawStack())
            .setAttributes(attributes);
    if (linkedStack.hasSpanInfo()) {
      logDataBuilder.setSpanContext(linkedStack.getSpanContext());
    }
    return logDataBuilder.build();
  }
}
