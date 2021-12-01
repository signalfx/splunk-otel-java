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
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.ENDUSER_ID;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.InstrumentationLibraryLogs;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ResourceLogsAdapterTest {

  @Test
  void testApply() {
    Instant now = Instant.now();
    LogDataBuilder builder =
        LogDataBuilder.create(
            Resource.getDefault(), InstrumentationLibraryInfo.create("test", "1.2.3"));
    LogData log1 =
        builder
            .setEpoch(now.plus(0, SECONDS))
            .setAttributes(Attributes.of(ENDUSER_ID, "jimmy"))
            .setBody("test log 1")
            .build();
    LogData log2 =
        builder
            .setEpoch(now.plus(1, SECONDS))
            .setAttributes(Attributes.of(ENDUSER_ID, "sally"))
            .setBody("test log 2")
            .build();
    LogData log3 =
        builder
            .setEpoch(now.plus(2, SECONDS))
            .setAttributes(Attributes.of(ENDUSER_ID, "ward"))
            .setBody("test log 3")
            .build();
    List<LogData> sourceLogs = Arrays.asList(log1, log2, log3);
    LogDataAdapter logDataAdapter = new LogDataAdapter();
    InstrumentationLibraryLogsAdapter instLogsAdapter =
        InstrumentationLibraryLogsAdapter.builder()
            .logDataAdapter(logDataAdapter)
            .instrumentationName("a")
            .instrumentationVersion("b")
            .build();
    List<KeyValue> resourceAttrs =
        Arrays.asList(kv("service.name", "zoom.vroom"), kv("environment.name", "staging"));
    ResourceLogsAdapter adapter = new ResourceLogsAdapter(instLogsAdapter, resourceAttrs);
    ResourceLogs result = adapter.apply(sourceLogs);

    assertEquals(SCHEMA_URL, result.getSchemaUrl());
    List<KeyValue> resultResourceAttrs = result.getResource().getAttributesList();
    assertEquals(2, resultResourceAttrs.size());
    assertEquals("service.name", resultResourceAttrs.get(0).getKey());
    assertEquals("zoom.vroom", resultResourceAttrs.get(0).getValue().getStringValue());
    assertEquals("environment.name", resultResourceAttrs.get(1).getKey());
    assertEquals("staging", resultResourceAttrs.get(1).getValue().getStringValue());
    List<InstrumentationLibraryLogs> resultInstLogsList =
        result.getInstrumentationLibraryLogsList();
    assertEquals(1, resultInstLogsList.size());
    List<LogRecord> logRecords = resultInstLogsList.get(0).getLogsList();
    assertEquals(3, logRecords.size());

    assertLog(logRecords.get(0), now, "jimmy", "test log 1");
    assertLog(logRecords.get(1), now.plus(1, SECONDS), "sally", "test log 2");
    assertLog(logRecords.get(2), now.plus(2, SECONDS), "ward", "test log 3");
  }

  private void assertLog(LogRecord logRecord, Instant time, String username, String body) {
    assertEquals(
        TimeUnit.SECONDS.toNanos(time.getEpochSecond()) + time.getNano(),
        logRecord.getTimeUnixNano());
    assertEquals(body, logRecord.getBody().getStringValue());
    assertEquals(ENDUSER_ID.getKey(), logRecord.getAttributes(0).getKey());
    assertEquals(username, logRecord.getAttributes(0).getValue().getStringValue());
  }

  private KeyValue kv(String k, String v) {
    return KeyValue.newBuilder()
        .setKey(k)
        .setValue(AnyValue.newBuilder().setStringValue(v).build())
        .build();
  }
}
