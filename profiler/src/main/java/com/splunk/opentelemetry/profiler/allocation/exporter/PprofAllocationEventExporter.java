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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_TIME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SPAN_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STACK_TRUNCATED;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STATE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.TRACE_ID;

import com.google.perftools.profiles.ProfileProto;
import com.google.perftools.profiles.ProfileProto.Profile;
import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.EventReader;
import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.exporter.PprofLogDataExporter;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.SpanContext;
import java.time.Instant;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;

public class PprofAllocationEventExporter implements AllocationEventExporter {
  private final EventReader eventReader;
  private final PprofLogDataExporter pprofLogDataExporter;
  private final int stackDepth;
  private Pprof pprof = createPprof();

  private PprofAllocationEventExporter(Builder builder) {
    this.eventReader = builder.eventReader;
    this.stackDepth = builder.stackDepth;
    this.pprofLogDataExporter =
        new PprofLogDataExporter(
            builder.otelLogger, ProfilingDataType.ALLOCATION, InstrumentationSource.CONTINUOUS);
  }

  @Override
  public void export(IItem event, AllocationEventSampler sampler, SpanContext spanContext) {
    IMCStackTrace stackTrace = eventReader.getStackTrace(event);
    if (stackTrace == null) {
      return;
    }

    // ObjectAllocationSample event doesn't have allocationSize using weight instead. Aggregating
    // the weights for a large number of samples, for a particular class, thread or stack trace,
    // gives a statistically accurate representation of the allocation pressure.
    long allocationSize =
        "jdk.ObjectAllocationSample".equals(event.getType().getIdentifier())
            ? eventReader.getSampleWeight(event)
            : eventReader.getAllocationSize(event);

    Sample.Builder sample = Sample.newBuilder();
    sample.addValue(allocationSize);

    if (stackTrace.getTruncationState().isTruncated()
        || stackTrace.getFrames().size() > stackDepth) {
      pprof.addLabel(sample, THREAD_STACK_TRUNCATED, true);
    }

    stackTrace.getFrames().stream()
        // limit the number of stack frames in case jfr stack depth is greater than our stack depth
        // truncate the bottom stack frames the same way as jfr
        .limit(stackDepth)
        .forEachOrdered(
            frame -> {
              IMCMethod method = frame.getMethod();
              if (method == null) {
                sample.addLocationId(pprof.getLocationId("unknown", "unknown", "unknown", 0));
              } else {
                String className = method.getType().getFullName();
                if (className == null) {
                  className = "unknown";
                }
                String methodName = method.getMethodName();
                if (methodName == null) {
                  methodName = "unknown";
                }
                Integer lineNumber = frame.getFrameLineNumber();
                sample.addLocationId(
                    pprof.getLocationId(
                        "unknown", // file name is not known
                        className,
                        methodName,
                        lineNumber != null && lineNumber != -1 ? lineNumber : 0));
              }
              pprof.incFrameCount();
            });

    String eventName = event.getType().getIdentifier();
    pprof.addLabel(sample, SOURCE_EVENT_NAME, eventName);
    Instant time = eventReader.getStartInstant(event);
    pprof.addLabel(sample, SOURCE_EVENT_TIME, time.toEpochMilli());

    IMCThread thread = eventReader.getThread(event);
    if (thread != null) {
      if (thread.getThreadId() != null) {
        pprof.addLabel(sample, THREAD_ID, thread.getThreadId());
        pprof.addLabel(sample, THREAD_NAME, thread.getThreadName());
      }
    }
    pprof.addLabel(sample, THREAD_STATE, "RUNNABLE");

    if (spanContext != null && spanContext.isValid()) {
      pprof.addLabel(sample, TRACE_ID, spanContext.getTraceId());
      pprof.addLabel(sample, SPAN_ID, spanContext.getSpanId());
    }
    if (sampler != null) {
      sampler.addAttributes(
          (k, v) -> pprof.addLabel(sample, k, v), (k, v) -> pprof.addLabel(sample, k, v));
    }

    pprof.getProfileBuilder().addSample(sample);
  }

  private static Pprof createPprof() {
    Pprof pprof = new Pprof();
    Profile.Builder profile = pprof.getProfileBuilder();
    profile.addSampleType(
        ProfileProto.ValueType.newBuilder()
            .setType(pprof.getStringId("allocationSize"))
            .setUnit(pprof.getStringId("bytes"))
            .build());

    return pprof;
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
    private EventReader eventReader;
    private Logger otelLogger;
    private int stackDepth;

    public PprofAllocationEventExporter build() {
      return new PprofAllocationEventExporter(this);
    }

    public Builder eventReader(EventReader eventReader) {
      this.eventReader = eventReader;
      return this;
    }

    public Builder otelLogger(Logger otelLogger) {
      this.otelLogger = otelLogger;
      return this;
    }

    public Builder stackDepth(int stackDepth) {
      this.stackDepth = stackDepth;
      return this;
    }
  }
}
