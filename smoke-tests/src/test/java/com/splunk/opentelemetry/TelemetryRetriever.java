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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

class TelemetryRetriever {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String EMPTY_CONTENT = "[]";

  private final OkHttpClient client;
  private final int backendPort;

  TelemetryRetriever(OkHttpClient client, int backendPort) {
    this.client = client;
    this.backendPort = backendPort;
  }

  void clearTelemetry() throws IOException {
    client
        .newCall(
            new Request.Builder()
                .url(String.format("http://localhost:%d/clear", backendPort))
                .build())
        .execute()
        .close();
  }

  TraceInspector waitForTraces() throws IOException, InterruptedException {
    Stream<JsonNode> content = waitForContent("get-traces");

    return new TraceInspector(
        content.map(this::deserializeTraceRequest).collect(Collectors.toList()));
  }

  MetricsInspector waitForMetrics() throws IOException, InterruptedException {
    Stream<JsonNode> content = waitForContent("get-metrics");

    return new MetricsInspector(
        content.map(this::deserializeMetricsRequest).collect(Collectors.toList()));
  }

  LogsInspector waitForLogs() throws IOException, InterruptedException {
    Stream<JsonNode> content = waitForContent("get-logs");

    return new LogsInspector(
        content.map(this::deserializeLogsRequest).collect(Collectors.toList()));
  }

  private ExportTraceServiceRequest deserializeTraceRequest(JsonNode it) {
    var builder = ExportTraceServiceRequest.newBuilder();
    deserializeIntoBuilder(it, builder);
    return builder.build();
  }

  private ExportMetricsServiceRequest deserializeMetricsRequest(JsonNode it) {
    var builder = ExportMetricsServiceRequest.newBuilder();
    deserializeIntoBuilder(it, builder);
    return builder.build();
  }

  private ExportLogsServiceRequest deserializeLogsRequest(JsonNode it) {
    var builder = ExportLogsServiceRequest.newBuilder();
    deserializeIntoBuilder(it, builder);
    return builder.build();
  }

  private void deserializeIntoBuilder(JsonNode it, GeneratedMessageV3.Builder<?> builder) {
    try {
      JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(it), builder);
    } catch (InvalidProtocolBufferException | JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid telemetry data", e);
    }
  }

  private Stream<JsonNode> waitForContent(String path) throws IOException, InterruptedException {
    long previousSize = 0;
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
    String content = EMPTY_CONTENT;
    while (System.currentTimeMillis() < deadline) {

      Request request =
          new Request.Builder()
              .url(String.format("http://localhost:%d/%s", backendPort, path))
              .build();

      try (ResponseBody body = client.newCall(request).execute().body()) {
        content = body.string();
      }

      if (content.length() > EMPTY_CONTENT.length() && content.length() == previousSize) {
        break;
      }
      previousSize = content.length();
      System.out.printf("Current content size %d%n", previousSize);
      TimeUnit.MILLISECONDS.sleep(500);
    }

    return StreamSupport.stream(OBJECT_MAPPER.readTree(content).spliterator(), false);
  }
}
