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

package com.splunk.opentelemetry.profiler.exporter;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.INSTRUMENTATION_LIBRARY_INFO;

import com.splunk.opentelemetry.profiler.Configuration;
import com.splunk.opentelemetry.profiler.LogDataCommonAttributes;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;

public class PlainTextProfilingEventExporter implements ProfilingEventExporter {
  private final LogProcessor logProcessor;
  private final LogDataCommonAttributes commonAttributes;
  private final Resource resource;

  private PlainTextProfilingEventExporter(Builder builder) {
    this.logProcessor = builder.logProcessor;
    this.commonAttributes = builder.commonAttributes;
    this.resource = builder.resource;
  }

  @Override
  public void export(StackToSpanLinkage linkedStack) {
    Attributes attributes =
        commonAttributes
            .builder(linkedStack)
            .put(DATA_TYPE, ProfilingDataType.PROFILING.value())
            .put(DATA_FORMAT, Configuration.DataFormat.TEXT.value())
            .build();

    LogDataBuilder logDataBuilder =
        LogDataBuilder.create(resource, INSTRUMENTATION_LIBRARY_INFO)
            .setEpoch(linkedStack.getTime())
            .setBody(linkedStack.getRawStack())
            .setAttributes(attributes);
    if (linkedStack.hasSpanInfo()) {
      logDataBuilder.setSpanContext(linkedStack.getSpanContext());
    }

    LogData log = logDataBuilder.build();
    logProcessor.emit(log);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LogProcessor logProcessor;
    private LogDataCommonAttributes commonAttributes;
    private Resource resource;

    public PlainTextProfilingEventExporter build() {
      return new PlainTextProfilingEventExporter(this);
    }

    public Builder logProcessor(LogProcessor logsProcessor) {
      this.logProcessor = logsProcessor;
      return this;
    }

    public Builder commonAttributes(LogDataCommonAttributes commonAttributes) {
      this.commonAttributes = commonAttributes;
      return this;
    }

    public Builder resource(Resource resource) {
      this.resource = resource;
      return this;
    }
  }
}
