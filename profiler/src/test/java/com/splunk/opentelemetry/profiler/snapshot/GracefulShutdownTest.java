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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

class GracefulShutdownTest {
  private final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();

  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(Snapshotting.customizer().withRealStackTraceSampler().withRealStagingArea().build())
          .with(
              new StackTraceExporterActivator(
                  new OtelLoggerFactory(
                      properties -> logExporter, declarativeConfigProperties -> logExporter)))
          .build();

  @Test
  void stopSnapshotProfilingExtensionWhenOpenTelemetrySdkIsShutdown(Tracer tracer)
      throws Exception {
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      scheduler.scheduleAtFixedRate(startNewTrace(tracer), 0, 50, TimeUnit.MILLISECONDS);
      await().until(() -> !logExporter.getFinishedLogRecordItems().isEmpty());
      sdk.close();
      logExporter.reset();

      var future =
          scheduler.schedule(logExporter::getFinishedLogRecordItems, 500, TimeUnit.MILLISECONDS);
      assertThat(future.get()).isEmpty();
    } finally {
      scheduler.shutdown();
    }
  }

  private Runnable startNewTrace(Tracer tracer) {
    var logger = LoggerFactory.getLogger(GracefulShutdownTest.class);
    return () -> {
      try (var ignored1 = Context.root().with(Volume.HIGHEST).makeCurrent()) {
        var span = tracer.spanBuilder("root").setSpanKind(SpanKind.SERVER).startSpan();
        try (var ignored2 = span.makeCurrent()) {
          Thread.sleep(25);
          span.end();
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    };
  }
}
