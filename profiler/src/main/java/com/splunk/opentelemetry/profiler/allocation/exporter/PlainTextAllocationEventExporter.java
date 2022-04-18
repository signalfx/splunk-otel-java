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

package com.splunk.opentelemetry.profiler.allocation.exporter;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.INSTRUMENTATION_SCOPE_INFO;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STACK_TRUNCATED;

import com.splunk.opentelemetry.profiler.Configuration.DataFormat;
import com.splunk.opentelemetry.profiler.LogDataCommonAttributes;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

public class PlainTextAllocationEventExporter implements AllocationEventExporter {
  private static final AttributeKey<Long> ALLOCATION_SIZE_KEY =
      AttributeKey.longKey("memory.allocated");

  private final StackSerializer stackSerializer;
  private final int stackDepth;
  private final LogProcessor logProcessor;
  private final LogDataCommonAttributes commonAttributes;
  private final Resource resource;

  private PlainTextAllocationEventExporter(Builder builder) {
    this.logProcessor = builder.logProcessor;
    this.commonAttributes = builder.commonAttributes;
    this.resource = builder.resource;
    this.stackDepth = builder.stackDepth;
    this.stackSerializer =
        builder.stackSerializer != null ? builder.stackSerializer : new StackSerializer(stackDepth);
  }

  @Override
  public void export(RecordedEvent event, AllocationEventSampler sampler, SpanContext spanContext) {
    RecordedStackTrace stackTrace = event.getStackTrace();
    if (stackTrace == null) {
      return;
    }
    Instant time = event.getStartTime();
    String body = buildBody(event, stackTrace);

    AttributesBuilder builder =
        commonAttributes
            .builder(event.getEventType().getName())
            .put(DATA_TYPE, ProfilingDataType.ALLOCATION.value())
            .put(DATA_FORMAT, DataFormat.TEXT.value())
            .put(ALLOCATION_SIZE_KEY, event.getLong("allocationSize"));
    if (sampler != null) {
      sampler.addAttributes((k, v) -> builder.put(k, v), (k, v) -> builder.put(k, v));
    }

    // stack trace is truncated either by jfr or in StackSerializer
    if (stackTrace.isTruncated() || stackTrace.getFrames().size() > stackDepth) {
      builder.put(THREAD_STACK_TRUNCATED, true);
    }

    Attributes attributes = builder.build();

    LogDataBuilder logDataBuilder =
        LogDataBuilder.create(resource, INSTRUMENTATION_SCOPE_INFO)
            .setEpoch(time)
            .setBody(body)
            .setAttributes(attributes);

    if (spanContext != null && spanContext.isValid()) {
      logDataBuilder.setSpanContext(spanContext);
    }

    logProcessor.emit(logDataBuilder.build());
  }

  private String buildBody(RecordedEvent event, RecordedStackTrace stackTrace) {
    String stack = stackSerializer.serialize(stackTrace);
    RecordedThread thread = event.getThread();
    String name = thread == null ? "unknown" : thread.getJavaName();
    long id = thread == null ? 0 : thread.getJavaThreadId();
    String result = "\"" + name + "\"" + " #" + id;
    if (thread != null) {
      result += " nid=0x" + Long.toHexString(thread.getOSThreadId());
    }
    result += "\n";
    result += "   java.lang.Thread.State: UNKNOWN\n" + stack;
    return result;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private StackSerializer stackSerializer;
    private LogProcessor logProcessor;
    private LogDataCommonAttributes commonAttributes;
    private Resource resource;
    private int stackDepth;

    public PlainTextAllocationEventExporter build() {
      return new PlainTextAllocationEventExporter(this);
    }

    public Builder stackSerializer(StackSerializer stackSerializer) {
      this.stackSerializer = stackSerializer;
      return this;
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

    public Builder stackDepth(int stackDepth) {
      this.stackDepth = stackDepth;
      return this;
    }
  }
}
