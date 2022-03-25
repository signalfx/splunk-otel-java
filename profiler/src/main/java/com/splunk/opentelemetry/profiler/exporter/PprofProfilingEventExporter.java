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

public class PprofProfilingEventExporter implements ProfilingEventExporter {
  private final DataFormat profilingDataFormat;
  private final EventPeriods eventPeriods;
  private final PprofLogDataExporter pprofLogDataExporter;
  private Pprof pprof = createPprof();

  private PprofProfilingEventExporter(Builder builder) {
    this.profilingDataFormat = builder.profilingDataFormat;
    this.eventPeriods = builder.eventPeriods;
    this.pprofLogDataExporter =
        new PprofLogDataExporter(
            builder.logProcessor,
            builder.resource,
            ProfilingDataType.PROFILING,
            builder.profilingDataFormat);
  }

  @Override
  public void export(StackToSpanLinkage stackToSpanLinkage) {
    StackTrace stackTrace = StackTraceParser.parse(stackToSpanLinkage.getRawStack());
    if (stackTrace == null || stackTrace.stackTraceLines.isEmpty()) {
      return;
    }

    Sample.Builder sample = Sample.newBuilder();

    if (stackTrace.threadId != -1) {
      pprof.addLabel(sample, "thread.id", stackTrace.threadId);
      pprof.addLabel(sample, "thread.name", stackTrace.threadName);
    }
    pprof.addLabel(sample, "thread.native.id", stackTrace.nativeThreadId);
    pprof.addLabel(sample, "thread.status", stackTrace.threadStatus);

    for (StackTraceParser.StackTraceLine stl : stackTrace.stackTraceLines) {
      sample.addLocationId(pprof.getLocationId(stl.location, stl.classAndMethod, stl.lineNumber));
    }

    String eventName = stackToSpanLinkage.getSourceEventName();
    pprof.addLabel(sample, SOURCE_EVENT_NAME.getKey(), eventName);
    Duration eventPeriod = eventPeriods.getDuration(eventName);
    if (!EventPeriods.UNKNOWN.equals(eventPeriod)) {
      pprof.addLabel(sample, SOURCE_EVENT_PERIOD.getKey(), eventPeriod.toMillis());
    }
    Instant time = stackToSpanLinkage.getTime();
    pprof.addLabel(sample, "source.event.time", time.toEpochMilli());

    SpanContext spanContext = stackToSpanLinkage.getSpanContext();
    if (spanContext != null && spanContext.isValid()) {
      pprof.addLabel(sample, "trace_id", spanContext.getTraceId());
      pprof.addLabel(sample, "span_id", spanContext.getSpanId());
    }

    pprof.getProfileBuilder().addSample(sample);
  }

  private static Pprof createPprof() {
    return new Pprof();
  }

  private byte[] serializePprof() {
    byte[] result = pprof.serialize(profilingDataFormat);
    pprof = createPprof();
    return result;
  }

  @Override
  public void flush() {
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
    private DataFormat profilingDataFormat;
    private EventPeriods eventPeriods;

    public PprofProfilingEventExporter build() {
      return new PprofProfilingEventExporter(this);
    }

    public Builder logProcessor(LogProcessor logsProcessor) {
      this.logProcessor = logsProcessor;
      return this;
    }

    public Builder resource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public Builder profilingDataFormat(DataFormat profilingDataFormat) {
      this.profilingDataFormat = profilingDataFormat;
      return this;
    }

    public Builder eventPeriods(EventPeriods eventPeriods) {
      this.eventPeriods = eventPeriods;
      return this;
    }
  }
}
