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

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SCHEMA_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.logs.v1.InstrumentationLibraryLogs;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstrumentationLibraryLogAdapterTest {

  @Test
  void testApply() {
    Instant now = Instant.now();
    LogEntry log1 =
        LogEntry.builder()
            .time(now.plus(0, MINUTES))
            .attributes(Attributes.of(HTTP_METHOD, "get"))
            .body("log1")
            .build();
    LogEntry log2 =
        LogEntry.builder()
            .time(now.plus(1, MINUTES))
            .attributes(Attributes.of(HTTP_METHOD, "put"))
            .body("log2")
            .build();
    LogEntry log3 =
        LogEntry.builder()
            .time(now.plus(2, MINUTES))
            .attributes(Attributes.of(HTTP_METHOD, "patch"))
            .body("log3")
            .build();
    List<LogEntry> logsEntries = Arrays.asList(log1, log2, log3);
    LogEntryAdapter logEntryAdapter = new LogEntryAdapter();

    InstrumentationLibraryLogsAdapter adapter =
        InstrumentationLibraryLogsAdapter.builder()
            .instrumentationName("otel-profiling")
            .instrumentationVersion("1.2.3")
            .logEntryAdapter(logEntryAdapter)
            .build();
    InstrumentationLibraryLogs result = adapter.apply(logsEntries);

    List<LogRecord> resultLogs = result.getLogsList();
    assertEquals("otel-profiling", result.getInstrumentationLibrary().getName());
    assertEquals(SCHEMA_URL, result.getSchemaUrl());
    assertEquals("1.2.3", result.getInstrumentationLibrary().getVersion());
    assertEquals(3, resultLogs.size());
    assertLog(resultLogs.get(0), now.plus(0, MINUTES), "get", "log1");
    assertLog(resultLogs.get(1), now.plus(1, MINUTES), "put", "log2");
    assertLog(resultLogs.get(2), now.plus(2, MINUTES), "patch", "log3");
  }

  private void assertLog(LogRecord logRecord, Instant time, String method, String body) {
    assertEquals(time.toEpochMilli() * 1_000_000, logRecord.getTimeUnixNano());
    assertEquals(HTTP_METHOD.getKey(), logRecord.getAttributesList().get(0).getKey());
    assertEquals(method, logRecord.getAttributesList().get(0).getValue().getStringValue());
    assertEquals(body, logRecord.getBody().getStringValue());
  }
}
