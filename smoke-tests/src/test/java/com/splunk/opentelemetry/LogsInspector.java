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

package com.splunk.opentelemetry;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class LogsInspector {

  private final List<ExportLogsServiceRequest> logRequests;

  public LogsInspector(List<ExportLogsServiceRequest> logRequests) {
    this.logRequests = logRequests;
  }

  public Stream<LogRecord> getLogStream() {
    return logRequests.stream()
        .flatMap(log -> log.getResourceLogsList().stream())
        .flatMap(log -> log.getInstrumentationLibraryLogsList().stream())
        .flatMap(log -> log.getLogsList().stream());
  }

  public Stream<LogRecord> getThreadDumpEvents() {
    return getLogStream().filter(LogsInspector::isThreadDumpEvent);
  }

  public Stream<LogRecord> getTlabEvents() {
    return getLogStream().filter(LogsInspector::isTlabEvent);
  }

  public static Predicate<LogRecord> hasThreadName(String name) {
    return logRecord ->
        logRecord.hasBody() && logRecord.getBody().getStringValue().startsWith("\"" + name + "\"");
  }

  private static boolean isThreadDumpEvent(LogRecord log) {
    String name = getStringAttr(log, "source.event.name");
    return "jdk.ThreadDump".equals(name);
  }

  private static boolean isTlabEvent(LogRecord log) {
    String name = getStringAttr(log, "source.event.name");
    return "jdk.ObjectAllocationInNewTLAB".equals(name)
        || "jdk.ObjectAllocationOutsideTLAB".equals(name);
  }

  private static String getStringAttr(LogRecord log, String name) {
    Optional<KeyValue> foundAttr = findAttr(log, name);
    return foundAttr.map(attr -> attr.getValue().getStringValue()).orElse(null);
  }

  private static Optional<KeyValue> findAttr(LogRecord log, String name) {
    return log.getAttributesList().stream().filter(a -> a.getKey().equals(name)).findFirst();
  }
}
