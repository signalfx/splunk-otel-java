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

import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.logs.v1.InstrumentationLibraryLogs;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Protobuf converter. Responsible for turning a list of LogData instances into an instance of
 * InstrumentationLibraryLogs.
 */
public class InstrumentationLibraryLogsAdapter
    implements Function<List<LogData>, InstrumentationLibraryLogs> {

  public final String instrumentationName;
  public final String instrumentationVersion;
  private final LogDataAdapter logDataAdapter;

  private InstrumentationLibraryLogsAdapter(Builder builder) {
    this.instrumentationName = builder.instrumentationName;
    this.instrumentationVersion = builder.instrumentationVersion;
    this.logDataAdapter = builder.logDataAdapter;
  }

  @Override
  public InstrumentationLibraryLogs apply(List<LogData> logEntries) {

    InstrumentationLibrary library =
        InstrumentationLibrary.newBuilder()
            .setName(instrumentationName)
            .setVersion(instrumentationVersion)
            .build();

    List<LogRecord> logs = logEntries.stream().map(logDataAdapter).collect(Collectors.toList());

    return InstrumentationLibraryLogs.newBuilder()
        .setSchemaUrl(ResourceAttributes.SCHEMA_URL)
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
    private LogDataAdapter logDataAdapter;

    public Builder instrumentationName(String instrumentationName) {
      this.instrumentationName = instrumentationName;
      return this;
    }

    public Builder instrumentationVersion(String instrumentationVersion) {
      this.instrumentationVersion = instrumentationVersion;
      return this;
    }

    public Builder logEntryAdapter(LogDataAdapter logDataAdapter) {
      this.logDataAdapter = logDataAdapter;
      return this;
    }

    public InstrumentationLibraryLogsAdapter build() {
      return new InstrumentationLibraryLogsAdapter(this);
    }
  }
}
