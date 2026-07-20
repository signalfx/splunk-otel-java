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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.common.export.HttpSenderProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilingAgentListenerTest {
  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk = OpenTelemetrySdkExtension.configure().build();

  @BeforeEach
  void setUp() {
    TraceThreadChangeDetector.SUPPLIER.configure(mock(TraceThreadChangeDetector.class));
    SnapshotProfilingSpanProcessor.SUPPLIER.configure(mock(SnapshotProfilingSpanProcessor.class));
  }

  @AfterEach
  void tearDown() {
    Snapshotting.resetProfiling();
  }

  @Test
  void configureStackTraceExporterProvider() {
    var logExporter = InMemoryLogRecordExporter.create();
    SnapshotProfilingConfiguration configuration =
        SnapshotProfilingConfiguration.builder()
            .setEnabled(true)
            .setConfigProperties(sdk.getConfig())
            .build();
    SnapshotProfilingConfiguration.SUPPLIER.configure(configuration);

    Snapshotting.agentListener(
            new OtelLoggerFactory(() -> logExporter, declarativeConfigProperties -> logExporter))
        .afterAgent(sdk);

    var exporter = StackTraceExporter.SUPPLIER.get();
    assertNotSame(StackTraceExporter.NOOP, exporter);
    assertInstanceOf(AsyncStackTraceExporter.class, exporter);
  }

  @Test
  void declarativeConfigWithoutExporterPreservesComponentLoader() {
    RecordingComponentLoader componentLoader = new RecordingComponentLoader();
    DeclarativeConfigProperties configProperties =
        YamlDeclarativeConfigProperties.create(Collections.emptyMap(), componentLoader);
    SnapshotProfilingConfiguration configuration =
        SnapshotProfilingConfiguration.builder()
            .setEnabled(true)
            .setConfigProperties(configProperties)
            .build();
    SnapshotProfilingConfiguration.SUPPLIER.configure(configuration);

    new SnapshotProfilingAgentListener().afterAgent(sdk);

    var exporter = StackTraceExporter.SUPPLIER.get();
    assertNotSame(StackTraceExporter.NOOP, exporter);
    assertInstanceOf(AsyncStackTraceExporter.class, exporter);
    assertTrue(componentLoader.loaded(HttpSenderProvider.class));
  }

  @Test
  void doNotConfigureStackTraceExporterProvider() {
    SnapshotProfilingConfiguration configuration =
        SnapshotProfilingConfiguration.builder().setEnabled(false).build();
    SnapshotProfilingConfiguration.SUPPLIER.configure(configuration);

    new SnapshotProfilingAgentListener().afterAgent(sdk);

    var exporter = StackTraceExporter.SUPPLIER.get();
    assertSame(StackTraceExporter.NOOP, exporter);
  }

  private static class RecordingComponentLoader implements ComponentLoader {
    private final ComponentLoader delegate =
        ComponentLoader.forClassLoader(SnapshotProfilingAgentListenerTest.class.getClassLoader());
    private final List<Class<?>> loadedSpiClasses = new ArrayList<>();

    @Override
    public <T> Iterable<T> load(Class<T> spiClass) {
      loadedSpiClasses.add(spiClass);
      return delegate.load(spiClass);
    }

    boolean loaded(Class<?> spiClass) {
      return loadedSpiClasses.contains(spiClass);
    }
  }
}
