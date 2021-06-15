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

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.InstrumentationLibraryLogs;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.sdk.resources.Resource;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Protobuf converter. Turns a LogEntry into a protobuf LogRecord */
public class ResourceLogsAdapter implements Function<List<LogEntry>, ResourceLogs> {

  private final InstrumentationLibraryLogsAdapter logsAdapter;
  private final List<KeyValue> resourceAttributes;

  public ResourceLogsAdapter(InstrumentationLibraryLogsAdapter logsAdapter, Resource resource) {
    this(logsAdapter, convertResourceAttributes(resource));
  }

  ResourceLogsAdapter(
      InstrumentationLibraryLogsAdapter logsAdapter, List<KeyValue> resourceAttributes) {
    this.logsAdapter = logsAdapter;
    this.resourceAttributes = resourceAttributes;
  }

  private static List<KeyValue> convertResourceAttributes(Resource resource) {
    return resource.getAttributes().asMap().entrySet().stream()
        .map(kv -> CommonAdapter.toProtoAttribute(kv.getKey(), kv.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public ResourceLogs apply(List<LogEntry> logEntry) {
    InstrumentationLibraryLogs instrumentationLibraryLogs = logsAdapter.apply(logEntry);
    io.opentelemetry.proto.resource.v1.Resource protoResource =
        io.opentelemetry.proto.resource.v1.Resource.newBuilder()
            .addAllAttributes(resourceAttributes)
            .build();
    return ResourceLogs.newBuilder()
        .setResource(protoResource)
        .addInstrumentationLibraryLogs(instrumentationLibraryLogs)
        .build();
  }
}
