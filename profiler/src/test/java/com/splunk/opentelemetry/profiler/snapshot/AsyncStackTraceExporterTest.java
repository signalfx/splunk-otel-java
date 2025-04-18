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

package com.splunk.opentelemetry.profiler.snapshot;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.FRAME_COUNT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.INSTRUMENTATION_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_TIME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SPAN_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.TRACE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.perftools.profiles.ProfileProto.Profile;
import com.splunk.opentelemetry.profiler.exporter.InMemoryOtelLogger;
import com.splunk.opentelemetry.profiler.pprof.PprofUtils;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class AsyncStackTraceExporterTest {
  private final InMemoryOtelLogger logger = new InMemoryOtelLogger();
  private final AsyncStackTraceExporter exporter = new AsyncStackTraceExporter(logger, 200);

  @Test
  void exportStackTraceAsOpenTelemetryLog() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    assertEquals(1, logger.records().size());
  }

  @Test
  void exportMultipleStackTraceAsSingleOpenTelemetryLog() {
    var one = Snapshotting.stackTrace().with(new RuntimeException()).build();
    var two = Snapshotting.stackTrace().with(new IllegalArgumentException()).build();
    var three = Snapshotting.stackTrace().with(new NullPointerException()).build();

    exporter.export(List.of(one, two, three));
    await().until(() -> !logger.records().isEmpty());

    assertEquals(1, logger.records().size());
  }

  @Test
  void encodedLogBodyIsPprofProtobufMessage() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var logRecord = logger.records().get(0);
    assertDoesNotThrow(() -> Profile.parseFrom(PprofUtils.deserialize(logRecord)));
  }

  @Test
  void encodeLogBodyUsingBase64() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var logRecord = logger.records().get(0);
    assertDoesNotThrow(() -> PprofUtils.decode(logRecord));
  }

  @Test
  void logBodyIsGZipped() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var logRecord = logger.records().get(0);
    assertDoesNotThrow(
        () -> {
          var bytes = new ByteArrayInputStream(PprofUtils.decode(logRecord));
          var inputStream = new GZIPInputStream(bytes);
          inputStream.readAllBytes();
        });
  }

  @Test
  void includeSourceTypeOpenTelemetryAttribute() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var attributes = logger.records().get(0).getAttributes();
    assertThat(attributes.asMap()).containsEntry(SOURCE_TYPE, "otel.profiling");
  }

  @Test
  void includeDataTypeOpenTelemetryAttributeWithValueOfCpu() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var attributes = logger.records().get(0).getAttributes();
    assertThat(attributes.asMap()).containsEntry(DATA_TYPE, "cpu");
  }

  @Test
  void includeDataFormatOpenTelemetryAttributeWithValueOfGzipBase64() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var attributes = logger.records().get(0).getAttributes();
    assertThat(attributes.asMap()).containsEntry(DATA_FORMAT, "pprof-gzip-base64");
  }

  @Test
  void includeInstrumentationSourceOpenTelemetryAttributeWithValueOfSnapshot() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var attributes = logger.records().get(0).getAttributes();
    assertThat(attributes.asMap()).containsEntry(INSTRUMENTATION_SOURCE, "snapshot");
  }

  @Test
  void includeFrameCountOpenTelemetryAttributeInLogMessage() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var attributes = logger.records().get(0).getAttributes();
    assertThat(attributes.asMap())
        .containsEntry(FRAME_COUNT, (long) stackTrace.getStackFrames().length);
  }

  @Test
  void includeStackTraceDurationInSamples() throws Exception {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var profile = Profile.parseFrom(PprofUtils.deserialize(logger.records().get(0)));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels)
        .containsEntry(SOURCE_EVENT_TIME.getKey(), stackTrace.getTimestamp().toEpochMilli());
  }

  @Test
  void includeStackTraceTraceIdInSamples() throws Exception {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var profile = Profile.parseFrom(PprofUtils.deserialize(logger.records().get(0)));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels).containsEntry(TRACE_ID.getKey(), stackTrace.getTraceId());
  }

  @Test
  void includeStackTraceSpanIdInSamples() throws Exception {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().until(() -> !logger.records().isEmpty());

    var profile = Profile.parseFrom(PprofUtils.deserialize(logger.records().get(0)));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels).containsEntry(SPAN_ID.getKey(), stackTrace.getSpanId());
  }
}
