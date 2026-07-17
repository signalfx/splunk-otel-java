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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.LOCK_HELD_PREFIX;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.LOCK_OWNER_THREAD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.LOCK_WAITING_ON;
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
import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTrace;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.time.Instant;

public class PprofCpuEventExporter implements CpuEventExporter {
  private final Duration period;
  private final int stackDepth;
  private final PprofLogDataExporter pprofLogDataExporter;
  private Pprof pprof = createPprof();

  private PprofCpuEventExporter(Builder builder) {
    this.period = builder.period;
    this.stackDepth = builder.stackDepth;
    this.pprofLogDataExporter =
        new PprofLogDataExporter(
            builder.otelLogger, ProfilingDataType.CPU, builder.instrumentationSource);
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

  @Override
  public void export(
      ThreadInfo threadInfo, Instant eventTime, String traceId, String spanId, Duration duration) {
    Sample.Builder sample = Sample.newBuilder();
    addThreadInfo(
        sample, threadInfo.getThreadId(), threadInfo.getThreadName(), threadInfo.getThreadState());
    addLockInfo(sample, threadInfo);
    addSample(sample, threadInfo.getStackTrace(), eventTime, traceId, spanId, duration);
  }

  @Override
  public void export(
      long threadId,
      String threadName,
      Thread.State threadState,
      StackTraceElement[] stackTrace,
      Instant eventTime,
      String traceId,
      String spanId,
      Duration duration) {
    Sample.Builder sample = Sample.newBuilder();
    addThreadInfo(sample, threadId, threadName, threadState);
    addSample(sample, stackTrace, eventTime, traceId, spanId, duration);
  }

  private void addThreadInfo(
      Sample.Builder sample, long threadId, String threadName, Thread.State threadState) {
    pprof.addLabel(sample, THREAD_ID, threadId);
    pprof.addLabel(sample, THREAD_NAME, threadName);
    pprof.addLabel(sample, THREAD_STATE, threadState.name());
  }

  private void addSample(
      Sample.Builder sample,
      StackTraceElement[] stackTrace,
      Instant eventTime,
      String traceId,
      String spanId,
      Duration duration) {
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
      String methodName = ste.getMethodName();
      int lineNumber = Math.max(ste.getLineNumber(), 0);
      sample.addLocationId(pprof.getLocationId(fileName, className, methodName, lineNumber));
      pprof.incFrameCount();
    }

    pprof.addLabel(sample, SOURCE_EVENT_PERIOD, duration.toMillis());
    pprof.addLabel(sample, SOURCE_EVENT_TIME, eventTime.toEpochMilli());

    if (TraceId.isValid(traceId)) {
      pprof.addLabel(sample, TRACE_ID, traceId);
    }
    if (SpanId.isValid(spanId)) {
      pprof.addLabel(sample, SPAN_ID, spanId);
    }

    pprof.getProfileBuilder().addSample(sample);
  }

  private void addLockInfo(Sample.Builder sample, ThreadInfo threadInfo) {
    LockInfo waitingOn = threadInfo.getLockInfo();
    if (waitingOn != null) {
      pprof.addLabel(sample, LOCK_WAITING_ON, formatLock(waitingOn));
    }
    pprof.addLabel(sample, LOCK_OWNER_THREAD, threadInfo.getLockOwnerName());

    int heldLockIndex = 0;
    for (MonitorInfo monitor : threadInfo.getLockedMonitors()) {
      pprof.addLabel(sample, LOCK_HELD_PREFIX + heldLockIndex++, formatLock(monitor));
    }
    for (LockInfo synchronizer : threadInfo.getLockedSynchronizers()) {
      pprof.addLabel(sample, LOCK_HELD_PREFIX + heldLockIndex++, formatLock(synchronizer));
    }
  }

  private static String formatLock(LockInfo lockInfo) {
    return lockInfo.getClassName() + '@' + Integer.toHexString(lockInfo.getIdentityHashCode());
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
    private InstrumentationSource instrumentationSource = InstrumentationSource.CONTINUOUS;

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

    public Builder instrumentationSource(InstrumentationSource instrumentationSource) {
      this.instrumentationSource = instrumentationSource;
      return this;
    }
  }
}
