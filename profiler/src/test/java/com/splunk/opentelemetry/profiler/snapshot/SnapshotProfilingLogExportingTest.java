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

import static com.google.perftools.profiles.ProfileProto.Sample;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.perftools.profiles.ProfileProto.Profile;
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes;
import com.splunk.opentelemetry.profiler.pprof.PprofUtils;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;

class SnapshotProfilingLogExportingTest {
  private static final Predicate<Map.Entry<String, Object>> TRACE_ID_LABEL =
      kv -> ProfilingSemanticAttributes.TRACE_ID.getKey().equals(kv.getKey());

  private final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();

  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(Snapshotting.customizer().withRealStackTraceSampler().build())
          .with(new StackTraceExporterActivator(new OtelLoggerFactory(properties -> logExporter)))
          .build();

  @AfterEach
  void tearDown() {
    StackTraceExporterProvider.INSTANCE.reset();
  }

  @ParameterizedTest
  @SpanKinds.Entry
  void exportStackTracesForProfiledTraces(SpanKind kind, Tracer tracer) throws Exception {
    String traceId;
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var span = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
      try (var spanScope = span.makeCurrent()) {
        traceId = span.getSpanContext().getTraceId();
        Thread.sleep(250);
      }
      span.end();
    }

    await().until(() -> !logExporter.getFinishedLogRecordItems().isEmpty());

    var logRecord = logExporter.getFinishedLogRecordItems().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));

    var traceIds =
        profile.getSampleList().stream()
            .flatMap(sampleLabels(profile))
            .filter(TRACE_ID_LABEL)
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());
    assertEquals(Set.of(traceId), traceIds);
  }

  private Function<Sample, Stream<Map.Entry<String, Object>>> sampleLabels(Profile profile) {
    return sample -> PprofUtils.toLabelString(sample, profile).entrySet().stream();
  }
}
