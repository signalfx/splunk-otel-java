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

package com.splunk.opentelemetry.logs;

import com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.logs.v1.InstrumentationLibraryLogs;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Protobuf converter. Responsible for turning a list of LogEntry instances into an instance of
 * InstrumentationLibraryLogs.
 */
public class InstrumentationLibraryLogsAdapter
    implements Function<List<LogEntry>, InstrumentationLibraryLogs> {

  public final String instrumentationName;
  public final String instrumentationVersion;
  private final LogEntryAdapter logEntryAdapter;

  private InstrumentationLibraryLogsAdapter(Builder builder) {
    this.instrumentationName = builder.instrumentationName;
    this.instrumentationVersion = builder.instrumentationVersion;
    this.logEntryAdapter = builder.logEntryAdapter;
  }

  @Override
  public InstrumentationLibraryLogs apply(List<LogEntry> logEntries) {

    InstrumentationLibrary library =
        InstrumentationLibrary.newBuilder()
            .setName(instrumentationName)
            .setVersion(instrumentationVersion)
            .build();

    List<LogRecord> logs = logEntries.stream().map(logEntryAdapter).collect(Collectors.toList());

    return InstrumentationLibraryLogs.newBuilder()
        .setSchemaUrl(ProfilingSemanticAttributes.SCHEMA_URL)
        .setInstrumentationLibrary(library)
        .addAllLogs(logs)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String instrumentationName;
    private String instrumentationVersion;
    private LogEntryAdapter logEntryAdapter;

    public Builder instrumentationName(String instrumentationName) {
      this.instrumentationName = instrumentationName;
      return this;
    }

    public Builder instrumentationVersion(String instrumentationVersion) {
      this.instrumentationVersion = instrumentationVersion;
      return this;
    }

    public Builder logEntryAdapter(LogEntryAdapter logEntryAdapter) {
      this.logEntryAdapter = logEntryAdapter;
      return this;
    }

    public InstrumentationLibraryLogsAdapter build() {
      return new InstrumentationLibraryLogsAdapter(this);
    }
  }
}
