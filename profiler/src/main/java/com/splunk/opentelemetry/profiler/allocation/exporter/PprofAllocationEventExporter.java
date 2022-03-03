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

import static com.google.perftools.profiles.ProfileProto.*;
import static com.splunk.opentelemetry.profiler.LogDataCreator.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.LogExporterBuilder.INSTRUMENTATION_LIBRARY_INFO;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.splunk.opentelemetry.profiler.Configuration.AllocationDataFormat;
import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;
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
  private final AllocationDataFormat allocationDataFormat;
  private final EventPeriods eventPeriods;
  private ProfileData profileData = new ProfileData();

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

    Profile.Builder profile = profileData.profile;
    LocationTable locationTable = profileData.locationTable;
    StringTable stringTable = profileData.stringTable;

    long allocationSize = event.getLong("allocationSize");

    Sample.Builder sample = Sample.newBuilder();
    sample.addValue(allocationSize);
    // XXX StackSerializer limits stack depth. Although I believe jfr also limits it should we
    // attempt to limit the depth here too?
    for (RecordedFrame frame : stackTrace.getFrames()) {
      RecordedMethod method = frame.getMethod();
      if (method == null) {
        sample.addLocationId(locationTable.get("", "unknown.unknown", -1));
      } else {
        sample.addLocationId(
            locationTable.get(
                "unknown", // file name is not known
                method.getType().getName() + "." + method.getName(),
                frame.getLineNumber()));
      }
    }

    String eventName = event.getEventType().getName();
    addLabel(sample, SOURCE_EVENT_NAME.getKey(), event.getEventType().getName());
    Duration eventPeriod = eventPeriods.getDuration(eventName);
    if (!EventPeriods.UNKNOWN.equals(eventPeriod)) {
      addLabel(sample, SOURCE_EVENT_PERIOD.getKey(), eventPeriod.toMillis());
    }
    Instant time = event.getStartTime();
    addLabel(sample, "source.event.time", time.toEpochMilli());

    RecordedThread thread = event.getThread();
    addLabel(sample, "thread.id", thread.getJavaThreadId());
    addLabel(sample, "thread.name", thread.getJavaName());

    if (spanContext != null && spanContext.isValid()) {
      addLabel(sample, "trace_id", spanContext.getTraceId());
      addLabel(sample, "span_id", spanContext.getSpanId());
    }
    if (sampler != null) {
      sampler.addAttributes((k, v) -> addLabel(sample, k, v), (k, v) -> addLabel(sample, k, v));
    }
    // XXX are there any more attributes that should be added to the sample?

    profile.addSample(sample);
  }

  private void addLabel(Sample.Builder sample, String name, String value) {
    StringTable stringTable = profileData.stringTable;

    addLabel(sample, name, label -> label.setStr(stringTable.get(value)));
  }

  private void addLabel(Sample.Builder sample, String name, long value) {
    addLabel(sample, name, label -> label.setNum(value));
  }

  private void addLabel(Sample.Builder sample, String name, Consumer<Label.Builder> valueSetter) {
    StringTable stringTable = profileData.stringTable;

    Label.Builder label = Label.newBuilder();
    label.setKey(stringTable.get(name));
    valueSetter.accept(label);
    sample.addLabel(label.build());
  }

  private byte[] serializePprof() {
    Profile profile = profileData.profile.build();
    profileData = new ProfileData();

    try {
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      boolean gzip =
          allocationDataFormat == AllocationDataFormat.PPROF_GZIP
              || allocationDataFormat == AllocationDataFormat.PPROF_GZIP_BASE64;
      try (OutputStream outputStream = gzip ? new GZIPOutputStream(byteStream) : byteStream) {
        profile.writeTo(outputStream);
      }
      byte[] bytes = byteStream.toByteArray();
      if (allocationDataFormat == AllocationDataFormat.PPROF_BASE64
          || allocationDataFormat == AllocationDataFormat.PPROF_GZIP_BASE64) {
        bytes = Base64.getEncoder().encode(bytes);
      }
      return bytes;
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to serialize pprof", exception);
    }
  }

  public static final AttributeKey<String> DATA_TYPE = stringKey("com.splunk.profiling.data.type");
  public static final AttributeKey<String> DATA_FORMAT =
      stringKey("com.splunk.profiling.data.format");

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
    private AllocationDataFormat allocationDataFormat;
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

    public Builder allocationDataFormat(AllocationDataFormat allocationDataFormat) {
      this.allocationDataFormat = allocationDataFormat;
      return this;
    }

    public Builder eventPeriods(EventPeriods eventPeriods) {
      this.eventPeriods = eventPeriods;
      return this;
    }
  }

  private static class ProfileData {
    Profile.Builder profile = Profile.newBuilder();
    StringTable stringTable = new StringTable(profile);
    FunctionTable functionTable = new FunctionTable(profile, stringTable);
    LocationTable locationTable = new LocationTable(profile, functionTable);

    ProfileData() {
      profile.addSampleType(
          ValueType.newBuilder()
              .setType(stringTable.get("allocationSize"))
              .setUnit(stringTable.get("bytes"))
              .build());
    }
  }

  // copied from
  // https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/profiler/memory/AllocationTracker.java
  private static class StringTable {
    final Profile.Builder profile;
    final Map<String, Long> table = new HashMap<>();
    long index = 0;

    StringTable(Profile.Builder profile) {
      this.profile = profile;
      get(""); // 0 is reserved for the empty string
    }

    long get(String str) {
      return table.computeIfAbsent(
          str,
          key -> {
            profile.addStringTable(key);
            return index++;
          });
    }
  }

  private static class FunctionTable {
    final Profile.Builder profile;
    final StringTable stringTable;
    final Map<String, Long> table = new HashMap<>();
    long index = 1; // 0 is reserved

    FunctionTable(Profile.Builder profile, StringTable stringTable) {
      this.profile = profile;
      this.stringTable = stringTable;
    }

    long get(String file, String function) {
      return table.computeIfAbsent(
          file + "#" + function,
          key -> {
            Function fn =
                Function.newBuilder()
                    .setId(index)
                    .setFilename(stringTable.get(file))
                    .setName(stringTable.get(function))
                    .build();
            profile.addFunction(fn);
            return index++;
          });
    }
  }

  private static class LocationTable {
    final Profile.Builder profile;
    final FunctionTable functionTable;
    final Map<String, Long> table = new HashMap<>();
    long index = 1; // 0 is reserved

    LocationTable(Profile.Builder profile, FunctionTable functionTable) {
      this.profile = profile;
      this.functionTable = functionTable;
    }

    long get(String file, String function, long line) {
      return table.computeIfAbsent(
          file + "#" + function + "#" + line,
          key -> {
            Location location =
                Location.newBuilder()
                    .setId(index)
                    .addLine(
                        Line.newBuilder()
                            .setFunctionId(functionTable.get(file, function))
                            .setLine(line)
                            .build())
                    .build();
            profile.addLocation(location);
            return index++;
          });
    }
  }
}
