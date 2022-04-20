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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_TIME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SPAN_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_OS_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STATE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.TRACE_ID;

import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.Configuration.DataFormat;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTrace;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.time.Instant;

public class PprofCpuEventExporter implements CpuEventExporter {
  private final DataFormat dataFormat;
  private final EventPeriods eventPeriods;
  private final PprofLogDataExporter pprofLogDataExporter;
  private Pprof pprof = createPprof();

  private PprofCpuEventExporter(Builder builder) {
    this.dataFormat = builder.dataFormat;
    this.eventPeriods = builder.eventPeriods;
    this.pprofLogDataExporter =
        new PprofLogDataExporter(
            builder.logProcessor, builder.resource, ProfilingDataType.CPU, builder.dataFormat);
  }

  @Override
  public void export(StackToSpanLinkage stackToSpanLinkage) {
    StackTrace stackTrace = StackTraceParser.parse(stackToSpanLinkage.getRawStack());
    if (stackTrace == null || stackTrace.getStackTraceLines().isEmpty()) {
      return;
    }

    Sample.Builder sample = Sample.newBuilder();

    if (stackTrace.getThreadId() != -1) {
      pprof.addLabel(sample, THREAD_ID, stackTrace.getThreadId());
      pprof.addLabel(sample, THREAD_NAME, stackTrace.getThreadName());
    }
    if (stackTrace.getOsThreadId() != -1) {
      pprof.addLabel(sample, THREAD_OS_ID, stackTrace.getOsThreadId());
    }
    pprof.addLabel(sample, THREAD_STATE, stackTrace.getThreadState());

    for (StackTraceParser.StackTraceLine stl : stackTrace.getStackTraceLines()) {
      sample.addLocationId(
          pprof.getLocationId(stl.getLocation(), stl.getClassAndMethod(), stl.getLineNumber()));
    }

    String eventName = stackToSpanLinkage.getSourceEventName();
    pprof.addLabel(sample, SOURCE_EVENT_NAME, eventName);
    Duration eventPeriod = eventPeriods.getDuration(eventName);
    if (!EventPeriods.UNKNOWN.equals(eventPeriod)) {
      pprof.addLabel(sample, SOURCE_EVENT_PERIOD, eventPeriod.toMillis());
    }
    Instant time = stackToSpanLinkage.getTime();
    pprof.addLabel(sample, SOURCE_EVENT_TIME, time.toEpochMilli());

    SpanContext spanContext = stackToSpanLinkage.getSpanContext();
    if (spanContext != null && spanContext.isValid()) {
      pprof.addLabel(sample, TRACE_ID, spanContext.getTraceId());
      pprof.addLabel(sample, SPAN_ID, spanContext.getSpanId());
    }

    pprof.getProfileBuilder().addSample(sample);
  }

  private static Pprof createPprof() {
    return new Pprof();
  }

  private byte[] serializePprof() {
    byte[] result = pprof.serialize(dataFormat);
    pprof = createPprof();
    return result;
  }

  @Override
  public void flush() {
    if (!pprof.hasSamples()) {
      return;
    }
    // Flush is called after each JFR chunk, hopefully this will keep batch sizes small enough.
    byte[] bytes = serializePprof();
    pprofLogDataExporter.export(bytes);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LogProcessor logProcessor;
    private Resource resource;
    private DataFormat dataFormat;
    private EventPeriods eventPeriods;

    public PprofCpuEventExporter build() {
      return new PprofCpuEventExporter(this);
    }

    public Builder logProcessor(LogProcessor logsProcessor) {
      this.logProcessor = logsProcessor;
      return this;
    }

    public Builder resource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public Builder dataFormat(DataFormat dataFormat) {
      this.dataFormat = dataFormat;
      return this;
    }

    public Builder eventPeriods(EventPeriods eventPeriods) {
      this.eventPeriods = eventPeriods;
      return this;
    }
  }
}
