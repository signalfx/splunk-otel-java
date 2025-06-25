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
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SPAN_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.TRACE_ID;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.perftools.profiles.ProfileProto.Profile;
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.pprof.PprofUtils;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilingLogExportingTest {
  @RegisterExtension
  private final ContextStorageResettingSpanTrackingActivator spanTrackingActivator = new ContextStorageResettingSpanTrackingActivator();

  private final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();
  private final SnapshotProfilingSdkCustomizer customizer =
      Snapshotting.customizer().withRealStackTraceSampler().with(spanTrackingActivator).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .with(new StackTraceExporterActivator(new OtelLoggerFactory(properties -> logExporter)))
          .build();

  @AfterEach
  void tearDown() {
    StackTraceExporter.SUPPLIER.reset();
  }

  @Test
  void exportStackTracesForProfiledTraces(Tracer tracer) throws Exception {
    SpanContext spanContext;
    try (var ignoredScope1 = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var span = tracer.spanBuilder("root").startSpan();
      try (var ignoredScope2 = span.makeCurrent()) {
        spanContext = span.getSpanContext();
        Thread.sleep(250);
      }
      span.end();
    }

    await().until(() -> !logExporter.getFinishedLogRecordItems().isEmpty());

    var logRecord = logExporter.getFinishedLogRecordItems().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));

    assertEquals(Set.of(spanContext.getTraceId()), findLabelValues(profile, TRACE_ID));
    assertEquals(Set.of(spanContext.getSpanId()), findLabelValues(profile, SPAN_ID));
  }

  private Set<String> findLabelValues(Profile profile, AttributeKey<String> key) {
    return profile.getSampleList().stream()
        .flatMap(sampleLabels(profile))
        .filter(label(key))
        .map(Map.Entry::getValue)
        .map(String::valueOf)
        .collect(Collectors.toSet());
  }

  private Function<Sample, Stream<Map.Entry<String, Object>>> sampleLabels(Profile profile) {
    return sample -> PprofUtils.toLabelString(sample, profile).entrySet().stream();
  }

  private Predicate<Map.Entry<String, Object>> label(AttributeKey<String> key) {
    return kv -> key.getKey().equals(kv.getKey());
  }
}
