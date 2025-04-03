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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StackTraceExporterActivatorTest {
  @AfterEach
  void tearDown() {
    StackTraceExporterProvider.INSTANCE.reset();
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
      var exporter = StackTraceExporterProvider.INSTANCE.get();
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
      var exporter = StackTraceExporterProvider.INSTANCE.get();
      assertSame(StackTraceExporter.NOOP, exporter);
    }
  }
}
