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

import com.google.protobuf.ByteString;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns a LogEntry into a protobuf LogRecord, ready for exporting */
public class LogEntryAdapter implements Function<LogEntry, LogRecord> {

  @Override
  public LogRecord apply(LogEntry logEntry) {
    AnyValue body = AnyValue.newBuilder().setStringValue(logEntry.getBody()).build();
    Attributes sourceAttrs = logEntry.getAttributes();
    List<KeyValue> attributes =
        sourceAttrs.asMap().entrySet().stream()
            .map(kv -> CommonAdapter.toProtoAttribute(kv.getKey(), kv.getValue()))
            .collect(Collectors.toList());

    LogRecord.Builder builder =
        LogRecord.newBuilder()
            .addAllAttributes(attributes)
            .setBody(body)
            .setTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(logEntry.getTime().toEpochMilli()));

    if (logEntry.getName() != null) {
      builder.setName(logEntry.getName());
    }
    if (logEntry.getTraceId() != null) {
      builder.setTraceId(ByteString.copyFromUtf8(logEntry.getTraceId()));
    }
    if (logEntry.getSpanId() != null) {
      builder.setSpanId(ByteString.copyFromUtf8(logEntry.getSpanId()));
    }
    return builder.build();
  }
}
