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

package com.splunk.opentelemetry;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SPAN_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.TRACE_ID;

import com.google.perftools.profiles.ProfileProto;
import com.splunk.opentelemetry.profiler.Configuration;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class LogsInspector {

  private final List<ExportLogsServiceRequest> logRequests;
  private final List<ProfilerSample> cpuSamples = new ArrayList<>();
  private final List<ProfilerSample> memorySamples = new ArrayList<>();

  public LogsInspector(List<ExportLogsServiceRequest> logRequests) {
    this.logRequests = logRequests;

    Stream<LogRecord> profilingLogs =
        getLogStream().filter(log -> PROFILING_SOURCE.equals(getStringAttr(log, SOURCE_TYPE)));
    profilingLogs.forEach(this::parseLogRecord);
  }

  private List<ProfilerSample> getSamplesList(ProfilingDataType profilingDataType) {
    switch (profilingDataType) {
      case CPU:
        return cpuSamples;
      case ALLOCATION:
        return memorySamples;
      default:
        throw new IllegalStateException("unexpected data tye: " + profilingDataType);
    }
  }

  private void parseLogRecord(LogRecord logRecord) {
    String dataType = getStringAttr(logRecord, DATA_TYPE);
    Objects.requireNonNull(dataType, "Data type not set");
    ProfilingDataType profilingDataType =
        ProfilingDataType.valueOf(dataType.toUpperCase(Locale.ROOT));
    List<ProfilerSample> samples = getSamplesList(profilingDataType);

    String dataFormatAttr = getStringAttr(logRecord, ProfilingSemanticAttributes.DATA_FORMAT);
    Objects.requireNonNull(dataFormatAttr, "Data format not set");
    Configuration.DataFormat dataFormat = Configuration.DataFormat.fromString(dataFormatAttr);

    switch (dataFormat) {
      case PPROF_GZIP_BASE64:
        try {
          ProfileProto.Profile profile = parsePprof(logRecord.getBody().getStringValue());
          for (ProfileProto.Sample sample : profile.getSampleList()) {
            samples.add(new PprofProfilerSample(profile, sample));
          }
        } catch (IOException exception) {
          throw new IllegalStateException("Failed to parse pprof", exception);
        }
        break;
      case TEXT:
        samples.add(new TextProfilerSample(logRecord));
        break;
      default:
        throw new IllegalStateException("unexpected data format: " + dataFormat);
    }
  }

  private ProfileProto.Profile parsePprof(String body) throws IOException {
    byte[] bytes = Base64.getDecoder().decode(body);
    try (InputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
      return ProfileProto.Profile.parseFrom(inputStream);
    }
  }

  public Stream<LogRecord> getLogStream() {
    return logRequests.stream()
        .flatMap(log -> log.getResourceLogsList().stream())
        .flatMap(log -> log.getScopeLogsList().stream())
        .flatMap(log -> log.getLogRecordsList().stream());
  }

  public List<ProfilerSample> getCpuSamples() {
    return cpuSamples;
  }

  public List<ProfilerSample> getMemorySamples() {
    return memorySamples;
  }

  public static Predicate<ProfilerSample> hasThreadName(String name) {
    return sample -> name.equals(sample.getThreadName());
  }

  private static String getStringAttr(LogRecord log, AttributeKey<String> key) {
    return getStringAttr(log, key.getKey());
  }

  static String getStringAttr(LogRecord log, String name) {
    Optional<KeyValue> foundAttr = findAttr(log, name);
    return foundAttr.map(attr -> attr.getValue().getStringValue()).orElse(null);
  }

  private static Long getLongAttr(LogRecord log, AttributeKey<Long> key) {
    return getLongAttr(log, key.getKey());
  }

  private static Long getLongAttr(LogRecord log, String name) {
    Optional<KeyValue> foundAttr = findAttr(log, name);
    return foundAttr.map(attr -> attr.getValue().getIntValue()).orElse(null);
  }

  private static Optional<KeyValue> findAttr(LogRecord log, String name) {
    return log.getAttributesList().stream().filter(a -> a.getKey().equals(name)).findFirst();
  }

  public interface ProfilerSample {
    long getSourceEventPeriod();

    String getThreadName();

    Long getAllocated();

    String getTraceId();

    String getSpanId();
  }

  public class TextProfilerSample implements ProfilerSample {
    private final LogRecord logRecord;

    TextProfilerSample(LogRecord logRecord) {
      this.logRecord = logRecord;
    }

    @Override
    public long getSourceEventPeriod() {
      return getLongAttr(logRecord, SOURCE_EVENT_PERIOD);
    }

    @Override
    public String getThreadName() {
      if (!logRecord.hasBody()) {
        return null;
      }
      // body starts with quoted thread name
      String body = logRecord.getBody().getStringValue();
      if (!body.startsWith("\"")) {
        return null;
      }
      int nameEnd = body.indexOf("\"", 1);
      if (nameEnd == -1) {
        return null;
      }
      return body.substring(1, nameEnd);
    }

    @Override
    public Long getAllocated() {
      return getLongAttr(logRecord, "memory.allocated");
    }

    @Override
    public String getTraceId() {
      return TraceId.fromBytes(logRecord.getTraceId().toByteArray());
    }

    @Override
    public String getSpanId() {
      return SpanId.fromBytes(logRecord.getSpanId().toByteArray());
    }
  }

  public class PprofProfilerSample implements ProfilerSample {
    private final ProfileProto.Profile profile;
    private final ProfileProto.Sample sample;
    private final Map<String, ProfileProto.Label> labels = new HashMap<>();

    PprofProfilerSample(ProfileProto.Profile profile, ProfileProto.Sample sample) {
      this.profile = profile;
      this.sample = sample;

      for (ProfileProto.Label label : sample.getLabelList()) {
        String key = profile.getStringTable((int) label.getKey());
        Objects.requireNonNull(key, "String table entry missing for label key");
        labels.put(key, label);
      }
    }

    @Override
    public long getSourceEventPeriod() {
      ProfileProto.Label label = labels.get(SOURCE_EVENT_PERIOD.getKey());
      return label != null ? label.getNum() : -1;
    }

    private String getStringLabel(AttributeKey<String> key) {
      ProfileProto.Label label = labels.get(key.getKey());
      return label != null ? profile.getStringTable((int) label.getStr()) : "";
    }

    @Override
    public String getThreadName() {
      return getStringLabel(THREAD_NAME);
    }

    @Override
    public Long getAllocated() {
      if (sample.getValueCount() == 0) {
        return null;
      }
      return sample.getValue(0);
    }

    @Override
    public String getTraceId() {
      return getStringLabel(TRACE_ID);
    }

    @Override
    public String getSpanId() {
      return getStringLabel(SPAN_ID);
    }
  }
}
