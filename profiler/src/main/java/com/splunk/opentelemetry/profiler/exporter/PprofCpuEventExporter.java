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
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STACK_TRUNCATED;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STATE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.TRACE_ID;

import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTrace;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.SpanContext;
import java.time.Duration;
import java.time.Instant;

public class PprofCpuEventExporter implements CpuEventExporter {
  private Duration period;
  private final int stackDepth;
  private final PprofLogDataExporter pprofLogDataExporter;
  private Pprof pprof = createPprof();

  private PprofCpuEventExporter(Builder builder) {
    this.period = builder.period;
    this.stackDepth = builder.stackDepth;
    this.pprofLogDataExporter = new PprofLogDataExporter(builder.otelLogger, ProfilingDataType.CPU);
  }

  @Override
  public void export(
      Thread thread, StackTraceElement[] stackTrace, Instant eventTime, SpanContext spanContext) {
    Sample.Builder sample = Sample.newBuilder();

    pprof.addLabel(sample, THREAD_ID, thread.getId());
    pprof.addLabel(sample, THREAD_NAME, thread.getName());
    pprof.addLabel(sample, THREAD_STATE, thread.getState().name());

    if (stackTrace.length > stackDepth) {
      pprof.addLabel(sample, THREAD_STACK_TRUNCATED, true);
    }

    for (int i = 0; i < Math.min(stackDepth, stackTrace.length); i++) {
      StackTraceElement ste = stackTrace[i];

      String fileName = ste.getFileName();
      if (fileName == null) {
        fileName = "unknown";
      }
      String className = ste.getClassName();
      if (className == null) {
        className = "unknown";
      }
      String methodName = ste.getMethodName();
      if (methodName == null) {
        methodName = "unknown";
      }
      int lineNumber = ste.getLineNumber();
      if (lineNumber < 0) {
        lineNumber = 0;
      }
      sample.addLocationId(pprof.getLocationId(fileName, className, methodName, lineNumber));
      pprof.incFrameCount();
    }

    pprof.addLabel(sample, SOURCE_EVENT_PERIOD, period.toMillis());
    pprof.addLabel(sample, SOURCE_EVENT_TIME, eventTime.toEpochMilli());

    if (spanContext != null && spanContext.isValid()) {
      pprof.addLabel(sample, TRACE_ID, spanContext.getTraceId());
      pprof.addLabel(sample, SPAN_ID, spanContext.getSpanId());
    }

    pprof.getProfileBuilder().addSample(sample);
  }

  @Override
  public void export(StackToSpanLinkage stackToSpanLinkage) {
    StackTrace stackTrace = StackTraceParser.parse(stackToSpanLinkage.getRawStack(), stackDepth);
    if (stackTrace == null || stackTrace.getStackTraceLines().isEmpty()) {
      return;
    }

    Sample.Builder sample = Sample.newBuilder();

    if (stackTrace.getThreadId() != 0) {
      pprof.addLabel(sample, THREAD_ID, stackTrace.getThreadId());
      pprof.addLabel(sample, THREAD_NAME, stackTrace.getThreadName());
    }
    pprof.addLabel(sample, THREAD_STATE, stackTrace.getThreadState());

    if (stackTrace.isTruncated()) {
      pprof.addLabel(sample, THREAD_STACK_TRUNCATED, true);
    }

    for (StackTraceParser.StackTraceLine stl : stackTrace.getStackTraceLines()) {
      sample.addLocationId(
          pprof.getLocationId(
              stl.getLocation(), stl.getClassName(), stl.getMethod(), stl.getLineNumber()));
      pprof.incFrameCount();
    }

    String eventName = stackToSpanLinkage.getSourceEventName();
    pprof.addLabel(sample, SOURCE_EVENT_NAME, eventName);
    pprof.addLabel(sample, SOURCE_EVENT_PERIOD, period.toMillis());
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
    byte[] result = pprof.serialize();
    pprof = createPprof();
    return result;
  }

  @Override
  public void flush() {
    if (!pprof.hasSamples()) {
      return;
    }
    int frameCount = pprof.frameCount();
    // Flush is called after each JFR chunk, hopefully this will keep batch sizes small enough.
    byte[] bytes = serializePprof();
    pprofLogDataExporter.export(bytes, frameCount);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Logger otelLogger;
    private Duration period;
    private int stackDepth;

    public PprofCpuEventExporter build() {
      return new PprofCpuEventExporter(this);
    }

    public Builder otelLogger(Logger otelLogger) {
      this.otelLogger = otelLogger;
      return this;
    }

    public Builder period(Duration period) {
      this.period = period;
      return this;
    }

    public Builder stackDepth(int stackDepth) {
      this.stackDepth = stackDepth;
      return this;
    }
  }
}
