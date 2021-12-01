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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LogDataAdapterTest {

  @Test
  void testAdapt() {
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("origination"), "MyClass.myMethod(line:123)");
    Instant time = Instant.now();
    String body = "I am a log message!";
    LogData logData = basicBuilder(attributes, time, body).build();

    LogDataAdapter adapter = new LogDataAdapter();
    LogRecord result = adapter.apply(logData);
    assertEquals(body, result.getBody().getStringValue());

    List<KeyValue> resultAttrs = result.getAttributesList();
    Optional<KeyValue> origination =
        resultAttrs.stream().filter(kv -> kv.getKey().equals("origination")).findFirst();

    assertEquals(logData.getName(), result.getName());
    assertEquals(logData.getEpochNanos(), result.getTimeUnixNano());

    assertEquals(
        logData.getSpanContext().getTraceId(), toHexString(result.getTraceId().toByteArray()));
    assertEquals(
        logData.getSpanContext().getSpanId(), toHexString(result.getSpanId().toByteArray()));
    assertEquals(logData.getSpanContext().getTraceFlags().asByte(), result.getFlags());
    assertEquals(logData.getBody().asString(), result.getBody().getStringValue());
    assertEquals("MyClass.myMethod(line:123)", origination.get().getValue().getStringValue());
  }

  @Test
  void testNullNameIsValid() {
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("origination"), "MyClass.myMethod(line:123)");
    Instant time = Instant.now();
    LogData entry = basicBuilder(attributes, time, "I am a log message!").setName(null).build();

    LogDataAdapter adapter = new LogDataAdapter();
    LogRecord result = adapter.apply(entry);
    assertEquals("", result.getName());
  }

  private LogDataBuilder basicBuilder(Attributes attributes, Instant time, String body) {
    LogDataBuilder builder =
        LogDataBuilder.create(
            Resource.getDefault(), InstrumentationLibraryInfo.create("test", "1.2.3"));
    return builder
        .setName("__LOG__")
        .setEpoch(time)
        .setBody(body)
        .setContext(
            Context.root()
                .with(
                    Span.wrap(
                        SpanContext.create(
                            "c78bda329abae6a6c7111111112ae666",
                            "c78bda329abae6a6",
                            TraceFlags.getSampled(),
                            TraceState.getDefault()))))
        .setAttributes(attributes);
  }

  private static String toHexString(byte[] bytes) {
    return IntStream.range(0, bytes.length)
        .mapToObj(i -> bytes[i])
        .map(b -> String.format("%02X", b))
        .reduce("", (s1, s2) -> s1 + s2)
        .toLowerCase();
  }
}
