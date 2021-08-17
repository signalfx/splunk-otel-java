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

package com.splunk.opentelemetry.profiler;

import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class TestHelpers {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static boolean isThreadDumpEvent(LogRecord log) {
    String name = getStringAttr(log, "source.event.name");
    return "jdk.ThreadDump".equals(name);
  }

  public static boolean isTLABEvent(LogRecord log) {
    String name = getStringAttr(log, "source.event.name");
    return "jdk.ObjectAllocationInNewTLAB".equals(name)
        || "jdk.ObjectAllocationOutsideTLAB".equals(name);
  }

  static List<LogRecord> flattenToLogRecords(List<ExportLogsServiceRequest> resourceLogs) {
    return resourceLogs.stream()
        .flatMap(log -> log.getResourceLogsList().stream())
        .flatMap(log -> log.getInstrumentationLibraryLogsList().stream())
        .flatMap(log -> log.getLogsList().stream())
        .collect(Collectors.toList());
  }

  static String getStringAttr(LogRecord log, String name) {
    Optional<KeyValue> foundAttr = findAttr(log, name);
    return foundAttr.map(attr -> attr.getValue().getStringValue()).orElse(null);
  }

  static Long getLongAttr(LogRecord log, String name) {
    Optional<KeyValue> foundAttr = findAttr(log, name);
    return foundAttr.map(attr -> attr.getValue().getIntValue()).orElse(null);
  }

  static Optional<KeyValue> findAttr(LogRecord log, String name) {
    return log.getAttributesList().stream().filter(a -> a.getKey().equals(name)).findFirst();
  }

  static List<ExportLogsServiceRequest> parseToExportLogsServiceRequests(String bodyContent)
      throws IOException {
    JsonNode root = OBJECT_MAPPER.readTree(bodyContent);
    return StreamSupport.stream(root.spliterator(), false)
        .map(TestHelpers::deserializeJsonToObject)
        .collect(Collectors.toList());
  }

  private static ExportLogsServiceRequest deserializeJsonToObject(JsonNode node) {
    ExportLogsServiceRequest.Builder builder = ExportLogsServiceRequest.newBuilder();
    try {
      JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(node), builder);
      return builder.build();
    } catch (Exception e) {
      fail("Error mapping log results", e);
      return null;
    }
  }
}
