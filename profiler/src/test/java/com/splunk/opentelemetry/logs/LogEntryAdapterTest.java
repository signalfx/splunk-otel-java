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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LogEntryAdapterTest {

  @Test
  void testAdapt() {
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("origination"), "MyClass.myMethod(line:123)");
    Instant time = Instant.now();
    String body = "I am a log message!";
    LogEntry entry = basicBuilder(attributes, time, body).build();

    LogEntryAdapter adapter = new LogEntryAdapter();
    LogRecord result = adapter.apply(entry);
    assertEquals(body, result.getBody().getStringValue());

    List<KeyValue> resultAttrs = result.getAttributesList();
    Optional<KeyValue> origination =
        resultAttrs.stream().filter(kv -> kv.getKey().equals("origination")).findFirst();

    assertEquals(entry.getName(), result.getName());
    assertEquals(entry.getTime().toEpochMilli() * 1_000_000L, result.getTimeUnixNano());

    assertEquals(entry.getTraceFlags(), result.getFlags());
    assertEquals(entry.getTraceId(), toHexString(result.getTraceId().toByteArray()));
    assertEquals(entry.getSpanId(), toHexString(result.getSpanId().toByteArray()));
    assertEquals(entry.getBody(), result.getBody().getStringValue());
    assertEquals("MyClass.myMethod(line:123)", origination.get().getValue().getStringValue());
  }

  @Test
  void testNullNameIsValid() {
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("origination"), "MyClass.myMethod(line:123)");
    Instant time = Instant.now();
    LogEntry entry = basicBuilder(attributes, time, "I am a log message!").name(null).build();

    LogEntryAdapter adapter = new LogEntryAdapter();
    LogRecord result = adapter.apply(entry);
    assertEquals("", result.getName());
  }

  private LogEntry.Builder basicBuilder(Attributes attributes, Instant time, String body) {
    return LogEntry.builder()
        .name("__LOG__")
        .time(time)
        .body(body)
        .traceFlags(0x42)
        .traceId("c78bda329abae6a6c7111111112ae666")
        .spanId("c78bda329abae6a6")
        .attributes(attributes);
  }

  private static String toHexString(byte[] bytes) {
    return IntStream.range(0, bytes.length)
        .mapToObj(i -> bytes[i])
        .map(b -> String.format("%02X", b))
        .reduce("", (s1, s2) -> s1 + s2)
        .toLowerCase();
  }
}
