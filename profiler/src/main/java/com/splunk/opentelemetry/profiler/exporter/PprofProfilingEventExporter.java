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

import static com.splunk.opentelemetry.profiler.LogExporterBuilder.INSTRUMENTATION_LIBRARY_INFO;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static com.splunk.opentelemetry.profiler.pprof.PprofAttributeKeys.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.pprof.PprofAttributeKeys.DATA_TYPE;

import com.google.perftools.profiles.ProfileProto.Label;
import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.Configuration.DataFormat;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PprofProfilingEventExporter implements ProfilingEventExporter {
  private static final Logger logger = LoggerFactory.getLogger(PprofProfilingEventExporter.class);

  private final LogProcessor logProcessor;
  private final Resource resource;
  private final DataFormat profilingDataFormat;
  private final EventPeriods eventPeriods;
  private Pprof pprof = createPprof();

  private PprofProfilingEventExporter(Builder builder) {
    this.logProcessor = builder.logProcessor;
    this.resource = builder.resource;
    this.profilingDataFormat = builder.profilingDataFormat;
    this.eventPeriods = builder.eventPeriods;
  }

  @Override
  public void export(StackToSpanLinkage stackToSpanLinkage) {
    StackTrace st = handleStackTrace(stackToSpanLinkage.getRawStack());
    if (st.stackTraceLines.isEmpty()) {
      return;
    }

    Sample.Builder sample = Sample.newBuilder();
    {
      Label.Builder label = Label.newBuilder();
      // XXX should we attempt to extract relevant attributes here?
      label.setKey(pprof.getStringId("thread.header"));
      label.setStr(pprof.getStringId(st.header));
    }

    {
      // XXX is this needed
      Label.Builder label = Label.newBuilder();
      label.setKey(pprof.getStringId("thread.status"));
      label.setStr(pprof.getStringId(st.status));
    }

    for (StackTraceLine stl : st.stackTraceLines) {
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

  private static StackTrace handleStackTrace(String stackTrace) {
    // \\R - Any Unicode linebreak sequence
    String[] lines = stackTrace.split("\\R");
    String header = lines[0];
    String status = lines[1];
    List<StackTraceLine> stackTraceLines = new ArrayList<>();
    for (int i = 2; i < lines.length; i++) {
      StackTraceLine stl = handleStackTraceLine(lines[i]);
      if (stl != null) {
        stackTraceLines.add(stl);
      }
    }

    return new StackTrace(header, status, stackTraceLines);
  }

  private static StackTraceLine handleStackTraceLine(String line) {
    // we expect the stack trace line to look like
    // at java.lang.Thread.run(java.base@11.0.9.1/Thread.java:834)
    if (!line.startsWith("\tat ")) {
      return null;
    }
    if (!line.endsWith(")")) {
      return null;
    }
    // remove "\tat " and trailing ")"
    line = line.substring(4, line.length() - 1);
    int i = line.lastIndexOf('(');
    if (i == -1) {
      return null;
    }
    String classAndMethod = line.substring(0, i);
    String location = line.substring(i + 1);

    i = location.indexOf('/');
    if (i != -1) {
      location = location.substring(i + 1);
    }

    int lineNumber = -1;
    i = location.indexOf(':');
    if (i != -1) {
      try {
        lineNumber = Integer.parseInt(location.substring(i + 1));
      } catch (NumberFormatException ignored) {
      }
      location = location.substring(0, i);
    }

    return new StackTraceLine(classAndMethod, location, lineNumber);
  }

  private static class StackTrace {
    final String header;
    final String status;
    final List<StackTraceLine> stackTraceLines;

    StackTrace(String header, String status, List<StackTraceLine> stackTraceLines) {
      this.header = header;
      this.status = status;
      this.stackTraceLines = stackTraceLines;
    }
  }

  private static class StackTraceLine {
    final String classAndMethod;
    final String location;
    final int lineNumber;

    StackTraceLine(String classAndMethod, String location, int lineNumber) {
      this.classAndMethod = classAndMethod;
      this.location = location;
      this.lineNumber = lineNumber;
    }
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
    String format = profilingDataFormat.toString().toLowerCase(Locale.ROOT).replace('_', '-');

    // XXX just to give an overview of exported data size
    logger.info("Exporting {}, size {}", format, bytes.length);

    Attributes attributes =
        Attributes.builder()
            .put(SOURCE_TYPE, PROFILING_SOURCE)
            .put(DATA_TYPE, "profiling") // tells that this message is about allocation samples
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
