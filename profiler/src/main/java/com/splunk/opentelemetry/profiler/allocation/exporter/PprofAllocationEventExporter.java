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

import static com.splunk.opentelemetry.profiler.LogDataCreator.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.LogExporterBuilder.INSTRUMENTATION_LIBRARY_INFO;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static com.splunk.opentelemetry.profiler.pprof.PprofAttributeKeys.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.pprof.PprofAttributeKeys.DATA_TYPE;

import com.google.perftools.profiles.ProfileProto;
import com.google.perftools.profiles.ProfileProto.Profile;
import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.Configuration.DataFormat;
import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PprofAllocationEventExporter implements AllocationEventExporter {
  private static final Logger logger = LoggerFactory.getLogger(PprofAllocationEventExporter.class);

  private final LogProcessor logProcessor;
  private final Resource resource;
  private final DataFormat allocationDataFormat;
  private final EventPeriods eventPeriods;
  private Pprof pprof = createPprof();

  private PprofAllocationEventExporter(Builder builder) {
    this.logProcessor = builder.logProcessor;
    this.resource = builder.resource;
    this.allocationDataFormat = builder.allocationDataFormat;
    this.eventPeriods = builder.eventPeriods;
  }

  @Override
  public void export(RecordedEvent event, AllocationEventSampler sampler, SpanContext spanContext) {
    RecordedStackTrace stackTrace = event.getStackTrace();
    if (stackTrace == null) {
      return;
    }

    long allocationSize = event.getLong("allocationSize");

    Sample.Builder sample = Sample.newBuilder();
    sample.addValue(allocationSize);
    // XXX StackSerializer limits stack depth. Although I believe jfr also limits it should we
    // attempt to limit the depth here too?
    for (RecordedFrame frame : stackTrace.getFrames()) {
      RecordedMethod method = frame.getMethod();
      if (method == null) {
        sample.addLocationId(pprof.getLocationId("unknown", "unknown.unknown", -1));
      } else {
        sample.addLocationId(
            pprof.getLocationId(
                "unknown", // file name is not known
                method.getType().getName() + "." + method.getName(),
                frame.getLineNumber()));
      }
    }

    String eventName = event.getEventType().getName();
    pprof.addLabel(sample, SOURCE_EVENT_NAME.getKey(), eventName);
    Duration eventPeriod = eventPeriods.getDuration(eventName);
    if (!EventPeriods.UNKNOWN.equals(eventPeriod)) {
      pprof.addLabel(sample, SOURCE_EVENT_PERIOD.getKey(), eventPeriod.toMillis());
    }
    Instant time = event.getStartTime();
    pprof.addLabel(sample, "source.event.time", time.toEpochMilli());

    RecordedThread thread = event.getThread();
    pprof.addLabel(sample, "thread.id", thread.getJavaThreadId());
    pprof.addLabel(sample, "thread.name", thread.getJavaName());

    if (spanContext != null && spanContext.isValid()) {
      pprof.addLabel(sample, "trace_id", spanContext.getTraceId());
      pprof.addLabel(sample, "span_id", spanContext.getSpanId());
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
    byte[] result = pprof.serialize(allocationDataFormat);
    pprof = createPprof();
    return result;
  }

  @Override
  public void flush() {
    // Flush is called after each JFR chunk, hopefully this will keep batch sizes small enough.
    byte[] bytes = serializePprof();
    String format = allocationDataFormat.toString().toLowerCase(Locale.ROOT).replace('_', '-');

    // XXX just to give an overview of exported data size
    logger.info("Exporting {}, size {}", format, bytes.length);

    Attributes attributes =
        Attributes.builder()
            .put(SOURCE_TYPE, PROFILING_SOURCE)
            .put(DATA_TYPE, "allocation") // tells that this message is about allocation samples
            .put(DATA_FORMAT, format) // data format
            .build();

    String body = new String(bytes, StandardCharsets.ISO_8859_1);
    LogDataBuilder logDataBuilder =
        LogDataBuilder.create(resource, INSTRUMENTATION_LIBRARY_INFO)
            .setEpoch(Instant.now())
            .setBody(body)
            .setAttributes(attributes);

    logProcessor.emit(logDataBuilder.build());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LogProcessor logProcessor;
    private Resource resource;
    private DataFormat allocationDataFormat;
    private EventPeriods eventPeriods;

    public PprofAllocationEventExporter build() {
      return new PprofAllocationEventExporter(this);
    }

    public Builder logProcessor(LogProcessor logsProcessor) {
      this.logProcessor = logsProcessor;
      return this;
    }

    public Builder resource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public Builder allocationDataFormat(DataFormat allocationDataFormat) {
      this.allocationDataFormat = allocationDataFormat;
      return this;
    }

    public Builder eventPeriods(EventPeriods eventPeriods) {
      this.eventPeriods = eventPeriods;
      return this;
    }
  }
}
