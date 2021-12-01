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
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InstrumentationLibraryLogAdapterTest {

  @Test
  void testApply() {
    Instant now = Instant.now();
    LogDataBuilder builder =
        LogDataBuilder.create(
            Resource.getDefault(), InstrumentationLibraryInfo.create("test", "1.2.3"));
    LogData log1 =
        builder
            .setEpoch(now.plus(0, MINUTES))
            .setAttributes(Attributes.of(HTTP_METHOD, "get"))
            .setBody("log1")
            .build();
    LogData log2 =
        builder
            .setEpoch(now.plus(1, MINUTES))
            .setAttributes(Attributes.of(HTTP_METHOD, "put"))
            .setBody("log2")
            .build();
    LogData log3 =
        builder
            .setEpoch(now.plus(2, MINUTES))
            .setAttributes(Attributes.of(HTTP_METHOD, "patch"))
            .setBody("log3")
            .build();
    List<LogData> logsEntries = Arrays.asList(log1, log2, log3);
    LogDataAdapter logDataAdapter = new LogDataAdapter();

    InstrumentationLibraryLogsAdapter adapter =
        InstrumentationLibraryLogsAdapter.builder()
            .instrumentationName("otel-profiling")
            .instrumentationVersion("1.2.3")
            .logDataAdapter(logDataAdapter)
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
    assertEquals(
        TimeUnit.SECONDS.toNanos(time.getEpochSecond()) + time.getNano(),
        logRecord.getTimeUnixNano());
    assertEquals(HTTP_METHOD.getKey(), logRecord.getAttributesList().get(0).getKey());
    assertEquals(method, logRecord.getAttributesList().get(0).getValue().getStringValue());
    assertEquals(body, logRecord.getBody().getStringValue());
  }
}
