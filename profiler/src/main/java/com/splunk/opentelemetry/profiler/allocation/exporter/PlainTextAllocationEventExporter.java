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
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STACK_TRUNCATED;

import com.splunk.opentelemetry.profiler.Configuration.DataFormat;
import com.splunk.opentelemetry.profiler.EventReader;
import com.splunk.opentelemetry.profiler.LogDataCommonAttributes;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.time.Instant;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;

public class PlainTextAllocationEventExporter implements AllocationEventExporter {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(PlainTextAllocationEventExporter.class.getName());
  private static final AttributeKey<Long> ALLOCATION_SIZE_KEY =
      AttributeKey.longKey("memory.allocated");

  private final EventReader eventReader;
  private final StackSerializer stackSerializer;
  private final int stackDepth;
  private final Logger otelLogger;
  private final LogDataCommonAttributes commonAttributes;

  private PlainTextAllocationEventExporter(Builder builder) {
    logger.warning(
        "Plain text profiling format is deprecated and will be removed in the next release.");
    this.eventReader = builder.eventReader;
    this.otelLogger = builder.otelLogger;
    this.commonAttributes = builder.commonAttributes;
    this.stackDepth = builder.stackDepth;
    this.stackSerializer =
        builder.stackSerializer != null ? builder.stackSerializer : new StackSerializer(stackDepth);
  }

  @Override
  public void export(IItem event, AllocationEventSampler sampler, SpanContext spanContext) {
    IMCStackTrace stackTrace = eventReader.getStackTrace(event);
    if (stackTrace == null) {
      return;
    }
    Instant time = eventReader.getStartInstant(event);
    String body = buildBody(event, stackTrace);

    AttributesBuilder builder =
        commonAttributes
            .builder(event.getType().getIdentifier())
            .put(DATA_TYPE, ProfilingDataType.ALLOCATION.value())
            .put(DATA_FORMAT, DataFormat.TEXT.value())
            .put(ALLOCATION_SIZE_KEY, eventReader.getAllocationSize(event));
    if (sampler != null) {
      sampler.addAttributes((k, v) -> builder.put(k, v), (k, v) -> builder.put(k, v));
    }

    // stack trace is truncated either by jfr or in StackSerializer
    if (stackTrace.getTruncationState().isTruncated()
        || stackTrace.getFrames().size() > stackDepth) {
      builder.put(THREAD_STACK_TRUNCATED, true);
    }

    Attributes attributes = builder.build();

    LogRecordBuilder logRecordBuilder =
        otelLogger.logRecordBuilder().setEpoch(time).setBody(body).setAllAttributes(attributes);

    if (spanContext != null && spanContext.isValid()) {
      logRecordBuilder.setContext(Context.root().with(Span.wrap(spanContext)));
    }

    logRecordBuilder.emit();
  }

  private String buildBody(IItem event, IMCStackTrace stackTrace) {
    String stack = stackSerializer.serialize(stackTrace);
    IMCThread thread = eventReader.getThread(event);
    String name = thread == null ? "unknown" : thread.getThreadName();
    long id = thread == null || thread.getThreadId() == null ? 0 : thread.getThreadId();
    String result = "\"" + name + "\"" + " #" + id;
    if (thread != null) {
      Long osThreadId = eventReader.getOSThreadId(thread);
      if (osThreadId == null) {
        osThreadId = 0L;
      }
      result += " nid=0x" + Long.toHexString(osThreadId);
    }
    result += "\n";
    result += "   java.lang.Thread.State: UNKNOWN\n" + stack;
    return result;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private EventReader eventReader;
    private StackSerializer stackSerializer;
    private Logger otelLogger;
    private LogDataCommonAttributes commonAttributes;
    private int stackDepth;

    public PlainTextAllocationEventExporter build() {
      return new PlainTextAllocationEventExporter(this);
    }

    public Builder eventReader(EventReader eventReader) {
      this.eventReader = eventReader;
      return this;
    }

    public Builder stackSerializer(StackSerializer stackSerializer) {
      this.stackSerializer = stackSerializer;
      return this;
    }

    public Builder logEmitter(Logger otelLogger) {
      this.otelLogger = otelLogger;
      return this;
    }

    public Builder commonAttributes(LogDataCommonAttributes commonAttributes) {
      this.commonAttributes = commonAttributes;
      return this;
    }

    public Builder stackDepth(int stackDepth) {
      this.stackDepth = stackDepth;
      return this;
    }
  }
}
