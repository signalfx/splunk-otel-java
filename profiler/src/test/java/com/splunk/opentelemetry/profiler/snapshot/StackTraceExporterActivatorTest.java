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

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.createAutoConfiguredSdk;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class StackTraceExporterActivatorTest {
  @AfterEach
  void tearDown() {
    SpanTracker.SUPPLIER.reset();
    StackTraceSampler.SUPPLIER.reset();
    StagingArea.SUPPLIER.reset();
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.reset();
    StackTraceExporter.SUPPLIER.reset();
  }

  @Nested
  class SnapshotProfilingEnabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.configure()
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .with(new StackTraceExporterActivator())
            .build();

    @Test
    void configureStackTraceExporterProvider() {
      var exporter = StackTraceExporter.SUPPLIER.get();
      assertNotSame(StackTraceExporter.NOOP, exporter);
      assertInstanceOf(AsyncStackTraceExporter.class, exporter);
    }
  }

  @Nested
  class SnapshotProfilingDisabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.configure()
            .withProperty("splunk.snapshot.profiler.enabled", "false")
            .with(new StackTraceExporterActivator())
            .build();

    @Test
    void doNotConfigureStackTraceExporterProvider() {
      var exporter = StackTraceExporter.SUPPLIER.get();
      assertSame(StackTraceExporter.NOOP, exporter);
    }
  }

  @Nested
  class DeclarativeConfig {
    @RegisterExtension final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

    @Test
    void configureStackTraceExporterProvider(@TempDir Path tempDir) throws IOException {
      String yaml =
          """
            file_format: "1.0-rc.3"
            distribution:
              splunk:
                profiling:
                  exporter:
                    otlp_log_http:
                  callgraphs:
            """;
      var sdk = createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

      new StackTraceExporterActivator().afterAgent(sdk);

      var exporter = StackTraceExporter.SUPPLIER.get();
      assertNotSame(StackTraceExporter.NOOP, exporter);
      assertInstanceOf(AsyncStackTraceExporter.class, exporter);
    }

    @Test
    void doNotConfigureStackTraceExporterProviderWhenNoCallgraphs(@TempDir Path tempDir)
        throws IOException {
      String yaml =
          """
            file_format: "1.0-rc.3"
            distribution:
              splunk:
                profiling:
            """;
      var sdk = createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

      new StackTraceExporterActivator().afterAgent(sdk);

      var exporter = StackTraceExporter.SUPPLIER.get();
      assertSame(StackTraceExporter.NOOP, exporter);
    }
  }
}
