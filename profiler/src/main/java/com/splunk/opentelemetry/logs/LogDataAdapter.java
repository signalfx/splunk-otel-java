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

import static com.google.protobuf.UnsafeByteOperations.unsafeWrap;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.sdk.logs.data.LogData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns a LogData into a protobuf LogRecord, ready for exporting */
public class LogDataAdapter implements Function<LogData, LogRecord> {

  @Override
  public LogRecord apply(LogData logData) {
    AnyValue body = AnyValue.newBuilder().setStringValue(logData.getBody().asString()).build();
    Attributes sourceAttrs = logData.getAttributes();
    List<KeyValue> attributes =
        sourceAttrs.asMap().entrySet().stream()
            .map(kv -> CommonAdapter.toProtoAttribute(kv.getKey(), kv.getValue()))
            .collect(Collectors.toList());

    LogRecord.Builder builder =
        LogRecord.newBuilder()
            .addAllAttributes(attributes)
            .setBody(body)
            .setTimeUnixNano(
                TimeUnit.MILLISECONDS.toNanos(
                    TimeUnit.NANOSECONDS.toMillis(logData.getEpochNanos())));

    if (logData.getName() != null) {
      builder.setName(logData.getName());
    }
    SpanContext spanContext = logData.getSpanContext();
    if (spanContext.getTraceId() != null) {
      byte[] traceIdBytes =
          OtelEncodingUtils.bytesFromBase16(spanContext.getTraceId(), TraceId.getLength());
      builder.setTraceId(unsafeWrap(traceIdBytes));
    }
    if (spanContext.getSpanId() != null) {
      byte[] spanIdBytes =
          OtelEncodingUtils.bytesFromBase16(spanContext.getSpanId(), SpanId.getLength());
      builder.setSpanId(unsafeWrap(spanIdBytes));
      builder.setFlags(spanContext.getTraceFlags().asByte());
    }
    return builder.build();
  }
}
